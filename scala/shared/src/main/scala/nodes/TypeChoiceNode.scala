package nodes

import convertors.{ConvertableText, DisplayMode, HtmlElement}
import languages.AbstractNodeLanguage
import languages.terms.blanks.BlankTypeDropDown
import languages.terms.types.{Type, UnknownType}
import scalatags.Text.all.*

/** Node representing an unselected type.
  *
  * Displayed in the interface as a selector, where the user can choose a type name.
  */
case class TypeChoiceNode(lang: AbstractNodeLanguage) extends TypeNodeParent {
  override lazy val getType: Type = UnknownType()
  override val name: String = "TypeChoiceNode"
  override val args: List[InnerNode] = Nil
  private val expr = BlankTypeDropDown(lang)

  override def toTextReadOnly(mode: DisplayMode): ConvertableText = toText(mode).toReadOnly

  override def toText(mode: DisplayMode): ConvertableText =
    HtmlElement(expr.toText.asHtml(data("tree-path") := treePathString), expr.toText)

  override def toString: String = "TypeChoiceNode()"
}
