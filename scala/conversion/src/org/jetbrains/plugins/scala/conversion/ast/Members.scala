package org.jetbrains.plugins.scala.conversion.ast

//TODO setter&getter
case class FieldConstruction(
  modifiers: IntermediateNode,
  name: IntermediateNode,
  ftype: IntermediateNode,
  isVar: Boolean,
  initializer: Option[IntermediateNode]
) extends IntermediateNode with TypedElement {
  override def getType: TypeConstruction = ftype.asInstanceOf[TypedElement].getType
}

case class MethodConstruction(
  modifiers: IntermediateNode,
  name: IntermediateNode,
  typeParams: Seq[IntermediateNode],
  params: Seq[IntermediateNode],
  body: Option[IntermediateNode],
  retType: Option[IntermediateNode]
) extends IntermediateNode


trait Constructor

case class ConstructorSimply(
  modifiers: IntermediateNode,
  typeParams: Seq[IntermediateNode],
  params: Seq[IntermediateNode],
  body: Option[IntermediateNode]
) extends IntermediateNode

case class PrimaryConstruction(
  params: Seq[IntermediateNode],
  superCall: IntermediateNode,
  body: Option[Seq[IntermediateNode]],
  modifiers: IntermediateNode
) extends IntermediateNode with Constructor

case class EnumConstruction(name: IntermediateNode) extends IntermediateNode