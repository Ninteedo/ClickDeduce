package languages.previews

import convertors.text.*

case class EvalRulePart(t: ConvertableText) extends InferenceRulePart {
  override def toText: ConvertableText = t
}

object EvalRulePart {
  def apply(l: ConvertableText, r: ConvertableText): EvalRulePart = EvalRulePart(
    MultiElement(l, Symbols.doubleDownArrow.spacesAround, r)
  )

  def eToV(n: Int): EvalRulePart = EvalRulePart(TermCommons.e(n), TermCommons.v(n))

  def reflexive(t: ConvertableText): EvalRulePart = EvalRulePart(t, t)
}
