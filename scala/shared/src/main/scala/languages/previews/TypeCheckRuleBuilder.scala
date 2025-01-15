package languages.previews

import convertors.text.ConvertableText

class TypeCheckRuleBuilder {
  private var assumptions: List[RulePart] = Nil
  private var conclusion: Option[RulePart] = None

  def addAssumption(assumption: RulePart): TypeCheckRuleBuilder = {
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

  def setConclusion(conclusion: RulePart): TypeCheckRuleBuilder = {
    this.conclusion = Some(conclusion)
    this
  }

  def setConclusion(l: ConvertableText, r: ConvertableText): TypeCheckRuleBuilder = {
    setConclusion(TypeCheckRulePart(l, r))
  }

  def build: TypeCheckRulePreview = TypeCheckRulePreview(conclusion.get, assumptions: _*)
}
