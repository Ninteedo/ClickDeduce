package actions

import actions.exceptions.IllegalMoveException
import languages.AbstractActionLanguage
import nodes.{ExprChoiceNode, ExprNodeParent, OuterNode, TypeNodeParent}

case class MoveAction(
  override val originalTree: OuterNode,
  override val treePath: List[Int],
  override val lang: AbstractActionLanguage,
  sourcePath: List[Int],
) extends Action (originalTree, treePath, lang) {
  override lazy val newTree: OuterNode = {
    if (treePath.startsWith(sourcePath)) {
        throw IllegalMoveException("Cannot move a node to a child of itself")
    }

    originalTree.findChild(treePath) match {
      case None => throw IllegalMoveException("Target node not found")
      case Some(targetNode: ExprNodeParent) => originalTree.findChild(sourcePath) match {
        case None => throw IllegalMoveException("Source node not found")
        case Some(sourceNode: ExprNodeParent) =>
          val removedSource = originalTree.replace(sourcePath, ExprChoiceNode(lang))
          removedSource.replace(treePath, sourceNode)
        case _ => throw IllegalMoveException("Mismatched node types, expr and type")
      }
      case Some(targetNode: TypeNodeParent) => originalTree.findChild(sourcePath) match {
        case None => throw IllegalMoveException("Source node not found")
        case Some(sourceNode: TypeNodeParent) =>
          val removedSource = originalTree.replace(sourcePath, ExprChoiceNode(lang))
          removedSource.replace(treePath, sourceNode)
        case _ => throw IllegalMoveException("Mismatched node types, expr and type")
      }
      case _ => throw IllegalMoveException("Unknown node type")
    }
  }
}
