package languages.terms.literals

import app.HTMLHelper
import languages.env.{TypeEnv, ValueEnv}
import languages.terms.Term
import scalatags.Text.TypedTag
import scalatags.Text.all.*

/** A literal term. Entered as a string. */
abstract class Literal extends Term {
  override lazy val toHtml: TypedTag[String] = p(getValue)
  val value: Any
  val defaultContents: String = ""
  private var overrideContents: Option[String] = None

  def toHtmlInput(treePath: String, env: ValueEnv | TypeEnv): TypedTag[String] =
    HTMLHelper.literalInputBase(treePath, getValue)

  def toHtmlInputReadOnly(originPath: String): TypedTag[String] =
    HTMLHelper.literalInputBaseReadOnly(originPath, getValue)

  def getValue: String = overrideContents.getOrElse(value.toString)

  def setOverrideContents(contents: String): Unit = overrideContents = Some(contents)
}

/** Companion object for [[Literal]].
  */
object Literal {
  def fromStringOfType(s: String, l: Class[_ <: Literal]): Literal = l match {
    case c if c == classOf[LiteralBool]             => LiteralBool(s.toBoolean)
    case c if c == classOf[LiteralString]           => LiteralString(s)
    case c if c == classOf[LiteralInt]              => LiteralInt.fromString(s)
    case c if c == classOf[LiteralIdentifierBind]   => LiteralIdentifierBind(s)
    case c if c == classOf[LiteralIdentifierLookup] => LiteralIdentifierLookup(s)
    case c if c == classOf[LiteralAny]              => LiteralAny(s)
    case _                                          => throw LiteralParseException(s)
  }

  def placeholderOf(literal: Literal, contents: String): Literal = {
    val copy = literal match {
      case LiteralInt(n)              => LiteralInt(n)
      case LiteralBool(b)             => LiteralBool(b)
      case LiteralString(s)           => LiteralString(s)
      case LiteralIdentifierBind(s)   => LiteralIdentifierBind(s)
      case LiteralIdentifierLookup(s) => LiteralIdentifierLookup(s)
      case LiteralAny(s)              => LiteralAny(s)
    }
    copy.setOverrideContents(contents)
    copy
  }
}
