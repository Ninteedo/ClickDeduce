package actions

import actions.exceptions.InvalidDeleteTargetException
import languages.AbstractActionLanguage
import nodes.*

case class DeleteAction(
  override val originalTree: OuterNode,
  override val treePath: List[Int],
  override val lang: AbstractActionLanguage
) extends Action(originalTree, treePath, lang) {
  override lazy val newTree: OuterNode = originalTree.findChild(treePath) match {
    case Some(_: ExprNode)       => originalTree.replace(treePath, ExprChoiceNode(lang))
    case Some(_: TypeNodeParent) => originalTree.replace(treePath, TypeChoiceNode(lang))
    case other                   => throw new InvalidDeleteTargetException(other)
  }
}
