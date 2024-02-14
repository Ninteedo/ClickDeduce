package convertors

import scalatags.Text.TypedTag
import scalatags.Text.all.{div, i, raw, span}

trait ConvertableText {
  def asPlainText: String
  def asHtml: TypedTag[String]
  def asHtmlReadOnly: TypedTag[String]
  def asLaTeX: String

  override def toString: String = asPlainText
}

case class TextElement(text: String) extends ConvertableText {
  override def asPlainText: String = text
  override def asHtml: TypedTag[String] = span(raw(text))
  override def asHtmlReadOnly: TypedTag[String] = asHtml
  override def asLaTeX: String = s"\\textrm{$text}"
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

case class HtmlElement(html: TypedTag[String]) extends ConvertableText {
  override def asPlainText: String = html.toString
  override def asHtml: TypedTag[String] = html
  override def asHtmlReadOnly: TypedTag[String] = html
  override def asLaTeX: String = html.toString
}
