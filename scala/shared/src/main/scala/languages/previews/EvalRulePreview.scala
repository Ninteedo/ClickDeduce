package languages.previews

import convertors.ClassDict

case class EvalRulePreview(conclusion: RulePart, assumptions: RulePart*)
    extends InferenceRulePreview {
  override protected val divClass: String = ClassDict.RULE_EVAL
}
