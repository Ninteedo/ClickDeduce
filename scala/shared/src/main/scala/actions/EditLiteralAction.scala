package actions

import actions.exceptions.InvalidEditTargetException
import languages.AbstractActionLanguage
import languages.terms.literals.Literal
import nodes.{LiteralNode, OuterNode}

case class EditLiteralAction(
  override val originalTree: OuterNode,
  override val treePath: List[Int],
  override val lang: AbstractActionLanguage,
  newLiteralText: String
) extends Action(originalTree, treePath, lang) {
  override lazy val newTree: OuterNode = originalTree.findChild(treePath) match {
    case Some(literalNode: LiteralNode) =>
      originalTree.replace(
        treePath,
        LiteralNode(Literal.fromStringOfType(newLiteralText, literalNode.literal.getClass))
      )
    case other => throw new InvalidEditTargetException(other)
  }
}
