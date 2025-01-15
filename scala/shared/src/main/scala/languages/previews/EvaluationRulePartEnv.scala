package languages.previews

import convertors.text.*

case class EvaluationRulePartEnv(l: ConvertableText, r: ConvertableText, lookups: List[ConvertableText])
    extends InferenceRulePart {
  override def toText: ConvertableText = MultiElement(ListElement(lookups, start = Symbols.sigma, end = NullElement()))
}
