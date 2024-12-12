package languages.terms.errors

/** An error that occurs due to attempting to process an unknown `Expr`.
 *
 * @param message
 *   The error message.
 */
case class UnexpectedExprType(override val message: String) extends TypeError
