package org.jetbrains.plugins.scala.lang.transformation.general

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.extensions.{&&, NextSibling, Whitespace}
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScBlockStatement
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createSemicolon
import org.jetbrains.plugins.scala.lang.transformation.AbstractTransformer
import org.jetbrains.plugins.scala.project.ProjectContext

class AppendSemicolon extends AbstractTransformer {
  override protected def transformation(implicit project: ProjectContext): PartialFunction[PsiElement, Unit] = {
    case (statement: ScBlockStatement) && NextSibling(Whitespace(s)) if s.contains("\n") =>
      statement.getParent.addAfter(createSemicolon, statement)
  }
}
