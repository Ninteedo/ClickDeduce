package languages.terms.values

import convertors.ConvertableText
import languages.terms.types.{Type, TypePlaceholder}

/** A placeholder for text in a value.
 *
 * @param content
 * The text content.
 * @param needsBrackets
 * Whether this placeholder should be surrounded by brackets when converted to text.
 */
case class ValuePlaceholder(content: ConvertableText, override val needsBrackets: Boolean = false) extends Value {
  override val typ: Type = TypePlaceholder(content, needsBrackets)

  override def toText: ConvertableText = content
}

/** Companion object for [[ValuePlaceholder]].
 */
object ValuePlaceholder {

  /** Create a value placeholder from a value.
   *
   * @param value
   * The value.
   * @return
   * The value placeholder.
   */
  def apply(value: Value): ValuePlaceholder = ValuePlaceholder(value.toText, value.needsBrackets)
}
