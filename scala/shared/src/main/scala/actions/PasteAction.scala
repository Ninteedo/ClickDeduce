package actions

import actions.exceptions.InvalidPasteTargetException
import languages.AbstractActionLanguage
import nodes.{ExprNodeParent, Node, OuterNode, TypeNodeParent}

case class PasteAction(
  override val originalTree: OuterNode,
  override val treePath: List[Int],
  override val lang: AbstractActionLanguage,
  pasteNodeString: String
) extends Action(originalTree, treePath, lang) {
  override lazy val newTree: OuterNode = originalTree.findChild(treePath) match {
    case Some(_: ExprNodeParent) =>
      pasteNode match {
        case _: ExprNodeParent => originalTree.replace(treePath, pasteNode)
        case _                 => throw new InvalidPasteTargetException(Some(pasteNode))
      }
    case Some(_: TypeNodeParent) =>
      pasteNode match {
        case _: TypeNodeParent => originalTree.replace(treePath, pasteNode)
        case _                 => throw new InvalidPasteTargetException(Some(pasteNode))
      }
    case other => throw new InvalidPasteTargetException(other)
  }
  private val pasteNode: Node = Node.read(lang, pasteNodeString).get
}
