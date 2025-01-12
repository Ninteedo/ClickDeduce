package languages

import convertors.*
import languages.terms.types.Type
import scalatags.Text.TypedTag
import scalatags.Text.all.*

trait InferenceRulePart {
  def toText: ConvertableText
}

case class EvalRulePart(l: ConvertableText, r: ConvertableText) extends InferenceRulePart {
  override def toText: ConvertableText = MultiElement(l, DoubleDownArrow().spacesAround, r)
}

object EvalRulePart {
  def eToV(n: Int): EvalRulePart = EvalRulePart(TermCommons.e(n), TermCommons.v(n))
}

case class EvaluationRulePartEnv(l: ConvertableText, r: ConvertableText, lookups: List[ConvertableText]) extends InferenceRulePart {
  override def toText: ConvertableText = MultiElement(ListElement(lookups, start = SigmaSymbol(), end = NullElement()))
}

case class TypeCheckRulePart(l: ConvertableText, r: ConvertableText, binds: List[ConvertableText] = Nil) extends InferenceRulePart {
  override def toText: ConvertableText = MultiElement(
    GammaSymbol(),
    ListElement(binds, start = NullElement(), end = NullElement()),
    Turnstile().spacesAround,
    l,
    TextElement(":").spaceAfter,
    r
  )
}

object TypeCheckRulePart {
  def apply(l: ConvertableText, r: Type): TypeCheckRulePart = TypeCheckRulePart(l, r.toText)

  def eTo(n: Int, t: Type): TypeCheckRulePart = TypeCheckRulePart(TermCommons.e(n), t)
}

abstract class InferenceRulePreview {
  val assumptions: List[InferenceRulePart]
  val conclusion: InferenceRulePart

  protected val divClass: String

  def toHtml: TypedTag[String] = div(
    cls := divClass,
    div(
      cls := ClassDict.RULE_PREVIEW_ASSUMPTIONS,
      assumptions.map(a => a.toText.asHtml)
    ),
    div(
      cls := ClassDict.RULE_PREVIEW_CONCLUSION,
      conclusion.toText.asHtml
    )
  )
}

case class EvalRulePreview(assumptions: List[InferenceRulePart], conclusion: InferenceRulePart) extends InferenceRulePreview {
  override protected val divClass: String = ClassDict.RULE_EVAL
}

case class TypeCheckRulePreview(assumptions: List[InferenceRulePart], conclusion: InferenceRulePart) extends InferenceRulePreview {
  override protected val divClass: String = ClassDict.RULE_TYPE
}

case class RulePreview(typeCheckRule: TypeCheckRulePreview, evaluationRule: EvalRulePreview) {
  def toHtml: TypedTag[String] = div(
//    cls := ClassDict.RULE_PREVIEW,
    typeCheckRule.toHtml,
    evaluationRule.toHtml
  )
}
