package convertors.text

import scalatags.Text.TypedTag
import scalatags.Text.all.*

case class ValueElement(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = elem.asPlainText

  override def asHtml: TypedTag[String] = elem.asHtml(cls := "value")

  override def asHtmlReadOnly: TypedTag[String] = elem.asHtmlReadOnly(cls := "value")

  override def asLaTeX: String = elem.asLaTeX
}
