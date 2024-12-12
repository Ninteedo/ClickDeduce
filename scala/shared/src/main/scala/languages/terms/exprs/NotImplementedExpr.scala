package languages.terms.exprs

import languages.env.{TypeEnv, ValueEnv}
import languages.terms.errors.{UnexpectedExpr, UnexpectedExprType}
import languages.terms.types.Type
import languages.terms.values.Value

/** An expression that is not implemented.
 *
 * Not expected to be used in practice, but provided as an option for expressions that are not yet implemented or
 * should not be used.
 */
abstract class NotImplementedExpr extends Expr {
  override def evalInner(env: ValueEnv): Value = UnexpectedExpr(toString)

  override def typeCheckInner(tEnv: TypeEnv): Type = UnexpectedExprType(toString)
}
