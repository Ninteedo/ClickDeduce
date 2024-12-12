package actions

import languages.AbstractActionLanguage
import nodes.OuterNode

abstract class Action(val originalTree: OuterNode, val treePath: List[Int], val lang: AbstractActionLanguage) {
  lazy val newTree: OuterNode
}
