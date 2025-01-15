package languages.previews

import convertors.text.*
import languages.terms.types.Type

case class TypeCheckRulePart(t: ConvertableText) extends InferenceRulePart {
  override def toText: ConvertableText = t
}

object TypeCheckRulePart {
  def apply(l: ConvertableText, r: Type): TypeCheckRulePart = TypeCheckRulePart(l, r.toText)

  def apply(l: ConvertableText, r: ConvertableText, binds: List[ConvertableText] = Nil): TypeCheckRulePart = {
    TypeCheckRulePart(
      MultiElement(
        Symbols.gamma,
        if binds.isEmpty
        then NullElement()
        else MultiElement(MathElement.comma.spaceAfter, ListElement(binds, NullElement(), NullElement())),
        Symbols.turnstile.spacesAround,
        l,
        MathElement.colon.spaceAfter,
        r
      )
    )
  }

  def eTo(n: Int, t: Type): TypeCheckRulePart = TypeCheckRulePart(TermCommons.e(n), t)

  def eTo(n: Int, t: ConvertableText): TypeCheckRulePart = TypeCheckRulePart(TermCommons.e(n), t)

  def eToT(n: Int): TypeCheckRulePart = TypeCheckRulePart(TermCommons.e(n), TermCommons.t(n))
}
