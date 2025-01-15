package languages.terms.builders

import languages.previews.RulePreview
import languages.terms.exprs.Expr

trait ExprCompanion extends BuilderCompanion[Expr] {
  lazy val rulePreview: Option[RulePreview] = None
}
