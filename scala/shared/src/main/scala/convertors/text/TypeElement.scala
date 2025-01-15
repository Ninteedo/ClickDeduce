package convertors.text

import scalatags.Text.TypedTag
import scalatags.Text.all.*

case class TypeElement(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = elem.asPlainText

  override def asHtml: TypedTag[String] = elem.asHtml(cls := "value-type")

  override def asHtmlReadOnly: TypedTag[String] = elem.asHtmlReadOnly(cls := "type")

  override def asLaTeX: String = elem.asLaTeX
}
