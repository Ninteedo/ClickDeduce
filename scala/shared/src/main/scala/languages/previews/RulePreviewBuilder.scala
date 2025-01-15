package languages.previews

class RulePreviewBuilder {
  private var typeCheckRules: List[TypeCheckRulePreview] = Nil
  private var evaluationRules: List[EvalRulePreview] = Nil

  def addTypeCheckRule(rule: TypeCheckRulePreview): RulePreviewBuilder = {
    typeCheckRules = typeCheckRules :+ rule
    this
  }

  def addTypeCheckRule(rule: TypeCheckRuleBuilder): RulePreviewBuilder = {
    addTypeCheckRule(rule.build)
  }

  def addEvaluationRule(rule: EvalRulePreview): RulePreviewBuilder = {
    evaluationRules = evaluationRules :+ rule
    this
  }

  def addEvaluationRule(rule: EvalRuleBuilder): RulePreviewBuilder = {
    addEvaluationRule(rule.build)
  }

  def build: RulePreview = RulePreview(typeCheckRules, evaluationRules)

  def buildOption: Option[RulePreview] = if typeCheckRules.isEmpty && evaluationRules.isEmpty then None else Some(build)
}
