object Q {
  def apply[A <: String](a: A): String = "Q"
}


object Example {
  val q = Q
  val s = q[<caret>]("a")
}
// A <: String