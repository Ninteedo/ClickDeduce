package languages.previews

import convertors.text.{ConvertableText, MathElement, MultiElement}

case class TypeCheckRuleBind(l: ConvertableText, r: ConvertableText) extends RulePart {
  override def toText: ConvertableText = MultiElement(l, MathElement.colon, r)
}
