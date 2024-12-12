package languages.terms.errors

import languages.terms.types.Type

/** Evaluation error for when the depth limit is exceeded.
 */
case class StackOverflowErrorValue() extends EvalError {
  override val message: String = "Stack overflow error"

  override val typ: Type = StackOverflowErrorType()
}
