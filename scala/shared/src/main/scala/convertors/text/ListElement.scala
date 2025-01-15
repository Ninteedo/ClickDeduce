package convertors.text

import scalatags.Text.TypedTag
import scalatags.Text.all.*

case class ListElement(
  items: Seq[ConvertableText],
  start: ConvertableText = TextElement("["),
  end: ConvertableText = TextElement("]"),
  delimiter: ConvertableText = TextElement(", ")
) extends ConvertableText {
  override def asPlainText: String = items
    .map(_.asPlainText)
    .mkString(start.asPlainText, delimiter.asPlainText, end.asPlainText)

  override def asHtml: TypedTag[String] =
    span(start.asHtml, intersperse(items.map(_.asHtml).toList, delimiter.asHtml), end.asHtml)

  override def asHtmlReadOnly: TypedTag[String] = span(
    start.asHtmlReadOnly,
    intersperse(items.map(_.asHtmlReadOnly).toList, delimiter.asHtmlReadOnly),
    end.asHtmlReadOnly
  )

  override def asLaTeX: String = items.map(_.asLaTeX).mkString(start.asLaTeX, delimiter.asLaTeX, end.asLaTeX)

  private def intersperse[T](items: List[T], sep: T): List[T] = items match {
    case Nil      => Nil
    case x :: Nil => List(x)
    case x :: xs  => x :: sep :: intersperse(xs, sep)
  }
}
