package actions

import actions.exceptions.{InvalidSelectTargetException, InvalidSelectValueNameException}
import languages.AbstractActionLanguage
import nodes.{OuterNode, TypeChoiceNode, TypeNode}

case class SelectTypeAction(
  override val originalTree: OuterNode,
  override val treePath: List[Int],
  override val lang: AbstractActionLanguage,
  typeChoiceName: String
) extends Action(originalTree, treePath, lang) {
  override lazy val newTree: OuterNode = {
    val typeNode = TypeNode.fromTypeName(lang, typeChoiceName)
    if (typeNode.isEmpty) throw new InvalidSelectValueNameException(typeChoiceName)
    originalTree.findChild(treePath) match {
      case Some(typeChoiceNode: TypeChoiceNode) =>
        originalTree.replace(treePath, typeNode.get)
      case other => throw new InvalidSelectTargetException(other)
    }
  }
}
