package actions

import actions.exceptions.{InvalidSelectTargetException, InvalidSelectValueNameException}
import languages.AbstractActionLanguage
import nodes.{ExprChoiceNode, ExprNode, OuterNode}

case class SelectExprAction(
  override val originalTree: OuterNode,
  override val treePath: List[Int],
  override val lang: AbstractActionLanguage,
  exprChoiceName: String
) extends Action(originalTree, treePath, lang) {
  override lazy val newTree: OuterNode = {
    if (lang.getExprBuilder(exprChoiceName).isEmpty) throw new InvalidSelectValueNameException(exprChoiceName)

    val exprNode = ExprNode.createFromExprName(lang, exprChoiceName)
    if (exprNode.isEmpty) throw new InvalidSelectValueNameException(exprChoiceName)
    originalTree.findChild(treePath) match {
      case Some(exprChoiceNode: ExprChoiceNode) =>
        originalTree.replace(treePath, exprNode.get)
      case other => throw new InvalidSelectTargetException(other)
    }
  }
}
