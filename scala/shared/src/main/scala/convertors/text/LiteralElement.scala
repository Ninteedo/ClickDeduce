package convertors.text

import languages.terms.literals.Literal
import scalatags.Text.TypedTag
import scalatags.Text.all.{disabled, readonly}

case class LiteralElement(literal: Literal) extends ConvertableText {
  override def asPlainText: String = literal.toString

  override def asHtml: TypedTag[String] = literal.toHtml

  override def asHtmlReadOnly: TypedTag[String] = literal.toHtml(readonly, disabled)

  override def asLaTeX: String = literal.toText.asLaTeX
}
