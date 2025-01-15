package convertors.text

import scalatags.Text.TypedTag
import scalatags.Text.all.{raw, span}

case class SpaceAfter(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = s"${elem.asPlainText} "

  override def asHtml: TypedTag[String] = span(elem.asHtml, raw(" "))

  override def asHtmlReadOnly: TypedTag[String] = span(elem.asHtmlReadOnly, raw(" "))

  override def asLaTeX: String = elem.asLaTeX
}
