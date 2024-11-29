package convertors

import languages.LIf.Literal
import scalatags.Text.TypedTag
import scalatags.Text.all.*

trait ConvertableText {
  def asPlainText: String
  def asHtml: TypedTag[String]
  def asHtmlReadOnly: TypedTag[String]
  def asLaTeX: String

  def toReadOnly: ConvertableText = this

  override def toString: String = asPlainText
}

case class TextElement(text: String) extends ConvertableText {
  override def asPlainText: String = text
  override def asHtml: TypedTag[String] = span(raw(text))
  override def asHtmlReadOnly: TypedTag[String] = asHtml
  override def asLaTeX: String = s"\\textrm{${escapeLaTeX(text)}}"
}

case class MathElement(text: String) extends ConvertableText {
  override def asPlainText: String = text
  override def asHtml: TypedTag[String] = span(raw(text))
  override def asHtmlReadOnly: TypedTag[String] = asHtml
  override def asLaTeX: String = escapeLaTeX(text)
}

object MathElement {
  val comma: MathElement = MathElement(",")
  val equals: MathElement = MathElement("=")
  val plus: MathElement = MathElement("+")
  val colon: MathElement = MathElement(":")
  val period: MathElement = MathElement(".")
  val lessThan: MathElement = MathElement("<")
}

case class MultiElement(elems: ConvertableText*) extends ConvertableText {
  override def asPlainText: String = elems.map(_.asPlainText).mkString
  override def asHtml: TypedTag[String] = div(elems.map(_.asHtml): _*)
  override def asHtmlReadOnly: TypedTag[String] = div(elems.map(_.asHtmlReadOnly): _*)
  override def asLaTeX: String = elems.map(_.asLaTeX).mkString
}

case class ItalicsElement(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = elem.asPlainText
  override def asHtml: TypedTag[String] = i(elem.asHtml)
  override def asHtmlReadOnly: TypedTag[String] = i(elem.asHtmlReadOnly)
  override def asLaTeX: String = s"\\textit{${elem.asLaTeX}}"
}

case class BracketedElement(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = s"(${elem.asPlainText})"
  override def asHtml: TypedTag[String] = span(raw("("), elem.asHtml, raw(")"))
  override def asHtmlReadOnly: TypedTag[String] = span(raw("("), elem.asHtmlReadOnly, raw(")"))
  override def asLaTeX: String = s"(${elem.asLaTeX})"
}

case class SubscriptElement(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = elem.asPlainText
  override def asHtml: TypedTag[String] = sub(elem.asHtml)
  override def asHtmlReadOnly: TypedTag[String] = sub(elem.asHtmlReadOnly)
  override def asLaTeX: String = s"_{${elem.asLaTeX}}"
}

case class TimesSymbol() extends ConvertableText {
  override def asPlainText: String = "×"
  override def asHtml: TypedTag[String] = span(raw("&times;"))
  override def asHtmlReadOnly: TypedTag[String] = asHtml
  override def asLaTeX: String = "\\times"
}

case class LambdaSymbol(capital: Boolean = false) extends ConvertableText {
  override def asPlainText: String = if (capital) "Λ" else "λ"
  override def asHtml: TypedTag[String] = span(raw(if (capital) "&Lambda;" else "&lambda;"))
  override def asHtmlReadOnly: TypedTag[String] = asHtml
  override def asLaTeX: String = if (capital) "\\Lambda" else "\\lambda"
}

case class ForAllSymbol() extends ConvertableText {
  override def asPlainText: String = "∀"
  override def asHtml: TypedTag[String] = span(raw("&forall;"))
  override def asHtmlReadOnly: TypedTag[String] = asHtml
  override def asLaTeX: String = "\\forall"
}

case class HtmlElement(html: TypedTag[String], nonHtml: ConvertableText) extends ConvertableText {
  override def asPlainText: String = html.toString
  override def asHtml: TypedTag[String] = html
  override def asHtmlReadOnly: TypedTag[String] = html(readonly, disabled)
  override def asLaTeX: String = nonHtml.asLaTeX

  override def toReadOnly: ConvertableText = HtmlElement(asHtmlReadOnly, nonHtml)
}

