class ScalaConstructorD[T: Manifest]() {
  def this(a: Int) = this()
}

new ScalaConstructorD[Int](0)(<caret>)
// [T: Manifest]()(implicit manifest$T$0: Manifest[Int])