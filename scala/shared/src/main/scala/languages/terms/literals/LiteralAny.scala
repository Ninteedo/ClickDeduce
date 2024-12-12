package languages.terms.literals

import app.UtilityFunctions
import convertors.{ConvertableText, TextElement}
import scalatags.Text.all.s

/** A literal with no restrictions.
  *
  * @param value
  *   The value.
  */
case class LiteralAny(value: String) extends Literal {
  override lazy val toString: String = s"LiteralAny(${UtilityFunctions.quote(value)})"

  override def toText: ConvertableText = TextElement(getValue)
}
