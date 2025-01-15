package languages.previews

import scalatags.Text.TypedTag
import scalatags.Text.all.*

case class RulePreview(typeCheckRule: Seq[TypeCheckRulePreview], evaluationRule: Seq[EvalRulePreview]) {
  def toHtml: TypedTag[String] = div(div(typeCheckRule.map(_.toHtml)), div(evaluationRule.map(_.toHtml)))
}

object RulePreview {
  def apply(typeCheckRule: TypeCheckRulePreview, evaluationRule: EvalRulePreview): RulePreview =
    RulePreview(List(typeCheckRule), List(evaluationRule))
}
