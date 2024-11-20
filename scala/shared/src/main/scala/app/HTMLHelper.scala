package app

import convertors.ClassDict
import scalatags.Text.TypedTag
import scalatags.Text.all.*


object HTMLHelper {
  def literalInputBase(
    treePath: String,
    inputValue: String,
    inputKind: String = "text",
    extraClasses: String | List[String] = Nil
  ): TypedTag[String] = {
    input(
      `type` := inputKind,
      cls := literalClassList(extraClasses),
      width := Math.max(2, inputValue.length).toString + "ch",
      data("tree-path") := treePath,
      value := inputValue,
    )
  }

  def literalInputBaseReadOnly(
    originPath: String,
    inputValue: String,
    inputKind: String = "text",
    extraClasses: String | List[String] = Nil
  ): TypedTag[String] = {
    input(
      `type` := inputKind,
      cls := literalClassList(extraClasses),
      width := Math.max(1, inputValue.length).toString + "ch",
      data("origin") := originPath,
      value := inputValue,
      readonly,
      disabled,
    )
  }

  def literalClassList(extraClasses: String | List[String] = Nil): String = {
    (ClassDict.LITERAL :: (extraClasses match {
      case s: String => List(s)
      case l: List[String] => l
    })).mkString(" ")
  }
}
