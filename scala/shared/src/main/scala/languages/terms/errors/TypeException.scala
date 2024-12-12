package languages.terms.errors

/** An error that occurs due to attempting to type-check an expression that results in an exception.
  *
  * This is used when an exception is thrown during type-checking, not just when the type-checking results in an error.
  * For example, a stack overflow.
  *
  * @param message
  *   The error message.
  */
case class TypeException(override val message: String) extends TypeError

object TypeException {
  val stackOverflow: TypeError = TypeException("Stack overflow")
}
