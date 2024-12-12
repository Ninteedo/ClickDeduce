package languages.terms.errors

/** Type error for when the depth limit is exceeded.
 */
case class StackOverflowErrorType() extends TypeError {
  override val message: String = "Stack overflow error"
}
