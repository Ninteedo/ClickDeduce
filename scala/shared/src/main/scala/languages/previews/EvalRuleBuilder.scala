package languages.previews

import convertors.text.ConvertableText

class EvalRuleBuilder {
  private var assumptions: List[InferenceRulePart] = Nil
  private var conclusion: Option[InferenceRulePart] = None

  def addAssumption(assumption: InferenceRulePart): EvalRuleBuilder = {
    assumptions = assumptions :+ assumption
    this
  }

  def addAssumption(l: ConvertableText, r: ConvertableText): EvalRuleBuilder = {
    addAssumption(EvalRulePart(l, r))
  }

  def setConclusion(conclusion: InferenceRulePart): EvalRuleBuilder = {
    this.conclusion = Some(conclusion)
    this
  }

  def setConclusion(l: ConvertableText, r: ConvertableText): EvalRuleBuilder = {
    setConclusion(EvalRulePart(l, r))
  }

  def build: EvalRulePreview = EvalRulePreview(conclusion.get, assumptions: _*)
}
