package convertors.text

object TermCommons {
  def e(n: Int): ConvertableText = MultiElement(e, SubscriptElement(MathElement(n.toString)))

  def v(n: Int): ConvertableText = MultiElement(v, SubscriptElement(MathElement(n.toString)))

  def t(n: Int): ConvertableText = MultiElement(t, SubscriptElement(MathElement(n.toString)))

  val e: ConvertableText = MathElement("e")
  val v: ConvertableText = MathElement("v")
  val t: ConvertableText = Symbols.tau
  val f: ConvertableText = MathElement("f")
  val x: ConvertableText = MathElement("x")
  val y: ConvertableText = MathElement("y")
  val A: ConvertableText = MathElement("A")
}
