package languages.terms.literals

import app.HTMLHelper
import convertors.text.{ConvertableText, MathElement}
import languages.env.{TypeEnv, ValueEnv}
import scalatags.Text.TypedTag

/** A literal integer.
 *
 * Can store extremely large integers (using [[BigInt]]).
 *
 * @param value
 *   The integer value.
 */
case class LiteralInt(value: BigInt) extends Literal {
  override def toText: ConvertableText = MathElement(getValue)

  override def toHtmlInput(treePath: String, env: ValueEnv | TypeEnv): TypedTag[String] = HTMLHelper
    .literalInputBase(treePath, getValue, inputKind = "number", extraClasses = "integer")
}

object LiteralInt {
  def fromString(s: String): LiteralInt = try {
    LiteralInt(BigInt(s))
  } catch {
    case _: NumberFormatException =>
      throw LiteralParseException(s"LiteralInt only accepts integer values, not \"$s\"")
  }

  val default: LiteralInt = LiteralInt(0)
}
