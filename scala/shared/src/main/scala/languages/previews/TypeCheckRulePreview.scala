package languages.previews

import convertors.ClassDict

case class TypeCheckRulePreview(conclusion: RulePart, assumptions: RulePart*)
    extends InferenceRulePreview {
  override protected val divClass: String = ClassDict.RULE_TYPE
}
