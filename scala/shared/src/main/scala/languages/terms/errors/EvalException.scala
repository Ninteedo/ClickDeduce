package languages.terms.errors

import languages.terms.types.Type

/** An error that occurs due to attempting to evaluate an expression that results in an exception.
  *
  * This is used when an exception is thrown during evaluation, not just when the evaluation results in an error. For
  * example, a stack overflow.
  *
  * @param message
  *   The error message.
  */
case class EvalException(override val message: String) extends EvalError {
  override val typ: Type = TypeException(message)
}

object EvalException {
  val stackOverflow: EvalError = EvalException("Stack overflow")
}
