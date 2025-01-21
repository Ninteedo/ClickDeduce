package convertors.text

import convertors.ClassDict
import scalatags.Text.TypedTag
import scalatags.Text.all.*

object Symbols {
  private case class Symbol(plain: String, html: String, latex: String, mode: Mode = Mode.Text)
      extends ConvertableText {
    override def asPlainText: String = plain

    override def asHtml: TypedTag[String] =
      span(cls := mode.toClass, raw(html))

    override def asHtmlReadOnly: TypedTag[String] = asHtml

    override def asLaTeX: String = latex
  }

  private enum Mode {
    case Text, Math, Number

    def toClass: String = this match {
      case Text => ClassDict.TEXT_MODE
      case Math => ClassDict.MATH_MODE
      case Number => ClassDict.NUMBER_MODE
    }
  }

  val times: ConvertableText = Symbol("×", "&times;", "\\times", mode = Mode.Math)
  val gamma: ConvertableText = Symbol("Γ", "&Gamma;", "\\Gamma")
  val lambdaLower: ConvertableText = Symbol("λ", "&lambda;", "\\lambda", mode = Mode.Math)
  val lambdaUpper: ConvertableText = Symbol("Λ", "&Lambda;", "\\Lambda")
  val sigma: ConvertableText = Symbol("σ", "&sigma;", "\\sigma", mode = Mode.Math)
  val tau: ConvertableText = Symbol("τ", "&tau;", "\\tau", mode = Mode.Math)
  val forall: ConvertableText = Symbol("∀", "&forall;", "\\forall")
  val singleRightArrow: ConvertableText = Symbol("→", "&rarr;", "\\rightarrow")
  val doubleRightArrow: ConvertableText = Symbol("⇒", "&rArr;", "\\Rightarrow")
  val doubleDownArrow: ConvertableText = Symbol("⇓", "&dArr;", "\\Downarrow")
  val turnstile: ConvertableText = Symbol("⊢", "&vdash;", "\\vdash")
  val doubleStrokeN: ConvertableText = Symbol("ℕ", "&Nopf;", "\\mathbb{N}")
  val forwardSlash: ConvertableText = Symbol("/", "/", "\\slash")
  val assign: ConvertableText = Symbol(":=", ":=", ":=", mode = Mode.Number)
}
