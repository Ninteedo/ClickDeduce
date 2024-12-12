package languages.terms.errors

import languages.terms.types.Type

/** An error that occurs due to attempting to process an unknown `Expr`.
  *
  * @param message
  *   The error message.
  */
case class UnexpectedExpr(override val message: String) extends EvalError {
  override val typ: Type = UnexpectedExprType(message)
}
