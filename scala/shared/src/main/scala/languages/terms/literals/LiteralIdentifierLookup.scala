package languages.terms.literals

import app.{HTMLHelper, UtilityFunctions}
import convertors.text.{ConvertableText, ItalicsElement, TextElement}
import languages.env.{TypeEnv, ValueEnv}
import languages.terms.values.Value
import scalatags.Text.TypedTag
import scalatags.Text.all.*

import scala.util.matching.Regex

/** A literal identifier lookup.
  *
  * @param value
  *   The identifier value.
  */
case class LiteralIdentifierLookup(value: String) extends LiteralIdentifier {
  override lazy val toString: String = s"LiteralIdentifierLookup(${UtilityFunctions.quote(value)})"

  override def toText: ConvertableText = ItalicsElement(TextElement(getValue))

  /** Create an input for this literal identifier, with a dropdown showing bound variables in the environment.
    * @param treePath
    *   The path to this literal in the tree.
    * @param env
    *   The environment.
    * @return
    *   The HTML input.
    */
  override def toHtmlInput(treePath: String, env: ValueEnv | TypeEnv): TypedTag[String] = {
    div(
      cls := "literal-identifier-container",
      HTMLHelper.literalInputBase(treePath, getValue, extraClasses = "identifier-lookup"),
      div(
        cls := "dropdown",
        ul(
          cls := "identifier-suggestions",
          env.toMap
            .filter({
              case (_, v: Value) => v.showInValueLookupList
              case _             => true
            })
            .map((k, v) => li(data("value") := k, data("filter") := k, span(k, ": ", v.toHtml)))
            .toSeq
        )
      )
    )
  }

  /** Convert this to a [[LiteralIdentifierBind]] with the same identifier.
    */
  def toBind: LiteralIdentifierBind = LiteralIdentifierBind(value)
}

object LiteralIdentifierLookup {
  val identifierRegex: Regex = "[A-Za-z_$][\\w_$]*".r

  val default: LiteralIdentifierLookup = LiteralIdentifierLookup("")
}
