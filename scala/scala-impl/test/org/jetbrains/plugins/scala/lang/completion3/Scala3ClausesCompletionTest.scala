package org.jetbrains.plugins.scala.lang.completion3

import org.jetbrains.plugins.scala.ScalaVersion
import org.jetbrains.plugins.scala.lang.completion3.base.ScalaClausesCompletionTestBase

class Scala3ClausesCompletionTest extends ScalaClausesCompletionTestBase {
  override protected def supportedIn(version: ScalaVersion): Boolean = version >= ScalaVersion.Latest.Scala_3_0

  def testScala3Enum(): Unit = doMatchCompletionTest(
    fileText =
      s"""enum Direction:
         |  case North, South
         |  case West, East
         |end Direction
         |
         |object O:
         |  (_: Direction) m$CARET
       """.stripMargin,
    resultText =
      s"""enum Direction:
         |  case North, South
         |  case West, East
         |end Direction
         |
         |object O:
         |  (_: Direction) match
         |    case Direction.North => $START$CARET???$END
         |    case Direction.South => ???
         |    case Direction.West => ???
         |    case Direction.East => ???
       """.stripMargin
  )

  def testScala3Enum2(): Unit = doMatchCompletionTest(
    fileText =
      s"""enum Json:
         |  case JsString(value: String)
         |  case JsNumber(value: Double)
         |  case JsNull
         |end Json
         |
         |object O:
         |  (_: Json) m$CARET
         |
       """.stripMargin,
    resultText =
      s"""enum Json:
         |  case JsString(value: String)
         |  case JsNumber(value: Double)
         |  case JsNull
         |end Json
         |
         |object O:
         |  (_: Json) match
         |    case Json.JsString(value) => $START$CARET???$END
         |    case Json.JsNumber(value) => ???
         |    case Json.JsNull => ???
       """.stripMargin
  )

  def testScala3Enum3(): Unit = doMatchCompletionTest(
    fileText =
      s"""trait A
         |trait B
         |
         |enum MyEnum extends A with B:
         |  case Foo
         |  case Bar(value: Int)
         |end MyEnum
         |
         |object O:
         |  (_: MyEnum) m$CARET
         |
       """.stripMargin,
    resultText =
      s"""trait A
         |trait B
         |
         |enum MyEnum extends A with B:
         |  case Foo
         |  case Bar(value: Int)
         |end MyEnum
         |
         |object O:
         |  (_: MyEnum) match
         |    case MyEnum.Foo => $START$CARET???$END
         |    case MyEnum.Bar(value) => ???
       """.stripMargin
  )
}
