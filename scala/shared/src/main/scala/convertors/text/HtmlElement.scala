package convertors.text

import scalatags.Text.TypedTag
import scalatags.Text.all.{disabled, readonly}

case class HtmlElement(html: TypedTag[String], nonHtml: ConvertableText) extends ConvertableText {
  override def asPlainText: String = html.toString

  override def asHtml: TypedTag[String] = html

  override def asHtmlReadOnly: TypedTag[String] = html(readonly, disabled)

  override def asLaTeX: String = nonHtml.asLaTeX

  override def toReadOnly: ConvertableText = HtmlElement(asHtmlReadOnly, nonHtml)
}
