package actions

import languages.AbstractActionLanguage
import languages.terms.exprs.Expr
import nodes.*

case class ParseExprAction(
  override val originalTree: OuterNode,
  override val treePath: List[Int],
  override val lang: AbstractActionLanguage,
  exprText: String
) extends Action(originalTree, treePath, lang) {
  override lazy val newTree: OuterNode = originalTree.findChild(treePath) match {
    case Some(_: ExprNodeParent) =>
      parsedExpr match {
        case Some(expr) => originalTree.replace(treePath, ExprNode.fromExpr(lang, expr))
        case None => throw new Exception("Failed to parse expression")
      }
    case other => throw new Exception(s"Invalid parse expression target: $other")
  }

  private val parsedExpr: Option[Expr] = lang.parseExpr(exprText)
}
