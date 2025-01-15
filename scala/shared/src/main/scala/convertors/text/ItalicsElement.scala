package convertors.text

import scalatags.Text.TypedTag
import scalatags.Text.all.i

case class ItalicsElement(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = elem.asPlainText

  override def asHtml: TypedTag[String] = i(elem.asHtml)

  override def asHtmlReadOnly: TypedTag[String] = i(elem.asHtmlReadOnly)

  override def asLaTeX: String = s"\\textit{${elem.asLaTeX}}"
}
