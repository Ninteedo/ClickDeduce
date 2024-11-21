package languages

import app.{ClickDeduceException, UtilityFunctions}
import convertors.*
import scalatags.Text.TypedTag
import scalatags.Text.all.*

import scala.util.parsing.combinator.*

/** Defines the abstract syntax tree structure.
  *
  * The abstract [[Node]] class is the parent for all tree nodes.
  */
trait AbstractNodeLanguage extends AbstractLanguage {
  lang =>

  trait BlankSpace extends Term {
    override lazy val toHtml: TypedTag[String] = {
      input(`type` := "text", placeholder := "Term")
    }
  }

  /** Term representing an unselected expression.
    *
    * Will always evaluate and type-check to an error.
    */
  case class BlankExprDropDown() extends NotImplementedExpr, BlankSpace {
    override lazy val toHtml: TypedTag[String] = exprClassListDropdownHtml

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = HtmlElement(exprClassListDropdownHtml, TextElement("Unselected Expression"))
  }

  val defaultExpr: Expr = BlankExprDropDown()

  /** Term representing an unselected type.
    *
    * Will always type-check to an error.
    */
  case class BlankTypeDropDown() extends Type, BlankSpace {
    override lazy val toHtml: TypedTag[String] = typeClassListDropdownHtml

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = HtmlElement(typeClassListDropdownHtml, TextElement("Unselected Type"))
  }

  val defaultType: Type = BlankTypeDropDown()

  private lazy val exprClassListDropdownHtml: TypedTag[String] = {
    def createExprOption(exprBuilderName: BuilderName): TypedTag[String] = {
      val langName = exprBuilderName._1
      exprBuilderName._2 match {
        case name: String => option(data("value") := name, name)
        case (name: String, aliases: List[String]) =>
          option(data("value") := name, name, data("aliases") := aliases.mkString(","), data("lang") := langName)
      }
    }

    select(
      cls := ClassDict.EXPR_DROPDOWN,
      option(value := "", "Select Expr..."),
      exprBuilderNames.map(createExprOption)
    )
  }

  private lazy val typeClassListDropdownHtml: TypedTag[String] = {
    def createTypeOption(typeBuilderName: BuilderName) = {
      val langName = typeBuilderName._1
      typeBuilderName._2 match {
        case name: String => option(data("value") := name, name)
        case (name: String, aliases: List[String]) =>
          option(data("value") := name, name, data("aliases") := aliases.mkString(","))
      }
    }

    select(
      cls := ClassDict.TYPE_DROPDOWN,
      option(value := "", "Select Type..."),
      typeBuilderNames.map(createTypeOption)
    )
  }

  /** Evaluation error for when the depth limit is exceeded.
    */
  case class StackOverflowErrorValue() extends EvalError {
    override val message: String = "Stack overflow error"

    override val typ: Type = StackOverflowErrorType()
  }

  /** Type error for when the depth limit is exceeded.
    */
  case class StackOverflowErrorType() extends TypeError {
    override val message: String = "Stack overflow error"
  }

  /** Create a `Term` given its string representation.
    *
    * @return
    *   The `Term` created, if successful.
    */
  private def parseTerm(s: String): Option[Term] = {
    def makeTerm(name: String, args: List[Literal | Option[Term]]): Option[Term] = {
      val parsedArgs: List[Literal | Term] = args.map {
        case Some(e)        => e
        case other: Literal => other
        case _              => throw new NodeStringParseException(s"$name(${args.mkString(", ")})")
      }
      getExprBuilder(name) match {
        case Some(builder) => builder(parsedArgs)
        case None =>
          getTypeBuilder(name) match {
            case Some(builder) => builder(parsedArgs)
            case None          => None
          }
      }
    }

    object TermParser extends JavaTokenParsers with LiteralParser {
      def term: Parser[Option[Term]] = name ~ "(" ~ repsep(arg, "\\s*,\\s*".r) ~ ")" ^^ {
        case name ~ "(" ~ args ~ ")" => makeTerm(name, args)
        case _                       => None
      }

      def name: Parser[String] = "[A-Za-z]\\w*".r

      def identifier: Parser[String] = "[A-Za-z_$][\\w_$]*".r

      def arg: Parser[Literal | Option[Term]] = term | literal

      def parseTerm(s: String): ParseResult[Option[Term]] = parseAll(term, s.strip())
    }

    TermParser.parseTerm(s) match {
      case TermParser.Success(matched, _) => matched
      case x                              => None
    }
  }

