package languages.previews

import convertors.text.*

case class EvalRulePart(t: ConvertableText) extends RulePart {
  override def toText: ConvertableText = t
}

object EvalRulePart {
  def apply(l: ConvertableText, r: ConvertableText): EvalRulePart = EvalRulePart(l, r, Nil)

  def apply(l: ConvertableText, r: ConvertableText, binds: List[EvalRuleBind]): EvalRulePart = EvalRulePart(
    l, r, TermCommons.env, binds
  )

  def apply(l: ConvertableText, r: ConvertableText, env: ConvertableText, binds: List[EvalRuleBind] = Nil): EvalRulePart = EvalRulePart(
    MultiElement(
      env,
      if binds.isEmpty then NullElement()
      else ListElement(binds.map(_.toTextNoBrackets)),
      MathElement.comma.spaceAfter,
      l,
      Symbols.doubleDownArrow.spacesAround,
      r
    )
  )

  def eToV(n: Int): EvalRulePart = EvalRulePart(TermCommons.e(n), TermCommons.v(n))

  def reflexive(t: ConvertableText): EvalRulePart = EvalRulePart(t, t)
}
