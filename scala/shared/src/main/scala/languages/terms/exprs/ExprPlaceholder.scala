package languages.terms.exprs

import convertors.ConvertableText

/** A placeholder for text in an expression.
 *
 * Used when converting expressions to text in a recursive manner.
 *
 * Not intended for use within actual languages and is hidden from the user.
 *
 * @param content
 *   The text content.
 * @param needsBrackets
 *   Whether this placeholder should be surrounded by brackets when converted to text.
 */
case class ExprPlaceholder(content: ConvertableText, override val needsBrackets: Boolean = false)
  extends NotImplementedExpr {
  override def toText: ConvertableText = content
}

/** Companion object for [[ExprPlaceholder]].
 */
object ExprPlaceholder {

  /** Create an expression placeholder from an expression.
   * @param expr
   *   The expression.
   * @return
   *   The expression placeholder.
   */
  def apply(expr: Expr): ExprPlaceholder = ExprPlaceholder(expr.toText, expr.needsBrackets)
}
