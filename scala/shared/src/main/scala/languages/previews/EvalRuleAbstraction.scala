package languages.previews

import convertors.text.{AngleBracketedElement, ConvertableText, MathElement, MultiElement}

case class EvalRuleAbstraction(l: ConvertableText, r: ConvertableText) extends RulePart {
  override def toText: ConvertableText = AngleBracketedElement(
    MultiElement(l, MathElement.comma, r)
  )
}
