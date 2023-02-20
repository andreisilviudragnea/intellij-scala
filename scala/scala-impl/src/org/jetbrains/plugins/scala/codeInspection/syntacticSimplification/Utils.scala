package org.jetbrains.plugins.scala.codeInspection.syntacticSimplification

import org.jetbrains.plugins.scala.autoImport.GlobalImplicitInstance
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult

object Utils {
  def getNameFrom(scalaResolveResult: ScalaResolveResult): String = {
    val importUsed = scalaResolveResult.importsUsed.iterator.next()
    val qualName = importUsed.importExpr.get.qualifier.get.qualName
    scalaResolveResult.implicitConversion match {
      case Some(resolveResult) => s"$qualName.${resolveResult.name}"
      case None =>
        GlobalImplicitInstance.from(scalaResolveResult) match {
          case Some(globalImplicitInstance) => globalImplicitInstance.pathToOwner
          case None => scalaResolveResult.name match {
            case "apply" | "unapply" => s"$qualName.${scalaResolveResult.parentElement.get.asInstanceOf[ScObject].name}"
            case name => s"$qualName.$name"
          }
        }
    }
  }
}
