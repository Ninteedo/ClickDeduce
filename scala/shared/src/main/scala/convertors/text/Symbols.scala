package convertors.text

import convertors.ClassDict
import scalatags.Text.TypedTag
import scalatags.Text.all.*

object Symbols {
  private case class Symbol(plain: String, html: String, latex: String, mathMode: Boolean = false)
      extends ConvertableText {
    override def asPlainText: String = plain

    override def asHtml: TypedTag[String] =
      span(cls := (if mathMode then ClassDict.MATH_MODE else ClassDict.TEXT_MODE), raw(html))

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
