package languages.terms.blanks

import convertors.text.{ConvertableText, HtmlElement, TextElement}
import languages.AbstractNodeLanguage
import languages.terms.exprs.NotImplementedExpr
import scalatags.Text.TypedTag

/** Term representing an unselected expression.
  *
  * Will always evaluate and type-check to an error.
  */
case class BlankExprDropDown(lang: AbstractNodeLanguage) extends NotImplementedExpr, BlankSpace {
  override lazy val toHtml: TypedTag[String] = lang.exprClassListDropdownHtml

  override val needsBrackets: Boolean = false

  override def toText: ConvertableText = HtmlElement(lang.exprClassListDropdownHtml, TextElement("_"))
}
