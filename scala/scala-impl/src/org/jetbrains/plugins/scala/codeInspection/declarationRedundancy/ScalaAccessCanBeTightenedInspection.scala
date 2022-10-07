package org.jetbrains.plugins.scala.codeInspection.declarationRedundancy

import com.intellij.codeInspection.{LocalInspectionTool, LocalQuickFixOnPsiElement, ProblemHighlightType, ProblemsHolder}
import com.intellij.openapi.project.Project
import com.intellij.psi.{PsiElement, PsiElementVisitor, PsiFile}
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.codeInspection.declarationRedundancy.ScalaAccessCanBeTightenedInspection.getPipeline
import org.jetbrains.plugins.scala.codeInspection.declarationRedundancy.cheapRefSearch.Search.Pipeline
import org.jetbrains.plugins.scala.codeInspection.declarationRedundancy.cheapRefSearch.{SearchMethodsWithProjectBoundCache, ElementUsage, Search}
import org.jetbrains.plugins.scala.codeInspection.typeAnnotation.TypeAnnotationInspection
import org.jetbrains.plugins.scala.extensions.PsiModifierListOwnerExt
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPatternList
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScReferencePattern
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunctionDefinition, ScPatternDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.{ScModifierListOwner, ScNamedElement}

private final class ScalaAccessCanBeTightenedInspection extends LocalInspectionTool {
  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    new PsiElementVisitor {
      override def visitElement(element: PsiElement): Unit =
        element match {
          case n: ScNamedElement with ScModifierListOwner if !n.hasModifierPropertyScala("private") =>
            n match {
              case _: ScFunctionDefinition | _: ScTypeDefinition =>
                processElement(n, n, holder, isOnTheFly)
              case _ =>
            }
          case patternList@ScPatternList(Seq(pattern: ScReferencePattern)) =>
            val modifierListOwner = patternList.getParent.asInstanceOf[ScModifierListOwner]
            if (!modifierListOwner.hasModifierPropertyScala("private")) {
              processElement(pattern, modifierListOwner, holder, isOnTheFly)
            }
          case _ =>
        }
    }

  private def processElement(
    element: ScNamedElement,
    modifierListOwner: ScModifierListOwner,
    problemsHolder: ProblemsHolder,
    isOnTheFly: Boolean
  ): Unit = {

    /**
     * The reason for the below logic is as follows:
     *
     * 1. On the one hand we want to keep our QuickFix text congruent with Java's
     * [[com.intellij.codeInspection.visibility.AccessCanBeTightenedInspection]], where the QuickFix text
     * is "Make 'private'".
     * 2. On the other hand we prefer to present the QuickFix that adds the 'private'
     * modifier at the top of the QuickFix list.
     * 3. Since the platform applies alphabetical ordering when presenting the QuickFixes,
     * and quite often [[org.jetbrains.plugins.scala.codeInspection.typeAnnotation.TypeAnnotationInspection]]
     * offers a "Add type annotation" QuickFix at the same time that a declaration can be made private,
     * a QuickFix with the text "Make 'private'" would not appear at the top in such a case.
     *
     * So, in case a [[org.jetbrains.plugins.scala.codeInspection.declarationRedundancy.ScalaAccessCanBeTightenedInspection.MakePrivateQuickFix]] is
     * offered to the user, we essentially perform the same check as
     * [[org.jetbrains.plugins.scala.codeInspection.typeAnnotation.TypeAnnotationInspection]]. When the result is
     * positive (i.e. a type annotation QuickFix is offered), we go for a fallback text that starts with "Add ...".
     *
     * In case we ever get custom QuickFix ordering, this fallback routine becomes redundant.
     * See [[https://youtrack.jetbrains.com/issue/IDEA-88512]].
     */
    @Nls
    lazy val quickFixText: String = {
      val expression = modifierListOwner match {
        case value: ScPatternDefinition if value.isSimple && !value.hasExplicitType =>
          value.expr
        case method: ScFunctionDefinition if method.hasAssign && !method.hasExplicitType && !method.isConstructor =>
          method.body
        case _ => None
      }

      if (TypeAnnotationInspection.getReasonForTypeAnnotationOn(modifierListOwner, expression).isEmpty) {
        ScalaInspectionBundle.message("make.private")
      } else {
        ScalaInspectionBundle.message("add.private.modifier")
      }
    }

    if (Search.Util.shouldProcessElement(element)) {
      val usages = getPipeline(element.getProject).runSearchPipeline(element, isOnTheFly)

      if (usages.forall(_.targetCanBePrivate)) {
        val fix = new ScalaAccessCanBeTightenedInspection.MakePrivateQuickFix(modifierListOwner, quickFixText)
        problemsHolder.registerProblem(element.nameId, ScalaInspectionBundle.message("access.can.be.private"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fix)
      }
    }
  }
}

private object ScalaAccessCanBeTightenedInspection {

  private[declarationRedundancy] class MakePrivateQuickFix(element: ScModifierListOwner, @Nls text: String) extends LocalQuickFixOnPsiElement(element) {
    override def invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement): Unit =
      element.setModifierProperty("private")

    override def getText: String = text

    override def getFamilyName: String = ScalaInspectionBundle.message("change.modifier")
  }

  private def getPipeline(project: Project): Pipeline = {

    val canExit = (usage: ElementUsage) => !usage.targetCanBePrivate

    val searcher = SearchMethodsWithProjectBoundCache(project)

    val localSearch = searcher.LocalSearchMethods
    val globalSearch = searcher.GlobalSearchMethods

    new Pipeline(localSearch ++ globalSearch, canExit)
  }
}