package languages.terms.blanks

import convertors.{ConvertableText, HtmlElement, TextElement}
import languages.AbstractNodeLanguage
import languages.terms.types.Type
import scalatags.Text.TypedTag

/** Term representing an unselected type.
  *
  * Will always type-check to an error.
  */
case class BlankTypeDropDown(lang: AbstractNodeLanguage) extends Type, BlankSpace {
  override lazy val toHtml: TypedTag[String] = lang.typeClassListDropdownHtml

  override val needsBrackets: Boolean = false

  override def toText: ConvertableText = HtmlElement(lang.typeClassListDropdownHtml, TextElement("Unselected Type"))
}
