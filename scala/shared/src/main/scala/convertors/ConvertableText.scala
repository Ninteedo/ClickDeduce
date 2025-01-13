package convertors

import languages.terms.literals.Literal
import scalatags.Text.TypedTag
import scalatags.Text.all.*

trait ConvertableText {
  def asPlainText: String
  def asHtml: TypedTag[String]
  def asHtmlReadOnly: TypedTag[String]
  def asLaTeX: String

  def toReadOnly: ConvertableText = this

  def spacesAround: ConvertableText = SurroundSpaces(this)
  def spaceAfter: ConvertableText = SpaceAfter(this)

  override def toString: String = asPlainText
}

case class TextElement(text: String) extends ConvertableText {
  override def asPlainText: String = text
  override def asHtml: TypedTag[String] = span(cls := ClassDict.TEXT_MODE, raw(text))
  override def asHtmlReadOnly: TypedTag[String] = asHtml
  override def asLaTeX: String = s"\\textrm{${escapeLaTeX(text)}}"
}

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

case class MathElement(text: String) extends ConvertableText {
  override def asPlainText: String = text
  override def asHtml: TypedTag[String] = span(cls := ClassDict.MATH_MODE, raw(text))
  override def asHtmlReadOnly: TypedTag[String] = asHtml
  override def asLaTeX: String = escapeLaTeX(text)
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

case class SquareBracketedElement(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = s"[${elem.asPlainText}]"
  override def asHtml: TypedTag[String] = span(raw("["), elem.asHtml, raw("]"))
  override def asHtmlReadOnly: TypedTag[String] = span(raw("["), elem.asHtmlReadOnly, raw("]"))
  override def asLaTeX: String = s"[${elem.asLaTeX}]"
}

case class SubscriptElement(elem: ConvertableText) extends ConvertableText {
  override def asPlainText: String = elem.asPlainText
  override def asHtml: TypedTag[String] = sub(elem.asHtml)
  override def asHtmlReadOnly: TypedTag[String] = sub(elem.asHtmlReadOnly)
  override def asLaTeX: String = s"_{${elem.asLaTeX}}"
}

object SubscriptElement {
  def labelled(elem: String, label: String): MultiElement = labelled(TextElement(elem), TextElement(label))

  def labelled(elem: ConvertableText, label: ConvertableText): MultiElement = MultiElement(
    elem, SubscriptElement(label)
  )
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

case class NullElement() extends ConvertableText {
  override def asPlainText: String = ""
  override def asHtml: TypedTag[String] = span()
  override def asHtmlReadOnly: TypedTag[String] = span()
  override def asLaTeX: String = ""
}

object Symbols {
  private case class Symbol(plain: String, html: String, latex: String, mathMode: Boolean = false) extends ConvertableText {
    override def asPlainText: String = plain
    override def asHtml: TypedTag[String] = span(cls := (if mathMode then ClassDict.MATH_MODE else ClassDict.TEXT_MODE), raw(html))
    override def asHtmlReadOnly: TypedTag[String] = asHtml
    override def asLaTeX: String = latex
  }

  val times: ConvertableText = Symbol("×", "&times;", "\\times", mathMode = true)
  val gamma: ConvertableText = Symbol("Γ", "&Gamma;", "\\Gamma")
  val lambdaLower: ConvertableText = Symbol("λ", "&lambda;", "\\lambda", mathMode = true)
  val lambdaUpper: ConvertableText = Symbol("Λ", "&Lambda;", "\\Lambda", mathMode = true)
  val sigma: ConvertableText = Symbol("σ", "&sigma;", "\\sigma", mathMode = true)
  val tau: ConvertableText = Symbol("τ", "&tau;", "\\tau", mathMode = true)
  val forall: ConvertableText = Symbol("∀", "&forall;", "\\forall")
  val singleRightArrow: ConvertableText = Symbol("→", "&rarr;", "\\rightarrow")
  val doubleRightArrow: ConvertableText = Symbol("⇒", "&rArr;", "\\Rightarrow")
  val doubleDownArrow: ConvertableText = Symbol("⇓", "&dArr;", "\\Downarrow")
  val turnstile: ConvertableText = Symbol("⊢", "&vdash;", "\\vdash")
  val doubleStrokeN: ConvertableText = Symbol("ℕ", "&Nopf;", "\\mathbb{N}")
  val forwardSlash: ConvertableText = Symbol("/", "/", "\\slash")
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

object TermCommons {
  def e(n: Int): ConvertableText = MultiElement(MathElement("e"), SubscriptElement(MathElement(n.toString)))
  def v(n: Int): ConvertableText = MultiElement(MathElement("v"), SubscriptElement(MathElement(n.toString)))
  def t(n: Int): ConvertableText = MultiElement(Symbols.tau, SubscriptElement(MathElement(n.toString)))
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
