package languages.previews

import convertors.text.{ConvertableText, MultiElement, SquareBracketedElement, Symbols}

case class EvalRuleBind(l: ConvertableText, r: ConvertableText) extends RulePart {
  override def toText: ConvertableText = SquareBracketedElement(toTextNoBrackets)

  def toTextNoBrackets: ConvertableText = MultiElement(
    l,
    Symbols.assign.spacesAround,
    r
  )
}
