package languages.previews

import convertors.ClassDict

case class TypeCheckRulePreview(conclusion: InferenceRulePart, assumptions: InferenceRulePart*)
    extends InferenceRulePreview {
  override protected val divClass: String = ClassDict.RULE_TYPE
}
