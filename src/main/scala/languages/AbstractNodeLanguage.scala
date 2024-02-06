package languages

import app.{ClickDeduceException, UtilityFunctions}
import convertors.{ClassDict, DisplayMode}
import scalatags.Text.TypedTag
import scalatags.Text.all.*

import java.lang.reflect
import scala.util.parsing.combinator.*

trait AbstractNodeLanguage extends AbstractLanguage {
  lang =>

  trait BlankSpace extends Term {
    override lazy val toHtml: TypedTag[String] = {
      input(`type` := "text", placeholder := "Term")
    }
  }

  case class BlankExprDropDown() extends Expr, BlankSpace {
    override lazy val toHtml: TypedTag[String] = exprClassListDropdownHtml

    override val needsBrackets: Boolean = false
  }

  val defaultExpr: Expr = BlankExprDropDown()

  case class BlankTypeDropDown() extends Type, BlankSpace {
    override lazy val toHtml: TypedTag[String] = typeClassListDropdownHtml

    override val needsBrackets: Boolean = false
  }

  val defaultType: Type = BlankTypeDropDown()

  private lazy val exprClassListDropdownHtml: TypedTag[String] = select(
    cls := ClassDict.EXPR_DROPDOWN,
    option(value := "", "Select Expr..."),
    exprClassList.map(e => option(value := e.getSimpleName, e.getSimpleName))
  )

  private lazy val typeClassListDropdownHtml: TypedTag[String] = select(
    cls := ClassDict.TYPE_DROPDOWN,
    option(value := "", "Select Type..."),
    typeClassList.map(e => option(value := e.getSimpleName, e.getSimpleName))
  )

