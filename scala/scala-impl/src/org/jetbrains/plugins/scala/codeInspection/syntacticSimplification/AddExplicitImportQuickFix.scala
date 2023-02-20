package org.jetbrains.plugins.scala.codeInspection.syntacticSimplification

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.plugins.scala.{ScalaBundle, editor}
import org.jetbrains.plugins.scala.codeInspection.AbstractFixOnPsiElement
import org.jetbrains.plugins.scala.codeInspection.syntacticSimplification.Utils.getNameFrom
import org.jetbrains.plugins.scala.editor.importOptimizer.ScalaImportOptimizer
import org.jetbrains.plugins.scala.extensions.executeWriteActionCommand
import org.jetbrains.plugins.scala.lang.psi.ScImportsHolder
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference

class AddExplicitImportQuickFix(reference: ScReference) extends AbstractFixOnPsiElement("Add explicit import", reference) {
  override protected def doApplyFix(element: ScReference)(implicit project: Project): Unit = {
    element.multiResolveScala(false).map { scalaResolveResult =>
      val importsUsed = scalaResolveResult.importsUsed
      if (importsUsed.nonEmpty && importsUsed.exists(_.importExpr.exists(_.hasWildcardSelector))) {
        val importUsed = importsUsed.iterator.next()
        val qualName = importUsed.importExpr.get.qualifier.get.qualName
        val importText = s"$qualName.${getNameFrom(scalaResolveResult)}"
        executeWriteActionCommand("Add explicit import") {
          if (element.isValid) {
            ScImportsHolder(element).addImportForPath(importText)
          }
        }
//        val scalaFile = element.getContainingFile.asInstanceOf[ScalaFile]
//        ScalaImportOptimizer.findOptimizerFor(scalaFile).get.processFile(scalaFile).run()
      }
    }
  }
}
