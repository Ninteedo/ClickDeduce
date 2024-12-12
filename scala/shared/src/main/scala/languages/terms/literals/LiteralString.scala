package languages.terms.literals

import app.UtilityFunctions
import convertors.{ConvertableText, TextElement}

/** A literal string.
    *
    * When entered, surrounded by double quotes. Also surrounded by double quotes when converted to text.
 *
 * @param value
    *   The string value.
    */
case class LiteralString(value: String) extends Literal {
  override lazy val toString: String = s"LiteralString(${UtilityFunctions.quote(value)})"

  override def toText: ConvertableText = TextElement(getValue)
}