  /** Create an `Term` given its string representation.
    *
    * @return
    *   The `Term` created, if successful.
    */
  private def parseTerm(s: String): Option[Term] = {
    def makeTerm(name: String, args: List[Any]): Option[Term] = {
      val parsedArgs = args.map {
        case Some(e) => e
        case other   => other
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

    object TermParser extends JavaTokenParsers {
      def term: Parser[Option[Term]] = name ~ "(" ~ repsep(arg, "\\s*,\\s*".r) ~ ")" ^^ {
        case name ~ "(" ~ args ~ ")" => makeTerm(name, args)
        case _                       => None
      }

      def name: Parser[String] = "[A-Za-z]\\w*".r

      def identifier: Parser[String] = "[A-Za-z_$][\\w_$]*".r

      def arg: Parser[Any] = term | stringLiteral ^^ (s => LiteralString(s)) |
        wholeNumber ^^ (n => LiteralInt(BigInt(n))) |
        "true" ^^ (_ => LiteralBool(true)) | "false" ^^ (_ => LiteralBool(false)) |
        identifier ^^ (s => LiteralIdentifier(s))

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
    val name: String

    val children: List[OuterNode] = Nil

    private var parent: Option[Option[OuterNode]] = None

    private var parentInitialised = false

    def getParent: Option[OuterNode] = parent match {
      case Some(value) => value
      case None        => throw new NodeParentNotInitialisedException()
    }

    def setParent(parentNode: Option[OuterNode]): Unit = {
      parent = Some(parentNode)
      markParentInitialised()
    }

    private def markParentInitialised(): Unit = {
      parentInitialised = true
    }

    def isParentInitialised: Boolean = parentInitialised

    def toHtmlLine(mode: DisplayMode): TypedTag[String]

    def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String]

    lazy val treePath: List[Int] = getParent match {
      case Some(value) => value.treePath :+ value.args.indexWhere(_ eq this)
      case None        => Nil
    }

    lazy val treePathString: String = treePath.mkString("-")
  }

  object Node {

    /** Creates a node from a correct string representation of a node (from Node.toString).
      */
    def read(s: String): Option[Node] = {
      def makeNode(name: String, args: List[Any]): Option[Node] = instantiate(name, args)

      object NodeParser extends JavaTokenParsers {
        def outerNode: Parser[Option[OuterNode | Expr | Type]] =
          outerNodeName ~ "(" ~ repsep(outerNodeArg, "\\s*,\\s*".r) ~ ")" ^^ {
            case name ~ "(" ~ args ~ ")" =>
              val node = makeNode(name, args)
              node match {
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
          outerListParse | innerNode | stringLiteral ^^ (s => Literal.fromString(UtilityFunctions.unquote(s)))

        def innerNodeArg: Parser[Any] = outerNode | stringLiteral ^^ (s => {
          val temp = s
          Literal.fromString(UtilityFunctions.unquote(s))
        })

        def innerNode: Parser[InnerNode] = innerNodeName ~ "(" ~ repsep(innerNodeArg, "\\s*,\\s*".r) ~ ")" ^^ {
          case name ~ "(" ~ args ~ ")" =>
            val node = makeNode(name, args)
            node match {
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

    def readPathString(s: String): List[Int] = s match {
      case "" => Nil
      case s =>
        if (s.split("-").forall(_.matches("\\d+"))) s.split("-").map(_.toInt).toList
        else throw new InvalidTreePathStringException(s)
    }

    def instantiate(nodeName: String, args: List[Any]): Option[Node] = {
      val parsedArgs = args.map({
        case l: Literal => l.toString
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
            case List(s: String) => Some(LiteralNode(s))
            case _               => None
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
    val args: List[InnerNode]

    def markRoot(): Unit = {
      setParent(None)
    }

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

    def indexOf(node: Node): Int = node match {
      case n: InnerNode => args.indexWhere(_ eq n)
      case n: OuterNode => args.indexWhere(_.children.exists(_ eq n))
    }

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

    def isPhantom: Boolean = false
  }

  abstract class ExprNode extends OuterNode {
    override def setParent(parentNode: Option[OuterNode]): Unit = parentNode match {
      case Some(n: ExprNode) =>
        val parentDepth = n.depth
        if (parentDepth >= depthLimit) throw new DepthLimitExceededException()
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

    def depth: Int = getParent match {
      case Some(value) => value.depth + 1
      case None        => 0
    }

    def checkDepthLimitWillBeExceeded(currDepth: Int = 0): Unit = {
      if (currDepth + 1 >= depthLimit) throw new DepthLimitExceededException()

      getVisibleChildren(DisplayMode.Evaluation).foreach({
        case n: ExprNode => n.checkDepthLimitWillBeExceeded(currDepth + 1)
        case _           =>
      })
    }

    val exprName: String

    def getExpr: Expr

    lazy val getEditValueResult: Value = getExpr.eval(getEditEnv)

    lazy val getValue: Value = getExpr.eval(getEvalEnv)

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

    def getEnv(mode: DisplayMode): ValueEnv | TypeEnv = mode match {
      case DisplayMode.Edit       => getEditEnv
      case DisplayMode.TypeCheck  => getTypeEnv
      case DisplayMode.Evaluation => getEvalEnv
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
            newNode.markPhantom()
            Some(newNode)
        }
      })
    }

    private var isPhantomStore = false

    private def markPhantom(): Unit = {
      isPhantomStore = true
    }

    override def isPhantom: Boolean = isPhantomStore
  }

  /** Concrete implementation of an expression node.
    */
  case class VariableNode(exprName: String, args: List[InnerNode] = Nil) extends ExprNode {
    override val name: String = "VariableNode"

    override val children: List[OuterNode] = args.flatMap(_.children)

    override def getExpr: Expr = exprOverride.getOrElse(expr)

    private var exprOverride: Option[Expr] = None

    def overrideExpr(e: Expr): Unit = {
      exprOverride = Some(e)
    }

    private lazy val exprClass: Class[Expr] = exprNameToClass(exprName) match {
      case Some(value) => value
      case None        => throw new IllegalArgumentException(s"Unknown expression type: $exprName")
    }

    lazy val expr: Expr = {
      val arguments = args.map {
        case n: SubExprNode => n.node.getExpr
        case n: LiteralNode => n.getLiteral
        case n: SubTypeNode => n.node.getType
      }
      buildExpr(exprName, arguments).get
    }

    private val htmlLineCache = collection.mutable.Map[DisplayMode, TypedTag[String]]()
    private val htmlLineReadOnlyCache = collection.mutable.Map[DisplayMode, TypedTag[String]]()

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      cacheQuery(htmlLineCache, mode, div(raw(getExprHtmlLine(mode))))

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] =
      cacheQuery(htmlLineReadOnlyCache, mode, div(raw(getExprHtmlLineReadOnly(mode))))

    private def getExprHtmlLine(mode: DisplayMode): String = {
      val arguments = args.map {
        case n: SubExprNode => n.getPlaceholder(mode)
        case n: LiteralNode => n.getPlaceholder(mode, false)
        case n: SubTypeNode => n.getPlaceholder(mode)
      }
      buildExpr(exprName, arguments).get.prettyPrint
    }

    private def getExprHtmlLineReadOnly(mode: DisplayMode): String = {
      val arguments = args.map(_.getPlaceholder(mode))
      buildExpr(exprName, arguments).get.prettyPrint
    }

    override def toString: String = s"VariableNode(${UtilityFunctions.quote(exprName)}, $args)"

    children.foreach(_.setParent(Some(this)))
    args.foreach(_.setParent(Some(this)))
  }

  object VariableNode {
    def createFromExprName(exprName: String): Option[VariableNode] = {
      val innerNodes = buildExpr(exprName, Nil) match {
        case Some(e: Product) =>
          e.productIterator.toList.collect({
            case c: Expr    => SubExprNode(ExprChoiceNode())
            case c: Literal => LiteralNode("")
            case c: Type    => SubTypeNode(TypeChoiceNode())
            case c          => throw new ClickDeduceException(s"Unexpected parameter type in createFromExpr: $c")
          })
        case _ => throw new ClickDeduceException(s"No default expression for $exprName")
      }
      val result = VariableNode(exprName, innerNodes)
      innerNodes.foreach(_.setParent(Some(result)))
      Some(result)
    }

    def fromExpr(e: Expr): ExprNode = e match {
      case blank: BlankExprDropDown => ExprChoiceNode()
      case e =>
        val innerNodes = e match {
          case e0: Product =>
            val values = e0.productIterator.toList
            values.collect({
              case c: Expr    => SubExprNode(VariableNode.fromExpr(c))
              case c: Literal => LiteralNode(c.toString)
              case c: Type    => SubTypeNode(TypeNode.fromType(c))
            })
        }
        val result = VariableNode(e.name, innerNodes)
        result.overrideExpr(e)
        innerNodes.foreach(_.setParent(Some(result)))
        result
    }
  }

  case class ExprChoiceNode() extends ExprNode {
    override val name: String = "ExprChoiceNode"

    override val args: List[InnerNode] = Nil

    override val children: List[OuterNode] = Nil

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      BlankExprDropDown().toHtml(data("tree-path") := treePathString)

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] = toHtmlLine(mode)(readonly, disabled)

    override val exprName: String = "ExprChoice"

    override def getExpr: Expr = BlankExprDropDown()
  }

  abstract class InnerNode extends Node {
    def getPlaceholder(mode: DisplayMode, readOnly: Boolean = true): Term
  }

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

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] = node.toHtmlLineReadOnly(mode)

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] = toHtmlLine(mode)

    override def getPlaceholder(mode: DisplayMode, readOnly: Boolean = true): ExprPlaceholder =
      ExprPlaceholder(node.toHtmlLineReadOnly(mode).toString, node.getExpr.needsBrackets)

    override val children: List[ExprNode] = List(node)
  }

  case class LiteralNode(literalText: String) extends InnerNode {
    override val name: String = "LiteralNode"

    private val htmlLineShared: TypedTag[String] =
      input(`type` := "text", cls := ClassDict.LITERAL, value := literalText)

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      htmlLineShared(width := s"${Math.max(2, literalText.length)}ch", data("tree-path") := treePathString)

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] =
      htmlLineShared(width := s"${Math.max(1, literalText.length)}ch", readonly, disabled)

    override def getPlaceholder(mode: DisplayMode, readOnly: Boolean = true): LiteralAny = if (readOnly) {
      LiteralAny(toHtmlLineReadOnly(mode).toString)
    } else {
      LiteralAny(toHtmlLine(mode).toString)
    }

    override val children: List[OuterNode] = Nil

    override def toString: String = s"LiteralNode(${UtilityFunctions.quote(literalText)})"

    lazy val getLiteral: Literal = Literal.fromString(literalText)
  }

  abstract class TypeNodeParent extends OuterNode {
    lazy val getType: Type

    lazy val getTypeName: String = getType.name

    override def getParent: Option[OuterNode] = {
      if (!isParentInitialised) markRoot()
      super.getParent match {
        case Some(n) => Some(n)
        case None    => None
      }
    }

    def getEnv(mode: DisplayMode): TypeEnv = getParent match {
      case Some(n: ExprNode)       => typeVariableEnv(n.getEnv(mode))
      case Some(n: TypeNodeParent) => n.getEnv(mode)
      case _                       => Env()
    }

    def getTypeCheckResult(mode: DisplayMode): Type = getType.typeCheck(getEnv(mode))
  }

  case class TypeNode(typeName: String, args: List[InnerNode]) extends TypeNodeParent {
    override val name: String = "TypeNode"

    override lazy val getType: Type = {
      val arguments = args.map {
        case tn: SubTypeNode => tn.node.getType
        case ln: LiteralNode => ln.getLiteral
      }
      buildType(typeName, arguments).get
    }

    private lazy val typeClass: Class[Type] = typeNameToClass(typeName) match {
      case Some(value) => value
      case None        => throw new IllegalArgumentException(s"Unknown expression type: $typeName")
    }

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      div(raw(getExprHtmlLine(mode)))(data("tree-path") := treePathString)

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] =
      div(raw(getExprHtmlLineReadOnly(mode)))(readonly, disabled)

    override val children: List[OuterNode] = args.filter(_.isInstanceOf[SubTypeNode]).flatMap(_.children)

    override def toString: String = s"TypeNode(${UtilityFunctions.quote(typeName)}, $args)"

    private def getExprHtmlLine(mode: DisplayMode): String = {
      val arguments = args.map {
        case n: LiteralNode => n.getPlaceholder(mode, false)
        case other          => other.getPlaceholder(mode)
      }
      buildType(typeName, arguments).get.prettyPrint
    }

    private def getExprHtmlLineReadOnly(mode: DisplayMode): String = {
      val arguments = args.map {
        case n: LiteralNode => LiteralAny(n.toHtmlLineReadOnly(mode).toString)
        case n: SubTypeNode => TypePlaceholder(n.node.toHtmlLineReadOnly(mode).toString, n.node.getType.needsBrackets)
      }
      buildType(typeName, arguments).get.prettyPrint
    }

    children.foreach(_.setParent(Some(this)))
    args.foreach(_.setParent(Some(this)))
  }

  object TypeNode {
    def fromTypeName(typeName: String): Option[TypeNode] = typeNameToClass(typeName) match {
      case Some(typ) =>
        val arguments = buildType(typeName, Nil) match {
          case Some(e: Product) =>
            e.productIterator.toList.collect({
              case c: Literal => LiteralNode(c.toString)
              case c: Type    => SubTypeNode(TypeNode.fromType(c))
              case c         => throw new ClickDeduceException(s"Unexpected parameter type in createFromTypeName: $c")
            })
          case _ => throw new ClickDeduceException(s"No default type for $typeName")
        }
        Some(TypeNode(typeName, arguments))
      case None => None
    }

    def fromType(typ: Type): TypeNodeParent = typ match {
      case blank: BlankTypeDropDown => TypeChoiceNode()
      case _ =>
        val innerNodes = typ match {
          case e0: Product =>
            e0.productIterator.toList.collect({
              case c: Literal => LiteralNode(c.toString)
              case c: Type    => SubTypeNode(TypeNode.fromType(c))
            })
        }
        val result = TypeNode(typ.name, innerNodes)
        innerNodes.foreach(_.setParent(Some(result)))
        result
    }
  }

  case class TypeChoiceNode() extends TypeNodeParent {
    override val name: String = "TypeChoiceNode"

    override val args: List[InnerNode] = Nil

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      BlankTypeDropDown().toHtml(data("tree-path") := treePathString)

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] = toHtmlLine(mode)(readonly, disabled)

    override lazy val getType: Type = UnknownType()
  }

  case class SubTypeNode(node: TypeNodeParent) extends InnerNode {
    override val name: String = "SubTypeNode"

    override val children: List[OuterNode] = List(node)

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] = node.toHtmlLineReadOnly(mode)

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] = toHtmlLine(mode)(readonly, disabled)

