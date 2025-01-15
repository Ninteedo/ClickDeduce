package nodes

import convertors.DisplayMode
import convertors.text.ConvertableText
import languages.terms.exprs.ExprPlaceholder
import nodes.exceptions.{InnerNodeCannotBeRootException, NodeParentWrongTypeException}

/** An inner node that contains a sub-expression.
  *
  * @param node
  *   The sub-expression node.
  */
case class SubExprNode(node: ExprNodeParent) extends InnerNode {
  override val name: String = "SubExprNode"
  override val children: List[ExprNodeParent] = List(node)

  override def getParent: Option[ExprNodeParent] = super.getParent match {
    case Some(n: ExprNodeParent) => Some(n)
    case None              => None
    case Some(n)           => throw NodeParentWrongTypeException("ExprNode", n.name)
  }

  override def setParent(parentNode: Option[OuterNode]): Unit = parentNode match {
    case Some(n: ExprNodeParent) => super.setParent(Some(n))
    case None              => throw InnerNodeCannotBeRootException()
    case Some(n)           => throw NodeParentWrongTypeException("ExprName", n.name)
  }

  override def toText(mode: DisplayMode): ConvertableText = node.toText(mode)

  override def toTextReadOnly(mode: DisplayMode): ConvertableText = node.toTextReadOnly(mode)

  override def getPlaceholder(mode: DisplayMode, readOnly: Boolean = true): ExprPlaceholder =
    ExprPlaceholder(node.toTextReadOnly(mode), node.getExpr.needsBrackets)
}
