package languages.terms

import convertors.{BracketedElement, ConvertableText, HTMLConvertor, LaTeXConvertor}
import languages.env.{TypeEnv, ValueEnv}
import languages.terms.exprs.Expr
import languages.terms.types.Type
import languages.terms.values.Value
import scalatags.Text.TypedTag

/** A term in the language.
 *
 * Base trait for all abstract language features in a language.
 *
 * Terms can be [[Expr]]s, [[Value]]s, [[Type]]s, or [[Literal]]s.
 */
trait Term {

  /** The name of this term.
   *
   * This is the name of the class by default, but can be overridden.
   */
  val name: String = toString.takeWhile(_ != '(')

  def getChildrenBase(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] = Nil

  def getChildrenEval(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] = Nil

  def getChildrenTypeCheck(tEnv: TypeEnv = TypeEnv.empty): List[(Term, TypeEnv)] = Nil

  /** Whether this term is a placeholder.
   *
   * Placeholders are used when converting terms to text, and are not evaluated or type-checked.
   *
   * Default is false, should only be overridden for placeholder terms.
   *
   * @return
   *   True if this term is a placeholder, false otherwise.
   */
  def isPlaceholder: Boolean = false

  /** Whether this term should be surrounded by brackets when converted to text inside another term.
   *
   * Default is true, should only be overridden for terms that should not be bracketed in any context. This is common
   * when a term is displayed as a single element, for example a number or variable.
   *
   * A term is bracketed if this is true and it is being called by [[toTextBracketed]].
   */
  val needsBrackets: Boolean = true

  /** Convert this term to [[ConvertableText]].
   *
   * This is used as a standard output format for different convertors, like [[HTMLConvertor]] and
   * [[LaTeXConvertor]].
   *
   * @return
   *   This term as [[ConvertableText]].
   */
  def toText: ConvertableText

  /** Convert this term to [[ConvertableText]], surrounded by brackets if [[needsBrackets]] is true.
   */
  final def toTextBracketed: ConvertableText = if (needsBrackets) BracketedElement(toText) else toText

  /** Convert this term to HTML.
   */
  lazy val toHtml: TypedTag[String] = toText.asHtml

  /** Convert this term to plain text.
   * @return
   *   The plain text representation of this term.
   */
  final def prettyPrint: String = toText.asPlainText

  /** Convert this term to plain text, surrounded by brackets if [[needsBrackets]] is true.
   * @return
   *   The plain text representation of this term.
   */
  final def prettyPrintBracketed: String = if (needsBrackets) s"($prettyPrint)" else prettyPrint
}
