package convertors.text

import scalatags.Text
import scalatags.Text.TypedTag
import scalatags.Text.all.*

case class AngleBracketedElement(contents: ConvertableText) extends ConvertableText {
  override def asPlainText: String = s"⟨${contents.asPlainText}⟩"

  override def asHtml: TypedTag[String] = span(raw("⟨"), contents.asHtml, raw("⟩"))

  override def asHtmlReadOnly: TypedTag[String] = span(raw("⟨"), contents.asHtmlReadOnly, raw("⟩"))

  override def asLaTeX: String = s"\\langle ${contents.asLaTeX} \\rangle"
}
