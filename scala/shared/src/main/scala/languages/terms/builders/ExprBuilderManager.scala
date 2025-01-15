package languages.terms.builders

import languages.previews.RulePreview
import languages.terms.exprs.Expr

class ExprBuilderManager extends BuilderManager[Expr, ExprCompanion] {
  def getRulePreview(name: String): Option[RulePreview] = getCompanion(name).flatMap(_.rulePreview)
}
