package languages.terms.errors

import convertors.{ClassDict, ConvertableText, HtmlElement, TextElement}
import languages.terms.values.Value
import scalatags.Text.TypedTag
import scalatags.Text.all.*

/** An error resulting from an expression being evaluated.
 */
abstract class EvalError extends Value, TermError {
  override lazy val tooltipText: String = message

  override lazy val valueText: TypedTag[String] = div("error!", cls := ClassDict.ERROR_ORIGIN)

  override val isError: Boolean = true

  override def toText: ConvertableText =
    HtmlElement(span(cls := "error-origin", raw("error!")), TextElement("error!"))

  override val needsBrackets: Boolean = false
}
