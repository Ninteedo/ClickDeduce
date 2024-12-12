package nodes

import convertors.{ConvertableText, DisplayMode}
import languages.terms.types.TypePlaceholder

/** An inner node that contains a sub-type.
  *
  * @param node
  *   The sub-type node.
  */
case class SubTypeNode(node: TypeNodeParent) extends InnerNode {
  override val name: String = "SubTypeNode"

  override val children: List[OuterNode] = List(node)

  override def toText(mode: DisplayMode): ConvertableText = node.toText(mode)

  override def toTextReadOnly(mode: DisplayMode): ConvertableText = node.toTextReadOnly(mode)

  override def getPlaceholder(mode: DisplayMode, readOnly: Boolean = true): TypePlaceholder =
    TypePlaceholder(node.toTextReadOnly(mode), node.getType.needsBrackets)
}
