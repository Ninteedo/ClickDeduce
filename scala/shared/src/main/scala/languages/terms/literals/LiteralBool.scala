package languages.terms.literals

import app.HTMLHelper
import convertors.ClassDict
import convertors.text.{ConvertableText, TextElement}
import languages.env.{TypeEnv, ValueEnv}
import scalatags.Text.TypedTag
import scalatags.Text.all.*

/** A literal boolean.
  *
  * @param value
  *   The boolean value.
  */
case class LiteralBool(value: Boolean) extends Literal {
  override def toText: ConvertableText = TextElement(getValue)

  override def toHtmlInput(treePath: String, env: ValueEnv | TypeEnv): TypedTag[String] = div(
    cls := "literal-checkbox-container",
    input(
      `type` := "checkbox",
      data("tree-path") := treePath,
      cls := ClassDict.LITERAL + " " + "boolean",
      if (getValue.toBoolean) checked else ()
    )
  )

  override def toHtmlInputReadOnly(originPath: String): TypedTag[String] =
    HTMLHelper.literalInputBaseReadOnly(originPath, getValue, extraClasses = "boolean")
}
