package org.jetbrains.plugins.scala.lang.psi.impl.toplevel
package typedef

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScGiven

trait ScGivenImpl extends ScGiven {
  override def nameElement: Option[PsiElement] =
    findFirstChildByType(ScalaTokenTypes.tIDENTIFIER)
}
