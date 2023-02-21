package org.jetbrains.plugins.scala.codeInspection.syntacticSimplification

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.codeInspection.AbstractFixOnPsiElement
import org.jetbrains.plugins.scala.codeInspection.syntacticSimplification.Utils.{getNameFrom, usesImportWithWildcard}
import org.jetbrains.plugins.scala.lang.psi.ScImportsHolder
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

class AddImplicitConversionImportQuickFix(expression: ScExpression) extends AbstractFixOnPsiElement("Add import for implicit conversion", expression) {
  override protected def doApplyFix(element: ScExpression)(implicit project: Project): Unit = {
    element.implicitConversion() match {
      case Some(scalaResolveResult) =>
        if (usesImportWithWildcard(scalaResolveResult)) {
          ScImportsHolder(element).addImportForPath(getNameFrom(scalaResolveResult))
//          executeWriteActionCommand("Add import for implicit conversion") {
//            if (element.isValid) {
//
//            }
//          }
//          val scalaFile = element.getContainingFile.asInstanceOf[ScalaFile]
//          ScalaImportOptimizer.findOptimizerFor(scalaFile).get.processFile(scalaFile).run()
        }
      case None =>
    }
  }
}
