package convertors.text

import app.UtilityFunctions
import convertors.ClassDict
import scalatags.Text.TypedTag
import scalatags.Text.all.*

case class TextElement(text: String) extends ConvertableText {
  override def asPlainText: String = text

  override def asHtml: TypedTag[String] = span(cls := ClassDict.TEXT_MODE, raw(text))

  override def asHtmlReadOnly: TypedTag[String] = asHtml

  override def asLaTeX: String = s"\\textrm{${UtilityFunctions.escapeLaTeX(text)}}"
}
