package languages.previews

import convertors.text.ConvertableText

class TypeCheckRuleBuilder {
  private var assumptions: List[InferenceRulePart] = Nil
  private var conclusion: Option[InferenceRulePart] = None

  def addAssumption(assumption: InferenceRulePart): TypeCheckRuleBuilder = {
    assumptions = assumptions :+ assumption
    this
  }

  def addAssumption(
    l: ConvertableText,
    r: ConvertableText,
    binds: List[ConvertableText] = Nil
  ): TypeCheckRuleBuilder = {
    addAssumption(TypeCheckRulePart(l, r, binds))
  }

  def setConclusion(conclusion: InferenceRulePart): TypeCheckRuleBuilder = {
    this.conclusion = Some(conclusion)
    this
  }

  def setConclusion(l: ConvertableText, r: ConvertableText): TypeCheckRuleBuilder = {
    setConclusion(TypeCheckRulePart(l, r))
  }

  def build: TypeCheckRulePreview = TypeCheckRulePreview(conclusion.get, assumptions: _*)
}
