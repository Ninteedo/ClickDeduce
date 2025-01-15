package convertors.text

import convertors.ClassDict
import scalatags.Text.TypedTag
import scalatags.Text.all.*

case class NumberElement(num: String) extends ConvertableText {
  override def asPlainText: String = num

  override def asHtml: TypedTag[String] = span(cls := ClassDict.NUMBER_MODE, raw(num))

  override def asHtmlReadOnly: TypedTag[String] = asHtml

  override def asLaTeX: String = num
}

object NumberElement {
  def apply(num: Int): NumberElement = NumberElement(num.toString)

  def apply(num: BigInt): NumberElement = NumberElement(num.toString)
}
