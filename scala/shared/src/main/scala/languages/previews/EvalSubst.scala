package languages.previews

import convertors.text.{ConvertableText, MultiElement, SquareBracketedElement, Symbols}

case class EvalSubst(l: ConvertableText, r: ConvertableText) extends RulePart {
  override def toText: ConvertableText = SquareBracketedElement(MultiElement(l, Symbols.forwardSlash, r))
}
