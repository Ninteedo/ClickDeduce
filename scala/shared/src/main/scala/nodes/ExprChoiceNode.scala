package nodes

import convertors.DisplayMode
import convertors.text.{ConvertableText, HtmlElement}
import languages.AbstractNodeLanguage
import languages.terms.blanks.BlankExprDropDown
import languages.terms.exprs.Expr
import scalatags.Text.all.*

/** Node representing an unselected expression.
  *
  * Displayed in the interface as a selector, where the user can choose an expression name.
  */
case class ExprChoiceNode(lang: AbstractNodeLanguage) extends ExprNodeParent(lang) {
  override val name: String = "ExprChoiceNode"

  override val args: List[InnerNode] = Nil

  override val children: List[OuterNode] = Nil
  override val exprName: String = "ExprChoice"
  private val expr = BlankExprDropDown(lang)

  override def toTextReadOnly(mode: DisplayMode): ConvertableText = toText(mode).toReadOnly

  override def toText(mode: DisplayMode): ConvertableText =
    HtmlElement(expr.toText.asHtml(data("tree-path") := treePathString), expr.toText)

  override def getExpr: Expr = expr

  override def toString: String = "ExprChoiceNode()"
}
