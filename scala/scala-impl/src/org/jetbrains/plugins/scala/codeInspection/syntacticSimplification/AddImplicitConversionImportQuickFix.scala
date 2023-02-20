package org.jetbrains.plugins.scala.codeInspection.syntacticSimplification

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.codeInspection.AbstractFixOnPsiElement
import org.jetbrains.plugins.scala.codeInspection.syntacticSimplification.Utils.getNameFrom
import org.jetbrains.plugins.scala.editor.importOptimizer.ScalaImportOptimizer
import org.jetbrains.plugins.scala.extensions.executeWriteActionCommand
import org.jetbrains.plugins.scala.lang.psi.ScImportsHolder
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

class AddImplicitConversionImportQuickFix(expression: ScExpression) extends AbstractFixOnPsiElement("Add import for implicit conversion", expression) {
  override protected def doApplyFix(element: ScExpression)(implicit project: Project): Unit = {
    element.implicitConversion() match {
      case Some(scalaResolveResult) =>
        val importsUsed = scalaResolveResult.importsUsed
        if (importsUsed.nonEmpty && importsUsed.exists(_.importExpr.exists(_.hasWildcardSelector))) {
          val importUsed = importsUsed.iterator.next()
          val qualName = importUsed.importExpr.get.qualifier.get.qualName
          val importText = s"$qualName.${getNameFrom(scalaResolveResult)}"
          ScImportsHolder(element).addImportForPath(importText)
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
