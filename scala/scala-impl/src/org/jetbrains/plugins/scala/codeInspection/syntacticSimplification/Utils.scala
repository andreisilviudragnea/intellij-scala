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
          case Some(globalImplicitInstance) =>
            s"${globalImplicitInstance.pathToOwner}.${globalImplicitInstance.member.getName}"
          case None => s"$qualName.${scalaResolveResult.name}"
        }
    }
  }

  def usesImportWithWildcard(scalaResolveResult: ScalaResolveResult): Boolean = {
    val importsUsed = scalaResolveResult.importsUsed
    scalaResolveResult.implicitConversion match {
      case Some(resolveResult) => importsUsed.nonEmpty && importsUsed.forall(_.importExpr.exists(
        importExpr => importExpr.hasWildcardSelector && !importExpr.importedNames.contains(resolveResult.name)
      ))
      case None => scalaResolveResult.name match {
        case "apply" | "unapply" =>
          importsUsed.nonEmpty && importsUsed.forall(_.importExpr.exists(importExpr => importExpr.hasWildcardSelector &&
            !importExpr.importedNames.contains(scalaResolveResult.parentElement.get.asInstanceOf[ScObject].name)
          ))
        case name => importsUsed.nonEmpty && importsUsed.forall(_.importExpr.exists(
          importExpr => importExpr.hasWildcardSelector && !importExpr.importedNames.contains(name)
        ))
      }
    }
  }
}
