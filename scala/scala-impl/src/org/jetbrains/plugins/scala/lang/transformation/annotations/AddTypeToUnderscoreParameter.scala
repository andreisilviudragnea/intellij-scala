package org.jetbrains.plugins.scala.lang.transformation.annotations

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.extensions.{&&, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScUnderscoreSection
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaCode._
import org.jetbrains.plugins.scala.lang.psi.types.result.Typeable
import org.jetbrains.plugins.scala.lang.transformation.AbstractTransformer
import org.jetbrains.plugins.scala.project.ProjectContext

class AddTypeToUnderscoreParameter extends AbstractTransformer {
  override protected def transformation(implicit project: ProjectContext): PartialFunction[PsiElement, Unit] = {
    case (e: ScUnderscoreSection) && Typeable(t) if !e.nextSibling.exists(_.textMatches(":")) =>
      appendTypeAnnotation(t, e, { annotation =>
        val replacement = code"(_: $annotation)"
        val result = e.replace(replacement)
        result.getFirstChild.getNextSibling.getLastChild
      })
  }
}
