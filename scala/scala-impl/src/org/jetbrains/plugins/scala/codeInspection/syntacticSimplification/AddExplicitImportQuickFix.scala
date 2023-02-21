package org.jetbrains.plugins.scala.codeInspection.syntacticSimplification

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.codeInspection.AbstractFixOnPsiElement
import org.jetbrains.plugins.scala.codeInspection.syntacticSimplification.AddExplicitImportQuickFix.explicitImportText
import org.jetbrains.plugins.scala.codeInspection.syntacticSimplification.Utils.usesImportWithWildcard
import org.jetbrains.plugins.scala.lang.psi.ScImportsHolder
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult

class AddExplicitImportQuickFix(reference: ScReference) extends AbstractFixOnPsiElement("Add explicit import", reference) {
  override protected def doApplyFix(element: ScReference)(implicit project: Project): Unit = {
    element.multiResolveScala(false).foreach { scalaResolveResult =>
      if (usesImportWithWildcard(scalaResolveResult)) {
        ScImportsHolder(element).addImportForPath(explicitImportText(scalaResolveResult))
        //        executeWriteActionCommand("Add explicit import") {
        //          if (element.isValid) {
        //
        //          }
        //        }
        //        val scalaFile = element.getContainingFile.asInstanceOf[ScalaFile]
        //        ScalaImportOptimizer.findOptimizerFor(scalaFile).get.processFile(scalaFile).run()
      }
    }
  }
}

object AddExplicitImportQuickFix {
  def explicitImportText(scalaResolveResult: ScalaResolveResult): String = {
    val importUsed = scalaResolveResult.importsUsed.iterator.next()
    val qualName = importUsed.importExpr.get.qualifier.get.qualName
    scalaResolveResult.implicitConversion match {
      case Some(resolveResult) => s"$qualName.${resolveResult.name}"
      case None => scalaResolveResult.name match {
        case "apply" | "unapply" => s"$qualName.${scalaResolveResult.parentElement.get.asInstanceOf[ScObject].name}"
        case name => s"$qualName.$name"
      }
    }
  }
}