  /** Function to load an `Expr` from a string. Input must be in the format produced by `Expr.toString`
    *
    * @param s
    *   The string to be parsed.
    * @return
    *   The `Expr` represented by the string, if successful.
    */
  def readExpr(s: String): Option[Expr] = parseTerm(s) match {
    case Some(e: Expr) => Some(e)
    case _             => None
  }

  def readType(s: String): Option[Type] = parseTerm(s) match {
    case Some(t: Type) => Some(t)
    case _             => None
  }

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
      case None        => throw new NodeParentNotInitialisedException()
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
    def read(s: String): Option[Node] = {
      def makeNode(name: String, args: List[Any]): Option[Node] = instantiate(name, args)

      object NodeParser extends JavaTokenParsers with LiteralParser {
        def outerNode: Parser[Option[OuterNode | Expr | Type]] =
          outerNodeName ~ "(" ~ repsep(outerNodeArg, "\\s*,\\s*".r) ~ ")" ^^ {
            case name ~ "(" ~ args ~ ")" =>
              makeNode(name, args) match {
                case Some(n: OuterNode) =>
                  n.children.foreach(_.setParent(Some(n)))
                  Some(n)
                case _ => throw new NodeStringParseException(s"$name(${args.mkString(", ")})")
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
        else throw new InvalidTreePathStringException(s)
    }

    /** Create a node of a given type with the given arguments.
      * @param nodeName
      *   The name of the node type.
      * @param args
      *   The arguments for the node.
      * @return
      *   The created node, if successful.
      */
    def instantiate(nodeName: String, args: List[Any]): Option[Node] = {
      val parsedArgs = args.map({
        case l: Literal => l
        case Some(e)    => e
        case other      => other
      })
      nodeName match {
        case "VariableNode" =>
          parsedArgs match {
            case List(exprName: String, innerNodes: List[InnerNode]) => Some(VariableNode(exprName, innerNodes))
            case _                                                   => None
          }
        case "ExprChoiceNode" =>
          parsedArgs match {
            case Nil => Some(ExprChoiceNode())
            case _   => None
          }
        case "TypeChoiceNode" =>
          parsedArgs match {
            case Nil => Some(TypeChoiceNode())
            case _   => None
          }
        case "TypeNode" =>
          parsedArgs match {
            case List(typeName: String, innerNodes: List[InnerNode]) => Some(TypeNode(typeName, innerNodes))
            case _                                                   => None
          }
        case "SubExprNode" =>
          parsedArgs match {
            case List(node: ExprNode) => Some(SubExprNode(node))
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

  /** Parent class for visible nodes in the expression tree.
    *
    * Has a list of inner nodes as arguments. Can be converted to HTML.
    */
  abstract class OuterNode extends Node {

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
        if (!args.indices.contains(head)) throw new InvalidTreePathException(path)
        else {
          args(head) match {
            case SubExprNode(node) => node.findChild(tail)
            case SubTypeNode(node) => node.findChild(tail)
            case n: LiteralNode =>
              tail match {
                case Nil => Some(n)
                case _   => throw new InvalidTreePathException(path)
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
                case n: ExprNode => n
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
                case _ => throw new InvalidTreePathException(path)
              }
          }
        )

        this match {
          case VariableNode(exprName, _) => VariableNode(exprName, updatedArgs)
          case TypeNode(typeName, _)     => TypeNode(typeName, updatedArgs)
        }
    }

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

    /** Whether this node is a phantom node.
      *
      * Phantom nodes are not part of the tree structure, but can appear during rendering.
      * @return
      *   whether this node is a phantom node
      */
    def isPhantom: Boolean = false
  }

  /** Parent class for nodes that represent an expression.
    *
    * Has a depth limit to prevent infinite recursion.
    */
  abstract class ExprNode extends OuterNode {
    override def setParent(parentNode: Option[OuterNode]): Unit = parentNode match {
      case Some(n: ExprNode) =>
        if (n.depth >= depthLimit) throw new DepthLimitExceededException()
        super.setParent(Some(n))
      case None    => super.setParent(None)
      case Some(n) => throw new NodeParentWrongTypeException("ExprNode", n.name)
    }

    override def getParent: Option[ExprNode] = {
      if (!isParentInitialised) markRoot()
      super.getParent match {
        case Some(n: ExprNode) => Some(n)
        case None              => None
        case Some(n)           => throw new NodeParentWrongTypeException("ExprNode", n.name)
      }
    }

    /** The depth of this node in the tree.
      *
      * Only starts
      * @return
      */
    def depth: Int = getPhantomDepth

    /** Check if the depth limit will be exceeded by evaluating this node, throwing an exception if it will.
      *
      * Similar to [[willDepthLimitBeExceeded]], but throws an exception if the depth limit will be exceeded.
      *
      * @param currDepth
      *   The current depth, default 0.
      * @throws DepthLimitExceededException
      *   if the depth limit will be exceeded.
      */
    def checkDepthLimitWillBeExceeded(currDepth: Int = 0): Unit = {
      if (currDepth + 1 >= depthLimit) throw new DepthLimitExceededException()

      getVisibleChildren(DisplayMode.Evaluation).reverse.foreach({
        case n: ExprNode => n.checkDepthLimitWillBeExceeded(currDepth + 1)
        case _           =>
      })
    }

    /** Whether the depth limit will be exceeded by evaluating this node.
      * @param currDepth
      *   The current depth, default 0.
      * @return
      *   Whether the depth limit will be exceeded.
      */
    def willDepthLimitBeExceeded(currDepth: Int = 0): Boolean = {
      (currDepth + 1 >= depthLimit) || getVisibleChildren(DisplayMode.Evaluation).reverse.exists({
        case n: ExprNode => n.willDepthLimitBeExceeded(currDepth + 1)
        case _           => false
      })
    }

    /** The name of the expression represented by this node.
      */
    val exprName: String

    /** The expression represented by this node.
      * @return
      *   Represented expression.
      */
    def getExpr: Expr

    lazy val getEditValueResult: Value = {
      if (willDepthLimitBeExceeded()) StackOverflowErrorValue()
      else getExpr.eval(getEditEnv)
    }

    /** Evaluation result of the expression represented by this node.
      */
    lazy val getValue: Value = getExpr.eval(getEvalEnv)

    /** Type-checking result of the expression represented by this node.
      */
    lazy val getType: Type = getExpr.typeCheck(getTypeEnv)

    private def getCorrectEnv[T](
      childrenFunction: Expr => Env[T] => List[(Term, Env[T])],
      parentEnvFunction: ExprNode => Env[T]
    ): Env[T] = getParent match {
      case Some(value) =>
        val parentEnv = parentEnvFunction(value)
        val parentExpr = value.getExpr
        val parentChildren = childrenFunction(parentExpr)(parentEnv)
        parentChildren.find(_._1 eq getExpr).map(_._2).getOrElse(parentEnv)
      case None => Env()
    }

    lazy val getEditEnv: ValueEnv = getCorrectEnv(_.getChildrenBase, _.getEditEnv)

    lazy val getEvalEnv: ValueEnv = getCorrectEnv(_.getChildrenEval, _.getEvalEnv)

    lazy val getTypeEnv: TypeEnv = getCorrectEnv(_.getChildrenTypeCheck, _.getTypeEnv)

    /** Get the environment for the given mode.
      * @param mode
      *   The display mode.
      * @return
      *   The environment for the given mode, either a [[ValueEnv]] or a [[TypeEnv]].
      */
    def getEnv(mode: DisplayMode): ValueEnv | TypeEnv = mode match {
      case DisplayMode.Edit       => getEditEnv
      case DisplayMode.TypeCheck  => getTypeEnv
      case DisplayMode.Evaluation => getEvalEnv
    }

    def hasUpdatedEnv(mode: DisplayMode): Boolean = {
      val env = getEnv(mode)
      val parentEnv = getParent.map(_.getEnv(mode))
      !parentEnv.contains(env)
    }

    private val visibleChildrenCache = collection.mutable.Map[DisplayMode, List[OuterNode]]()

    override def getVisibleChildren(mode: DisplayMode): List[OuterNode] = cacheQuery(
      visibleChildrenCache,
      mode,
      mode match {
        case DisplayMode.Edit       => children
        case DisplayMode.TypeCheck  => children
        case DisplayMode.Evaluation => visibleEvaluationChildren
      }
    )

    private def visibleEvaluationChildren: List[OuterNode] = {
      val childExprList = getExpr.getChildrenEval(getEvalEnv).map(_._1)
      var unconsumedChildren = children

      childExprList.flatMap({ case expr: Expr =>
        val matchingChild = unconsumedChildren.collectFirst {
          case c: ExprNode if c.getExpr eq expr                       => c
          case c: ExprChoiceNode if c.getExpr == expr && !c.isPhantom => c
        }

        matchingChild match {
          case Some(childNode) =>
            unconsumedChildren = unconsumedChildren.filter(_ ne childNode)
            Some(childNode)
          case None =>
            val newNode = VariableNode.fromExpr(expr)
            newNode.setParent(Some(this))
            newNode.markPhantom(getPhantomDepth + 1)
            Some(newNode)
        }
      })
    }

    private var isPhantomStore = false

    private def markPhantom(depth: Int): Unit = {
      isPhantomStore = true
      phantomDepth = Some(depth)

      if (depth >= depthLimit) throw new DepthLimitExceededException()
    }

    private var phantomDepth: Option[Int] = None

    private def getPhantomDepth: Int = phantomDepth.getOrElse(0)

    override def isPhantom: Boolean = isPhantomStore
  }

  /** Simple expression node implementation using an expression name and a list of arguments.
    *
    * Currently the only expression node implementation.
    */
  case class VariableNode(exprName: String, args: List[InnerNode] = Nil) extends ExprNode {
    override val name: String = "VariableNode"

    override val children: List[OuterNode] = args.flatMap(_.children)

    override def getExpr: Expr = exprOverride.getOrElse(expr)

    private var exprOverride: Option[Expr] = None

    /** Set this node's expression to the given expression, ignoring the class parameters.
      *
      * This means that [[getExpr]] will return the given expression, rather than what it would normally return.
      *
      * @param e
      *   The expression to set this node's expression to.
      */
    def overrideExpr(e: Expr): Unit = {
      exprOverride = Some(e)
    }

    lazy val expr: Expr = {
      val arguments = args.map {
        case n: SubExprNode => n.node.getExpr
        case n: LiteralNode => n.getLiteral
        case n: SubTypeNode => n.node.getType
      }
      buildExpr(exprName, arguments) match
        case Some(value) => value
        case None => throw new ClickDeduceException(s"Could not build an instance of $exprName with arguments $arguments")
    }

    private val htmlLineCache = collection.mutable.Map[DisplayMode, TypedTag[String]]()
    private val htmlLineReadOnlyCache = collection.mutable.Map[DisplayMode, TypedTag[String]]()

    override def toText(mode: DisplayMode): ConvertableText =
      HtmlElement(getExprHtmlLine(mode).asHtml, getExpr.toText)

    override def toTextReadOnly(mode: DisplayMode): ConvertableText =
      HtmlElement(getExprHtmlLineReadOnly(mode).asHtmlReadOnly, getExpr.toText)

    private def getExprHtmlLine(mode: DisplayMode): ConvertableText = {
      val arguments: List[Term] = args.map {
        case n: SubExprNode => n.getPlaceholder(mode)
        case n: LiteralNode => n.getPlaceholder(mode, false)
        case n: SubTypeNode => n.getPlaceholder(mode)
      }
      buildExpr(exprName, arguments).get.toText
    }

    private def getExprHtmlLineReadOnly(mode: DisplayMode): ConvertableText = {
      val arguments = args.map(_.getPlaceholder(mode))
      buildExpr(exprName, arguments).get.toText
    }

    override def toString: String = s"VariableNode(${UtilityFunctions.quote(exprName)}, $args)"

    children.foreach(_.setParent(Some(this)))
    args.foreach(_.setParent(Some(this)))
  }

  /** Companion object for [[VariableNode]].
    */
  object VariableNode {

    /** Create a new [[VariableNode]] from an expression name.
      * @param exprName
      *   The expression name.
      * @return
      *   The new [[VariableNode]].
      * @throws ClickDeduceException
      *   If the expression name is not recognised in this language, or if there is no default expression for the given
      *   name.
      */
    def createFromExprName(exprName: String): Option[VariableNode] = {
      val innerNodes = buildExpr(exprName, Nil) match {
        case Some(e: Product) =>
          e.productIterator.toList.collect({
            case c: Expr    => SubExprNode(ExprChoiceNode())
            case c: Literal => LiteralNode(c)
            case c: Type    => SubTypeNode(TypeChoiceNode())
            case c          => throw new ClickDeduceException(s"Unexpected parameter type in createFromExpr: $c")
          })
        case _ => throw new ClickDeduceException(s"No default expression for $exprName")
      }
      val result = VariableNode(exprName, innerNodes)
      innerNodes.foreach(_.setParent(Some(result)))
      Some(result)
    }

    /** Create a new [[VariableNode]] from an expression.
      * @param e
      *   The expression.
      * @return
      *   The new [[VariableNode]], matching the structure of the given expression.
      */
    def fromExpr(e: Expr): ExprNode = e match {
      case blank: BlankExprDropDown => ExprChoiceNode()
      case e =>
        val innerNodes = e match {
          case e0: Product =>
            val values = e0.productIterator.toList
            values.collect({
              case c: Expr    => SubExprNode(VariableNode.fromExpr(c))
              case c: Literal => LiteralNode(c)
              case c: Type    => SubTypeNode(TypeNode.fromType(c))
            })
        }
        val result = VariableNode(e.name, innerNodes)
        result.overrideExpr(e)
        innerNodes.foreach(_.setParent(Some(result)))
        result
    }
  }

  /** Node representing an unselected expression.
    *
    * Displayed in the interface as a selector, where the user can choose an expression name.
    */
  case class ExprChoiceNode() extends ExprNode {
    override val name: String = "ExprChoiceNode"

    override val args: List[InnerNode] = Nil

    override val children: List[OuterNode] = Nil

    override def toText(mode: DisplayMode): ConvertableText =
      HtmlElement(BlankExprDropDown().toText.asHtml(data("tree-path") := treePathString), BlankExprDropDown().toText)

    override def toTextReadOnly(mode: DisplayMode): ConvertableText = toText(mode).toReadOnly

    override val exprName: String = "ExprChoice"

    private val expr = BlankExprDropDown()

    override def getExpr: Expr = expr
  }

  /** Parent class for nodes that do not appear on their own in the tree structure.
    */
  abstract class InnerNode extends Node {

    /** Create a term placeholder for this node.
      * @param mode
      *   The display mode.
      * @param readOnly
      *   Whether the placeholder should be read-only.
      * @return
      *   The term placeholder.
      */
    def getPlaceholder(mode: DisplayMode, readOnly: Boolean = true): Term
  }

  /** An inner node that contains a sub-expression.
    * @param node
    *   The sub-expression node.
    */
  case class SubExprNode(node: ExprNode) extends InnerNode {
    override val name: String = "SubExprNode"

    override def setParent(parentNode: Option[OuterNode]): Unit = parentNode match {
      case Some(n: ExprNode) => super.setParent(Some(n))
      case None              => throw new InnerNodeCannotBeRootException()
      case Some(n)           => throw new NodeParentWrongTypeException("ExprName", n.name)
    }

    override def getParent: Option[ExprNode] = super.getParent match {
      case Some(n: ExprNode) => Some(n)
      case None              => None
      case Some(n)           => throw new NodeParentWrongTypeException("ExprNode", n.name)
    }

    override def toText(mode: DisplayMode): ConvertableText = node.toText(mode)

    override def toTextReadOnly(mode: DisplayMode): ConvertableText = node.toTextReadOnly(mode)

    override def getPlaceholder(mode: DisplayMode, readOnly: Boolean = true): ExprPlaceholder =
      ExprPlaceholder(node.toTextReadOnly(mode), node.getExpr.needsBrackets)

    override val children: List[ExprNode] = List(node)
  }

  /** An inner node that represents a literal field.
    * @param literalText
    *   The current text of the literal.
    */
  case class LiteralNode(literal: Literal) extends InnerNode {
    override val name: String = "LiteralNode"

    def toHtmlLine(mode: DisplayMode): TypedTag[String] = literal.toHtmlInput(treePathString, getEnv(mode))

    def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] = literal.toHtmlInputReadOnly(treePathString)

    override def toText(mode: DisplayMode): ConvertableText = HtmlElement(toHtmlLine(mode), getLiteral.toText)

    override def toTextReadOnly(mode: DisplayMode): ConvertableText =
      HtmlElement(toHtmlLineReadOnly(mode), getLiteral.toText)

    override def getPlaceholder(mode: DisplayMode, readOnly: Boolean = true): Literal =
      placeholderOfLiteral(literal, (if (readOnly) toHtmlLineReadOnly(mode) else toHtmlLine(mode)).toString)

    override val children: List[OuterNode] = Nil

    lazy val literalText: String = literal.getValue

    lazy val getLiteral: Literal = literal

    def getEnv(mode: DisplayMode): ValueEnv | TypeEnv = getParent match
      case Some(exprNode: ExprNode)       => exprNode.getEnv(mode)
      case Some(typeNode: TypeNodeParent) => typeNode.getEnv(mode)
      case None                           => Env()
  }

  /** Parent class for nodes that represent a type.
    *
    * Analogous to [[ExprNode]], but for types.
    */
  abstract class TypeNodeParent extends OuterNode {

    /** The type represented by this node.
      */
    lazy val getType: Type

    /** The name of the type represented by this node.
      */
    lazy val getTypeName: String = getType.name

    override def getParent: Option[OuterNode] = {
      if (!isParentInitialised) markRoot()
      super.getParent match {
        case Some(n) => Some(n)
        case None    => None
      }
    }

    /** Get the type variable environment for the given mode.
      * @param mode
      *   The display mode.
      * @return
      *   The type variable environment.
      */
    def getEnv(mode: DisplayMode): TypeEnv = getParent match {
      case Some(n: ExprNode)       => typeVariableEnv(n.getEnv(mode))
      case Some(n: TypeNodeParent) => n.getEnv(mode)
      case _                       => Env()
    }

    /** The result of type-checking this node in the given mode.
      * @param mode
      *   The display mode.
      * @return
      *   The type-checking result.
      */
    def getTypeCheckResult(mode: DisplayMode): Type = getType.typeCheck(getEnv(mode))
  }

  /** Implementation of a type node.
    *
    * Analogous to [[VariableNode]], but for types.
    * @param typeName
    *   The name of the type.
    * @param args
    *   The arguments of the type.
    */
  case class TypeNode(typeName: String, args: List[InnerNode]) extends TypeNodeParent {
    override val name: String = "TypeNode"

    override lazy val getType: Type = {
      val arguments = args.map {
        case tn: SubTypeNode => tn.node.getType
        case ln: LiteralNode => ln.getLiteral
      }
      buildType(typeName, arguments).get
    }

    override def toText(mode: DisplayMode): ConvertableText = {
      val arguments = args.map {
        case n: LiteralNode => n.getPlaceholder(mode, false)
        case other          => other.getPlaceholder(mode)
      }
      buildType(typeName, arguments).get.toText
    }

    override def toTextReadOnly(mode: DisplayMode): ConvertableText = {
      val arguments = args.map {
        case n: LiteralNode => LiteralAny(n.toHtmlLineReadOnly(mode).toString)
        case n: SubTypeNode => TypePlaceholder(n.node.toTextReadOnly(mode), n.node.getType.needsBrackets)
      }
      buildType(typeName, arguments).get.toText
    }

    override val children: List[OuterNode] = args.filter(_.isInstanceOf[SubTypeNode]).flatMap(_.children)

    override def toString: String = s"TypeNode(${UtilityFunctions.quote(typeName)}, $args)"

    children.foreach(_.setParent(Some(this)))
    args.foreach(_.setParent(Some(this)))
  }

  /** Companion object for [[TypeNode]].
    */
  object TypeNode {

    /** Create a new [[TypeNode]] from a type name.
      * @param typeName
      *   The type name.
      * @return
      *   The new [[TypeNode]].
      * @throws ClickDeduceException
      *   If the type name is not recognised in this language, or if there is no default type for the given name.
      */
    def fromTypeName(typeName: String): Option[TypeNode] = getTypeBuilder(typeName) match {
      case Some(builder) =>
        val arguments = builder(Nil) match {
          case Some(e: Product) =>
            e.productIterator.toList.collect({
              case c: Literal => LiteralNode(c)
              case c: Type    => SubTypeNode(TypeNode.fromType(c))
              case c          => throw new ClickDeduceException(s"Unexpected parameter type in createFromTypeName: $c")
            })
          case _ => throw new ClickDeduceException(s"No default type for $typeName")
        }
        Some(TypeNode(typeName, arguments))
      case None => None
    }

    /** Create a new [[TypeNode]] from a type.
      * @param typ
      *   The type.
      * @return
      *   The new [[TypeNode]], matching the structure of the given type.
      */
    def fromType(typ: Type): TypeNodeParent = typ match {
      case blank: BlankTypeDropDown => TypeChoiceNode()
      case _ =>
        val innerNodes = typ match {
          case e0: Product =>
            e0.productIterator.toList.collect({
              case c: Literal => LiteralNode(c)
              case c: Type    => SubTypeNode(TypeNode.fromType(c))
            })
        }
        val result = TypeNode(typ.name, innerNodes)
        innerNodes.foreach(_.setParent(Some(result)))
        result
    }
  }

  /** Node representing an unselected type.
    *
    * Displayed in the interface as a selector, where the user can choose a type name.
    */
  case class TypeChoiceNode() extends TypeNodeParent {
    override val name: String = "TypeChoiceNode"

    override val args: List[InnerNode] = Nil

    override def toText(mode: DisplayMode): ConvertableText =
      HtmlElement(BlankTypeDropDown().toText.asHtml(data("tree-path") := treePathString), BlankTypeDropDown().toText)

    override def toTextReadOnly(mode: DisplayMode): ConvertableText = toText(mode).toReadOnly

    override lazy val getType: Type = UnknownType()
  }

  /** An inner node that contains a sub-type.
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

  private val depthLimit: Int = 100

  class DepthLimitExceededException extends ClickDeduceException(s"Depth limit ($depthLimit) exceeded")

  class InvalidTreePathStringException(s: String) extends ClickDeduceException(s"Invalid tree path string: $s")

  class InvalidTreePathException(treePath: List[Int]) extends ClickDeduceException(s"Invalid tree path: $treePath")

  class NodeStringParseException(nodeString: String)
      extends ClickDeduceException(s"Could not parse node string: $nodeString")

  class NodeParentNotInitialisedException extends ClickDeduceException("Node parent not initialised")

  class NodeParentWrongTypeException(expected: String, actual: String)
      extends ClickDeduceException(s"Node parent has wrong type: expected $expected, got $actual")

  class InnerNodeCannotBeRootException extends ClickDeduceException("Inner node cannot be root")

  // utility

  private def cacheQuery[A, B](cache: collection.mutable.Map[A, B], key: A, value: => B): B = cache.get(key) match {
    case Some(value) => value
    case None =>
      val result = value
      cache += (key -> result)
      result
  }
}
