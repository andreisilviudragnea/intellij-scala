package org.jetbrains.plugins.scala.codeInspection.syntacticSimplification

import com.intellij.codeInspection.{LocalInspectionTool, ProblemHighlightType, ProblemsHolder}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.codeInspection.PsiElementVisitorSimple
import org.jetbrains.plugins.scala.codeInspection.syntacticSimplification.AddExplicitImportQuickFix.explicitImportText
import org.jetbrains.plugins.scala.codeInspection.syntacticSimplification.AddImplicitArgumentImportQuickFix.withImplicits
import org.jetbrains.plugins.scala.codeInspection.syntacticSimplification.Utils.{getNameFrom, usesImportWithWildcard}
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.lang.psi.api.ImplicitArgumentsOwner
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScConstructorInvocation, ScPrimaryConstructor, ScReference}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScExpression, ScMethodCall, ScNewTemplateDefinition, ScReferenceExpression}
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunctionDefinition, ScTypeAlias}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScTemplateParents
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult

class RedundantNewCaseClassInspection extends LocalInspectionTool {

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitorSimple = {
    case newTemplate: ScNewTemplateDefinition if !newTemplate.extendsBlock.isAnonymousClass =>
      if (hasRedundantNew(newTemplate)) {
        holder.registerProblem(newTemplate.getFirstChild, ScalaBundle.message("new.on.case.class.instantiation.redundant"),
          ProblemHighlightType.LIKE_UNUSED_SYMBOL, new RemoveNewQuickFix(newTemplate))
      }
    case element: ScReferenceExpression =>
      processReference(element, holder)
      processExpression(element, holder)
      processImplicitArgumentsOwner(element, holder)
    case element: ScReference =>
      processReference(element, holder)
    case element: ScExpression =>
      processExpression(element, holder)
      processImplicitArgumentsOwner(element, holder)
    case element: ImplicitArgumentsOwner =>
      processImplicitArgumentsOwner(element, holder)
    case _ =>
  }

  private def processReference(element: ScReference, holder: ProblemsHolder): Unit = {
    element.multiResolveScala(false).foreach { scalaResolveResult =>
      // TODO: Fix still failing import expansions
      if (usesImportWithWildcard(scalaResolveResult)) {
        holder.registerProblem(
          element,
          s"Wildcard import ${explicitImportText(scalaResolveResult)} for expression ${element.getText}",
          ProblemHighlightType.WARNING,
          new AddExplicitImportQuickFix(element)
        )
      }
    }
  }

  private def processExpression(element: ScExpression, holder: ProblemsHolder): Unit = {
    element.implicitConversion() match {
      case Some(scalaResolveResult) =>
        if (usesImportWithWildcard(scalaResolveResult)) {
          holder.registerProblem(
            element,
            s"Implicit conversion ${getNameFrom(scalaResolveResult)} for expression ${element.getText}",
            ProblemHighlightType.WARNING,
            new AddImplicitConversionImportQuickFix(element)
          )
        }
      case None =>
    }
  }

  private def processImplicitArgumentsOwner(element: ImplicitArgumentsOwner, holder: ProblemsHolder): Unit = {
    element.findImplicitArguments match {
      case Some(scalaResolveResults) => scalaResolveResults.foreach { scalaResolveResult =>
        withImplicits(scalaResolveResult).foreach { result =>
          if (usesImportWithWildcard(result)) {
            holder.registerProblem(
              element,
              s"Implicit argument import ${getNameFrom(result)} for expression ${element.getText}",
              ProblemHighlightType.WARNING,
              new AddImplicitArgumentImportQuickFix(element)
            )
          }
        }
      }
      case None =>
    }
  }

  private def hasRedundantNew(newTemplate: ScNewTemplateDefinition): Boolean = {
    val constructor = getConstructorInvocationFromTemplate(newTemplate)
    def resolvedConstructor = resolveConstructor(constructor)

    isCreatingSameType(newTemplate) &&
      constructorCallHasArgumentList(constructor) &&
      constructor.exists(hasApplyDefinedOnCaseClass) &&
      isProblemlessPrimaryConstructorOfCaseClass(resolvedConstructor) &&
      !isTypeAlias(resolvedConstructor)
  }

  private def hasApplyDefinedOnCaseClass(constrInvocation: ScConstructorInvocation): Boolean = {
    val constructorText = constrInvocation.getText
    val expression = ScalaPsiElementFactory.createExpressionWithContextFromText(constructorText, constrInvocation.getContext, constrInvocation)
    val reference = getDeepestInvokedReference(expression).filter(_.isValid)

    reference.flatMap(_.bind())
      .exists {
        case ScalaResolveResult(f: ScFunctionDefinition, _) => f.syntheticNavigationElement.isInstanceOf[ScClass]
        case _ => false
      }
  }

  private def getDeepestInvokedReference(resolved: ScExpression): Option[ScReferenceExpression] = {
    resolved match {
      case method: ScMethodCall => method.deepestInvokedExpr match {
        case deepestRef: ScReferenceExpression => Some(deepestRef)
        case _ => None
      }
      case _ => None
    }
  }

  /**
    * Determines if the type of the extends block is the same as the type of the new template type.
    * This prevents us from incorrectly displaying a warning when creating anonymous classes or instances with
    * mixin traits.
    */
  private def isCreatingSameType(newTemplate: ScNewTemplateDefinition): Boolean =
    newTemplate.extendsBlock.templateParents.exists(_.parentClauses.size == 1)

  private def isTypeAlias(maybeResolveResult: Option[ScalaResolveResult]): Boolean = {
    maybeResolveResult.map(_.getActualElement).exists {
      case _: ScTypeAlias => true
      case _ => false
    }
  }

  private def getConstructorInvocationFromTemplate(newTemplate: ScNewTemplateDefinition): Option[ScConstructorInvocation] =
    newTemplate.extendsBlock.firstChild.flatMap {
      case parents: ScTemplateParents => parents.firstParentClause
      case _                          => None
    }

  private def constructorCallHasArgumentList(maybeConstructorInvocation: Option[ScConstructorInvocation]): Boolean = {
    maybeConstructorInvocation.flatMap(_.args).isDefined
  }

  private def resolveConstructor(maybeConstructorInvocation: Option[ScConstructorInvocation]): Option[ScalaResolveResult] = {
    for {
      constrInvoc <- maybeConstructorInvocation
      ref <- constrInvoc.reference
      resolved <- ref.bind()
    } yield {
      resolved
    }
  }

  private def isProblemlessPrimaryConstructorOfCaseClass(maybeResolveResult: Option[ScalaResolveResult]): Boolean = {
    maybeResolveResult
      .filter(_.problems.isEmpty)
      .map(_.element)
      .exists {
        case ScPrimaryConstructor.ofClass(clazz) => clazz.isCase
        case _ => false
      }
  }
}
