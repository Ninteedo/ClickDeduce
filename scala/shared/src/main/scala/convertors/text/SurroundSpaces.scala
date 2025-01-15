package convertors.text

import scalatags.Text.TypedTag
import scalatags.Text.all.*

case class SurroundSpaces(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = s" ${elem.asPlainText} "

  override def asHtml: TypedTag[String] = span(raw(" "), elem.asHtml, raw(" "))

  override def asHtmlReadOnly: TypedTag[String] = span(raw(" "), elem.asHtmlReadOnly, raw(" "))

  override def asLaTeX: String = elem.asLaTeX
}
