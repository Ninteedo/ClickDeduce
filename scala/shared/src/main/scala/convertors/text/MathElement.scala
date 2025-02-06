package convertors.text

import app.UtilityFunctions
import convertors.ClassDict
import scalatags.Text.TypedTag
import scalatags.Text.all.*

case class MathElement(text: String) extends ConvertableText {
  override def asPlainText: String = text

  override def asHtml: TypedTag[String] = span(cls := ClassDict.MATH_MODE, raw(text))

  override def asHtmlReadOnly: TypedTag[String] = asHtml

  override def asLaTeX: String = UtilityFunctions.escapeLaTeX(text)
}

object MathElement {
  val comma: ConvertableText = TextElement(",")
  val equals: ConvertableText = MathElement("=")
  val doubleEquals: ConvertableText = MathElement("==")
  val notEquals: ConvertableText = MathElement("≠")
  val plus: ConvertableText = MathElement("+")
  val colon: ConvertableText = TextElement(":")
  val period: ConvertableText = TextElement(".")
  val lessThan: ConvertableText = MathElement("<")
  val greaterThanEqual: ConvertableText = MathElement("≥")
}
