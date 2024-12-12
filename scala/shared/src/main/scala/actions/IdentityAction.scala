package actions

import languages.AbstractActionLanguage
import nodes.OuterNode

case class IdentityAction(
  override val originalTree: OuterNode,
  override val treePath: List[Int],
  override val lang: AbstractActionLanguage
) extends Action(originalTree, treePath, lang) {
  override lazy val newTree: OuterNode = originalTree
}
