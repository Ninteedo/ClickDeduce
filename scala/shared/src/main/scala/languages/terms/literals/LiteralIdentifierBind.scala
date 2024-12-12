package languages.terms.literals

import app.UtilityFunctions
import convertors.{ConvertableText, TextElement}
import scalatags.Text.all.s

/** A literal identifier used for binding a variable.
 *
 * @param value
    *   The identifier value.
    */
case class LiteralIdentifierBind(value: String) extends LiteralIdentifier {
  override lazy val toString: String = s"LiteralIdentifierBind(${UtilityFunctions.quote(value)})"

  override def toText: ConvertableText = TextElement(getValue)

  /** Convert this to a [[LiteralIdentifierLookup]] with the same identifier.
    */
  def toLookup: LiteralIdentifierLookup = LiteralIdentifierLookup(value)
}

object LiteralIdentifierBind {
  val default: LiteralIdentifierBind = LiteralIdentifierBind("")
}
