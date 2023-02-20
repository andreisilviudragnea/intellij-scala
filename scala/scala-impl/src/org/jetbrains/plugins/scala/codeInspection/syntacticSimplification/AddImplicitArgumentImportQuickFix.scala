package org.jetbrains.plugins.scala.codeInspection.syntacticSimplification

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.codeInspection.AbstractFixOnPsiElement
import org.jetbrains.plugins.scala.codeInspection.syntacticSimplification.AddImplicitArgumentImportQuickFix.withImplicits
import org.jetbrains.plugins.scala.codeInspection.syntacticSimplification.Utils.getNameFrom
import org.jetbrains.plugins.scala.lang.psi.ScImportsHolder
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult
import org.jetbrains.plugins.scala.lang.psi.api.ImplicitArgumentsOwner

class AddImplicitArgumentImportQuickFix(expression: ImplicitArgumentsOwner) extends AbstractFixOnPsiElement("Add implicit argument import", expression) {
  override protected def doApplyFix(element: ImplicitArgumentsOwner)(implicit project: Project): Unit = {
    element.findImplicitArguments match {
      case Some(scalaResolveResults) => scalaResolveResults.foreach { scalaResolveResult =>
        withImplicits(scalaResolveResult).foreach { result =>
          val importsUsed = result.importsUsed
          if (importsUsed.nonEmpty && importsUsed.exists(_.importExpr.exists(_.hasWildcardSelector))) {
            val str = getNameFrom(result)
            ScImportsHolder(element).addImportForPath(str)
            //          executeWriteActionCommand("Add import for implicit conversion") {
            //            if (element.isValid) {
            //
            //            }
            //          }
            //          val scalaFile = element.getContainingFile.asInstanceOf[ScalaFile]
            //          ScalaImportOptimizer.findOptimizerFor(scalaFile).get.processFile(scalaFile).run()
          }
        }
      }
      case None =>
    }
  }
}

object AddImplicitArgumentImportQuickFix {
  def withImplicits(srr: ScalaResolveResult): Seq[ScalaResolveResult] = {
    srr +:
      srr.implicitConversion.toSeq.flatMap(withImplicits) ++:
      srr.implicitParameters.flatMap(withImplicits)
  }
}