case class LiteralElement(literal: Literal) extends ConvertableText {
  override def asPlainText: String = literal.toString
  override def asHtml: TypedTag[String] = literal.toHtml
  override def asHtmlReadOnly: TypedTag[String] = literal.toHtml(readonly, disabled)
  override def asLaTeX: String = literal.toText.asLaTeX
}

case class ValueElement(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = elem.asPlainText
  override def asHtml: TypedTag[String] = elem.asHtml(cls := "value")
  override def asHtmlReadOnly: TypedTag[String] = elem.asHtmlReadOnly(cls := "value")
  override def asLaTeX: String = elem.asLaTeX
}

case class TypeElement(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = elem.asPlainText
  override def asHtml: TypedTag[String] = elem.asHtml(cls := "value-type")
  override def asHtmlReadOnly: TypedTag[String] = elem.asHtmlReadOnly(cls := "type")
  override def asLaTeX: String = elem.asLaTeX
}

case class ListElement(
  items: List[ConvertableText],
  start: ConvertableText = TextElement("["),
  end: ConvertableText = TextElement("]"),
  delimiter: ConvertableText = TextElement(", ")
) extends ConvertableText {
  override def asPlainText: String = items.map(_.asPlainText).mkString(start.asPlainText, delimiter.asPlainText, end.asPlainText)
  override def asHtml: TypedTag[String] = span(start.asHtml, intersperse(items.map(_.asHtml), delimiter.asHtml).toSeq, end.asHtml)
  override def asHtmlReadOnly: TypedTag[String] = span(start.asHtmlReadOnly, intersperse(items.map(_.asHtmlReadOnly), delimiter.asHtmlReadOnly).toSeq, end.asHtmlReadOnly)
  override def asLaTeX: String = items.map(_.asLaTeX).mkString(start.asLaTeX, delimiter.asLaTeX, end.asLaTeX)
  
  private def intersperse[T](items: List[T], sep: T): List[T] = items match {
    case Nil => Nil
    case x :: Nil => List(x)
    case x :: xs => x :: sep :: intersperse(xs, sep)
  }
}

case class DoubleRightArrow() extends ConvertableText {
  override def asPlainText: String = "⇒"
  override def asHtml: TypedTag[String] = span(raw("&rArr;"))
  override def asHtmlReadOnly: TypedTag[String] = asHtml
  override def asLaTeX: String = "\\Rightarrow"
}

case class SingleRightArrow() extends ConvertableText {
  override def asPlainText: String = "→"
  override def asHtml: TypedTag[String] = span(raw("&rarr;"))
  override def asHtmlReadOnly: TypedTag[String] = asHtml
  override def asLaTeX: String = "\\rightarrow"
}

case class SurroundSpaces(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = s" ${elem.asPlainText} "
  override def asHtml: TypedTag[String] = span(raw(" "), elem.asHtml, raw(" "))
  override def asHtmlReadOnly: TypedTag[String] = span(raw(" "), elem.asHtmlReadOnly, raw(" "))
  override def asLaTeX: String = elem.asLaTeX
}

case class SpaceAfter(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = s"${elem.asPlainText} "
  override def asHtml: TypedTag[String] = span(elem.asHtml, raw(" "))
  override def asHtmlReadOnly: TypedTag[String] = span(elem.asHtmlReadOnly, raw(" "))
  override def asLaTeX: String = elem.asLaTeX
}

def escapeLaTeX(text: String): String = text
  .replace("\\", "\\textbackslash{}")
  .replace("{", "\\{")
  .replace("}", "\\}")
  .replace("_", "\\_")
  .replace("^", "\\^{}")
  .replace("~", "\\textasciitilde{}")
  .replace("#", "\\#")
  .replace("$", "\\$")
  .replace("%", "\\%")
  .replace("&", "\\&")
  .replace("<", "\\textless{}")
  .replace(">", "\\textgreater{}")
  .replace("|", "\\textbar{}")
