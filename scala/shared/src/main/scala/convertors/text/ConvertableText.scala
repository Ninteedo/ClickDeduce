package convertors.text

import convertors.*
import scalatags.Text.TypedTag

/**
 * Text that can be converted to plain text, HTML, and LaTeX.
 */
trait ConvertableText {
  def asPlainText: String
  def asHtml: TypedTag[String]
  def asHtmlReadOnly: TypedTag[String]
  def asLaTeX: String

  def toReadOnly: ConvertableText = this

  def spacesAround: ConvertableText = SurroundSpaces(this)
  def spaceAfter: ConvertableText = SpaceAfter(this)

  override def toString: String = asPlainText
}
