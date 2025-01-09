package languages

import convertors.*
import scalatags.Text.TypedTag
import scalatags.Text.all.*

trait InferenceRulePart {
  def toText: ConvertableText
}

case class EvaluationRulePart(l: ConvertableText, r: ConvertableText) extends InferenceRulePart {
  override def toText: ConvertableText = MultiElement(l, DoubleDownArrow(), r)
}

case class EvaluationRulePartEnv(l: ConvertableText, r: ConvertableText, lookups: List[ConvertableText]) extends InferenceRulePart {
  override def toText: ConvertableText = MultiElement(ListElement(lookups, start = SigmaSymbol(), end = NullElement()))
}

case class TypeCheckRulePart(l: ConvertableText, r: ConvertableText, binds: List[ConvertableText] = Nil) extends InferenceRulePart {
  override def toText: ConvertableText = MultiElement(
    GammaSymbol(),
    ListElement(binds, start = NullElement(), end = NullElement()),
    Turnstile(),
    l,
    TextElement(":"),
    r
  )
}

case class InferenceRulePreview(assumptions: List[InferenceRulePart], conclusion: InferenceRulePart) {
  def toHtml: TypedTag[String] = div(
    div(
      "Assumptions:",
      ul(assumptions.map(a => li(a.toText.asHtml)))
    ),
    div(
      "Conclusion:",
      conclusion.toText.asHtml
    )
  )
}

case class RulePreview(typeCheckRule: InferenceRulePreview, evaluationRule: InferenceRulePreview) {
  def toHtml: TypedTag[String] = div(
    cls := ClassDict.RULE_PREVIEW,
    typeCheckRule.toHtml,
    evaluationRule.toHtml
  )
}
