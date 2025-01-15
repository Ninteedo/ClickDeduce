package convertors.text

import scalatags.Text.TypedTag
import scalatags.Text.all.span

case class NullElement() extends ConvertableText {
  override def asPlainText: String = ""

  override def asHtml: TypedTag[String] = span()

  override def asHtmlReadOnly: TypedTag[String] = span()

  override def asLaTeX: String = ""
}
