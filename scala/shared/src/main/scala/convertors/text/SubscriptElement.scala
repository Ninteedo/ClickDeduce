package convertors.text

import scalatags.Text.TypedTag
import scalatags.Text.all.sub

case class SubscriptElement(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = elem.asPlainText

  override def asHtml: TypedTag[String] = sub(elem.asHtml)

  override def asHtmlReadOnly: TypedTag[String] = sub(elem.asHtmlReadOnly)

  override def asLaTeX: String = s"_{${elem.asLaTeX}}"
}

object SubscriptElement {
  def labelled(elem: String, label: String): MultiElement = labelled(TextElement(elem), TextElement(label))

  def labelled(elem: ConvertableText, label: ConvertableText): MultiElement =
    MultiElement(elem, SubscriptElement(label))
}