    override def getPlaceholder(mode: DisplayMode, readOnly: Boolean = true): TypePlaceholder =
      TypePlaceholder(node.toHtmlLineReadOnly(mode).toString, node.getType.needsBrackets)
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

  // class lists

  private lazy val exprClassList: List[Class[Expr]] = calculateExprClassList

  protected def calculateExprClassList: List[Class[Expr]]

  private lazy val typeClassList: List[Class[Type]] = calculateTypeClassList

  protected def calculateTypeClassList: List[Class[Type]] = List(classOf[UnknownType]).map(_.asInstanceOf[Class[Type]])

  private lazy val blankClassList: List[Class[BlankSpace]] =
    List(classOf[BlankExprDropDown], classOf[BlankTypeDropDown]).map(_.asInstanceOf[Class[BlankSpace]])

  private lazy val nodeClassList: List[Class[Node]] = List(
    classOf[VariableNode],
    classOf[ExprChoiceNode],
    classOf[TypeChoiceNode],
    classOf[TypeNode],
    classOf[SubExprNode],
    classOf[LiteralNode],
    classOf[SubTypeNode]
  ).map(_.asInstanceOf[Class[Node]])

  private def exprNameToClass(name: String): Option[Class[Expr]] = exprClassList.find(_.getSimpleName == name)

