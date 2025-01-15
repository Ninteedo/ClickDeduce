package convertors.text

import scalatags.Text.TypedTag
import scalatags.Text.all.div

case class MultiElement(elems: ConvertableText*) extends ConvertableText {
  override def asPlainText: String = elems.map(_.asPlainText).mkString

  override def asHtml: TypedTag[String] = div(elems.map(_.asHtml): _*)

  override def asHtmlReadOnly: TypedTag[String] = div(elems.map(_.asHtmlReadOnly): _*)

  override def asLaTeX: String = elems.map(_.asLaTeX).mkString
}
