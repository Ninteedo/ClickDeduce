package nodes

import app.UtilityFunctions
import convertors.DisplayMode
import convertors.text.ConvertableText
import languages.terms.exprs.Expr
import languages.terms.literals.{Literal, LiteralParser}
import languages.terms.types.Type
import languages.{AbstractNodeLanguage, ClickDeduceLanguage}
import nodes.exceptions.{InvalidTreePathStringException, NodeParentNotInitialisedException, NodeStringParseException}

import scala.util.parsing.combinator.JavaTokenParsers


/** Superclass for all nodes in the expression tree.
 *
 * Has a parent (or no parent if root) and a list of children.
 */
abstract class Node {

  /** The name of this node kind.
   */
  val name: String

  /** The outer nodes that this node is the parent of.
   */
  val children: List[OuterNode] = Nil

  /** Internal store for the parent of this node.
   *
   * If the parent is not initialised, this will be `None`.
   *
   * If the node is the root of the tree, this will be `Some(None)`.
   *
   * If the parent is initialised, this will be `Some(parentNode)`.
   */
  private var parent: Option[Option[OuterNode]] = None

  private var parentInitialised = false

  /** The parent of this node.
   *
   * If the node is the root of the tree, this will be `None`. If the parent is set, this will be `Some(parentNode)`.
   *
   * @throws NodeParentNotInitialisedException
   *   if the parent is not initialised
   * @return
   *   the parent of this node
   */
  def getParent: Option[OuterNode] = parent match {
    case Some(value) => value
    case None        => throw NodeParentNotInitialisedException()
  }

  /** Set the parent of this node.
   *
   * @param parentNode
   *   the parent node
   */
  def setParent(parentNode: Option[OuterNode]): Unit = {
    parent = Some(parentNode)
    markParentInitialised()
  }

  private def markParentInitialised(): Unit = {
    parentInitialised = true
  }

  /** Whether the parent of this node has been initialised.
   * @return
   *   whether the parent of this node has been initialised
   */
  def isParentInitialised: Boolean = parentInitialised

  /** The path from the root of the tree to this node.
   */
  lazy val treePath: List[Int] = getParent match {
    case Some(value) => value.treePath :+ value.args.indexWhere(_ eq this)
    case None        => Nil
  }

  /** The string representation of the path from the root of the tree to this node.
   *
   * This is a string of the form "0-1-2" where each number is the index of the child in the parent's list of
   * children.
   */
  lazy val treePathString: String = treePath.mkString("-")

  /** This node's text representation.
   * @param mode
   *   The display mode.
   * @return
   *   The text representation.
   */
  def toText(mode: DisplayMode): ConvertableText

  /** This node's text representation in read-only mode.
   * @param mode
   *   The display mode.
   * @return
   *   The text representation.
   */
  def toTextReadOnly(mode: DisplayMode): ConvertableText
}

/** Companion object for the [[Node]] class.
 */
object Node {

  /** Creates a node from a correct string representation of a node (from Node.toString).
   */
  def read(lang: AbstractNodeLanguage, s: String): Option[Node] = {
    def makeNode(name: String, args: List[Any]): Option[Node] = instantiate(lang, name, args)

    object NodeParser extends JavaTokenParsers with LiteralParser {
      def outerNode: Parser[Option[OuterNode | Expr | Type]] =
        outerNodeName ~ "(" ~ repsep(outerNodeArg, "\\s*,\\s*".r) ~ ")" ^^ {
          case name ~ "(" ~ args ~ ")" =>
            makeNode(name, args) match {
              case Some(n: OuterNode) =>
                n.children.foreach(_.setParent(Some(n)))
                Some(n)
              case _ => throw NodeStringParseException(s"$name(${args.mkString(", ")})")
            }
          case _ => None
        }

      def name: Parser[String] = "[A-Za-z]\\w*".r

      def outerListParse: Parser[List[Any]] =
        "Nil" ^^ { _ => Nil } | "List(" ~ repsep(outerNodeArg, "\\s*,\\s*".r) ~ ")" ^^ { case _ ~ args ~ _ =>
          args.filter(_ != None).map {
            case Some(e) => e
            case x       => x
          }
        }

      def outerNodeName: Parser[String] = "ExprChoiceNode" | "VariableNode" | "TypeChoiceNode" | "TypeNode"

      def innerNodeName: Parser[String] = "SubExprNode" | "LiteralNode" | "SubTypeNode"

      def outerNodeArg: Parser[Any] =
        outerListParse | innerNode | stringLiteral ^^ { s => UtilityFunctions.unquote(s) }

      def innerNodeArg: Parser[Any] =
        outerNode | literal | stringLiteral ^^ { s => UtilityFunctions.unquote(s) }

      def innerNode: Parser[InnerNode] = innerNodeName ~ "(" ~ repsep(innerNodeArg, "\\s*,\\s*".r) ~ ")" ^^ {
        case name ~ "(" ~ args ~ ")" =>
          makeNode(name, args) match {
            case Some(n: InnerNode) => n
            case _                  => throw new NodeStringParseException(s"$name(${args.mkString(", ")})")
          }
        case other => throw new NodeStringParseException(other.toString)
      }

      def parseNode(s: String): ParseResult[Option[Node | Expr | Type]] = parseAll(outerNode, s.strip())
    }

    NodeParser.parseNode(s) match {
      case NodeParser.Success(Some(matched: Node), _) => Some(matched)
      case x                                          => None
    }
  }

  /** Read a path string into a list of integers.
   *
   * Example of the expected format is "0-1-2".
   * @param s
   *   The path string.
   * @return
   *   The tree path.
   * @throws InvalidTreePathStringException
   *   If the path string is not in the expected format.
   */
  def readPathString(s: String): List[Int] = s match {
    case "" => Nil
    case s =>
      if (s.split("-").forall(_.matches("\\d+"))) s.split("-").map(_.toInt).toList
      else throw InvalidTreePathStringException(s)
  }

  /** Create a node of a given type with the given arguments.
   * @param nodeName
   *   The name of the node type.
   * @param args
   *   The arguments for the node.
   * @return
   *   The created node, if successful.
   */
  def instantiate(lang: AbstractNodeLanguage, nodeName: String, args: List[Any]): Option[Node] = {
    val parsedArgs = args.map({
      case l: Literal => l
      case Some(e)    => e
      case other      => other
    })
    nodeName match {
      case "VariableNode" =>
        parsedArgs match {
          case List(exprName: String, innerNodes: List[InnerNode]) => Some(ExprNode(lang, exprName, innerNodes))
          case _                                                   => None
        }
      case "ExprChoiceNode" =>
        parsedArgs match {
          case Nil => Some(ExprChoiceNode(lang))
          case _   => None
        }
      case "TypeChoiceNode" =>
        parsedArgs match {
          case Nil => Some(TypeChoiceNode(lang))
          case _   => None
        }
      case "TypeNode" =>
        parsedArgs match {
          case List(typeName: String, innerNodes: List[InnerNode]) => Some(TypeNode(lang, typeName, innerNodes))
          case _                                                   => None
        }
      case "SubExprNode" =>
        parsedArgs match {
          case List(node: ExprNodeParent) => Some(SubExprNode(node))
          case _                    => None
        }
      case "SubTypeNode" =>
        parsedArgs match {
          case List(node: TypeNodeParent) => Some(SubTypeNode(node))
          case _                          => None
        }
      case "LiteralNode" =>
        parsedArgs match {
          case List(l: Literal) => Some(LiteralNode(l))
          case _                => None
        }
      case _ => None
    }
  }
}
