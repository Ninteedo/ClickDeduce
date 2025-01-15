package languages.previews

import convertors.ClassDict

case class EvalRulePreview(conclusion: InferenceRulePart, assumptions: InferenceRulePart*)
    extends InferenceRulePreview {
  override protected val divClass: String = ClassDict.RULE_EVAL
}
