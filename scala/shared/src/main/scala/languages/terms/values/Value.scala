package languages.terms.values

import app.HTMLHelper
import convertors.ClassDict
import languages.terms.Term
import languages.terms.exprs.Expr
import languages.terms.types.Type
import scalatags.Text.TypedTag
import scalatags.Text.all.*

/** A value resulting from an [[Expr]] being evaluated.
 *
 * Also has a corresponding [[Type]].
 */
abstract class Value extends Term {
  override lazy val toHtml: TypedTag[String] = HTMLHelper.tooltip(valueText, span(tooltipText))

  lazy val tooltipText: String = toString + ": " + typ.toString

  lazy val valueText: TypedTag[String] = div(
    div(toText.asHtml, cls := ClassDict.VALUE),
    if (valueTextShowType) List(span(": "), div(typ.valueText, cls := ClassDict.VALUE_TYPE)) else div()
  )

  /** The type of this value. */
  val typ: Type

  /** Whether this value represents an evaluation error.
   *
   * Defaults to false, overridden in [[EvalError]].
   */
  val isError: Boolean = false

  /** Whether to show the type of the value in the text representation.
   *
   * Default is true, should be overridden for values that should not show their type.
   */
  def valueTextShowType: Boolean = true

  /** Whether to show variables with this value when displaying a list of bound variables.
   */
  val showInValueLookupList: Boolean = true
}
