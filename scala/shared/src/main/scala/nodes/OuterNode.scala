package nodes

import app.ClickDeduceException
import convertors.DisplayMode
import nodes.exceptions.InvalidTreePathException

/** Parent class for visible nodes in the expression tree.
  *
  * Has a list of inner nodes as arguments. Can be converted to HTML.
  */
abstract class OuterNode extends Node {

  override lazy val treePath: List[Int] = getParent match {
    case Some(value) =>
      val index: Int = value.args.indexWhere({
        case SubExprNode(node) => node eq this
        case SubTypeNode(node) => node eq this
        case _                 => false
      })
      if (index == -1) {
        if (isPhantom) Nil else throw new ClickDeduceException("Could not find self in parent node's args")
      } else value.treePath :+ index
    case None => Nil
  }
  /** The arguments of this node.
    *
    * This is different from the children of the node, as the children are the visible outer nodes, while the args are
    * the inner nodes.
    */
  val args: List[InnerNode]

  /** Mark this node as the root of the tree.
    */
  def markRoot(): Unit = {
    setParent(None)
  }

  /** The children of this node.
    *
    * This is the list of visible outer nodes.
    */
  def getVisibleChildren(mode: DisplayMode): List[OuterNode] = children

  /** Find the child of this expression tree at the given path.
    *
    * @param path
    *   the path to the child
    * @return
    *   the child at the given path, if it exists
    */
  def findChild(path: List[Int]): Option[Node] = path match {
    case Nil => Some(this)
    case head :: tail =>
      if (!args.indices.contains(head)) throw InvalidTreePathException(path)
      else {
        args(head) match {
          case SubExprNode(node) => node.findChild(tail)
          case SubTypeNode(node) => node.findChild(tail)
          case n: LiteralNode =>
            tail match {
              case Nil => Some(n)
              case _   => throw InvalidTreePathException(path)
            }
          case _ => None
        }
      }
  }

  /** Find the index of the given node in the args list.
    *
    * This accepts both [[InnerNode]]s and OuterNodes.
    * @param node
    *   The node to find.
    * @return
    *   The index of the node in the args list.
    */
  def indexOf(node: Node): Int = node match {
    case n: InnerNode => args.indexWhere(_ eq n)
    case n: OuterNode => args.indexWhere(_.children.exists(_ eq n))
  }

  /** Create a new outer node where the node at the given path is replaced with the given replacement.
    * @param path
    *   The path to the node to replace.
    * @param replacement
    *   The replacement node.
    * @return
    *   The new outer node.
    */
  def replace(path: List[Int], replacement: Node): OuterNode = path match {
    case Nil =>
      replacement match {
        case n: OuterNode => n
      }
    case head :: tail =>
      val updatedArgs = args.updated(
        head,
        args(head) match {
          case SubExprNode(node) =>
            SubExprNode(node.replace(tail, replacement) match {
              case n: ExprNodeParent => n
            })
          case SubTypeNode(node) =>
            SubTypeNode(node.replace(tail, replacement) match {
              case n: TypeNodeParent => n
            })
          case LiteralNode(literalText) =>
            tail match {
              case Nil =>
                replacement match {
                  case n: InnerNode => n
                }
              case _ => throw InvalidTreePathException(path)
            }
        }
      )

      this match {
        case ExprNode(lang, exprName, _) => ExprNode(lang, exprName, updatedArgs)
        case TypeNode(lang, typeName, _) => TypeNode(lang, typeName, updatedArgs)
      }
  }

  /** Whether this node is a phantom node.
    *
    * Phantom nodes are not part of the tree structure, but can appear during rendering.
    * @return
    *   whether this node is a phantom node
    */
  def isPhantom: Boolean = false
}
