package languages.previews

import convertors.text.ConvertableText

class EvalRuleBuilder {
  private var assumptions: List[RulePart] = Nil
  private var conclusion: Option[RulePart] = None

  def addAssumption(assumption: RulePart): EvalRuleBuilder = {
    assumptions = assumptions :+ assumption
    this
  }

  def addAssumption(l: ConvertableText, r: ConvertableText, binds: List[EvalRuleBind] = Nil): EvalRuleBuilder = {
    addAssumption(EvalRulePart(l, r, binds))
  }

  def setConclusion(conclusion: RulePart): EvalRuleBuilder = {
    this.conclusion = Some(conclusion)
    this
  }

  def setConclusion(l: ConvertableText, r: ConvertableText): EvalRuleBuilder = {
    setConclusion(EvalRulePart(l, r))
  }

  def build: EvalRulePreview = EvalRulePreview(conclusion.get, assumptions: _*)
}
