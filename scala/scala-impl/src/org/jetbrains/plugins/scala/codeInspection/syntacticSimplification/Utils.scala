package org.jetbrains.plugins.scala.codeInspection.syntacticSimplification

import org.jetbrains.plugins.scala.autoImport.GlobalImplicitInstance
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
}
