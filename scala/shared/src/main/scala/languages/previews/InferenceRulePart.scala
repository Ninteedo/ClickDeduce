package languages.previews

import convertors.text.ConvertableText
import scalatags.Text.TypedTag

trait InferenceRulePart extends ConvertableText {
  def toText: ConvertableText

  override def asPlainText: String = toText.asPlainText

  override def asHtml: TypedTag[String] = toText.asHtml

  override def asHtmlReadOnly: TypedTag[String] = toText.asHtmlReadOnly

  override def asLaTeX: String = toText.asLaTeX
}
