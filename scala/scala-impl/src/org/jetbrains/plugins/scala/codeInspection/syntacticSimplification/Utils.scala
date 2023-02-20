package org.jetbrains.plugins.scala.codeInspection.syntacticSimplification

import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult

object Utils {
  def getNameFrom(scalaResolveResult: ScalaResolveResult): String = {
    scalaResolveResult.implicitConversion match {
      case Some(resolveResult) => resolveResult.name
      case None => scalaResolveResult.name match {
        case "apply" => scalaResolveResult.parentElement.get.asInstanceOf[ScObject].name
        case name => name
      }
    }
  }
}