  private def typeNameToClass(name: String): Option[Class[Type]] = typeClassList.find(_.getSimpleName == name)

  // utility

  def findSubClassesOf[T](
    clazz: Class[_ <: AbstractNodeLanguage],
    superclass: Class[T],
    includeInterfaces: Boolean = true
  ): List[Class[T]] = {
    val subclasses = clazz.getDeclaredClasses.toList
      .filter(cls => superclass.isAssignableFrom(cls))
    val parentSubclasses = (List(clazz.getSuperclass) ++ (if (includeInterfaces) clazz.getInterfaces else Nil))
      .flatMap {
        case c: Class[_ <: AbstractNodeLanguage] => findSubClassesOf(c, superclass)
        case _                                   => Nil
      }
    val classes = parentSubclasses ++ subclasses.asInstanceOf[List[Class[T]]]
    classes.filterNot(cls => reflect.Modifier.isAbstract(cls.getModifiers)).filterNot(_.isInterface)
  }

  def findInterface[T](clazz: Class[_ <: AbstractNodeLanguage], interface: Class[T]): Class[T] = {
    clazz.getInterfaces.find(_ == interface) match {
      case Some(value) => value.asInstanceOf[Class[T]]
      case None =>
        clazz.getSuperclass match {
          case c: Class[_ <: AbstractNodeLanguage] => findInterface(c, interface)
          case _ => throw new Exception(s"Could not find interface $interface in class $clazz")
        }
    }
  }

  private def cacheQuery[A, B](cache: collection.mutable.Map[A, B], key: A, value: => B): B = cache.get(key) match {
    case Some(value) => value
    case None =>
      val result = value
      cache += (key -> result)
      result
  }
}
