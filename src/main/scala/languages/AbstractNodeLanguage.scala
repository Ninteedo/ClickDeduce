package languages

import app.{ClickDeduceException, UtilityFunctions}
import convertors.DisplayMode
import scalatags.Text.TypedTag
import scalatags.Text.all.*

import java.lang.reflect
import scala.annotation.tailrec
import scala.reflect.ClassTag
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

  case class BlankTypeDropDown() extends Type, BlankSpace {
    override lazy val toHtml: TypedTag[String] = typeClassListDropdownHtml

    override val needsBrackets: Boolean = false
  }

  private lazy val exprClassListDropdownHtml: TypedTag[String] = select(
    cls := "expr-dropdown",
    option(value := "", "Select Expr..."), exprClassList.map(e => option(value := e.getSimpleName, e.getSimpleName))
  )

  private lazy val typeClassListDropdownHtml: TypedTag[String] = select(
    cls := "type-dropdown",
    option(value := "", "Select Type..."),
    typeClassList.map(e => option(value := e.getSimpleName, e.getSimpleName))
  )

  /** Create an `Term` given its string representation.
    *
    * @return
    *   The `Term` created, if successful.
    */
  private def parseTerm(s: String): Option[Term] = {
    @tailrec
    def makeTerm(name: String, args: List[Any]): Option[Term] = {
      def constructTermFromArgs[T](termClass: Class[T]): T = {
        val constructor = termClass.getConstructors()(0)
        val arguments = this +: args.map {
          case Some(e) => e
          case x       => x
        }
        constructor.newInstance(arguments: _*).asInstanceOf[T]
      }

      val exprClass = exprClassList.find(_.getSimpleName == name)
      exprClass match {
        case Some(value) => Some(constructTermFromArgs(value))
        case None =>
          val blankClass = blankClassList.find(_.getSimpleName == name)
          blankClass match {
            case Some(value) => makeTerm("MissingExpr", Nil)
            case None =>
              typeClassList.find(_.getSimpleName == name) match {
                case Some(value) => Some(constructTermFromArgs(value))
                case None        => None
              }
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
    val innerNodeClasses: List[Class[_ <: Object]] =
      List(ExprChoiceNode.getClass, SubExprNode.getClass, LiteralNode.getClass)

    val outerNodeClasses: List[Class[_ <: Object]] = List(VariableNode.getClass)

    /** Creates a node from a correct string representation of a node (from Node.toString).
      */
    def read(s: String): Option[Node] = {
      def makeNode(name: String, args: List[Any], env: Env | TypeEnv = Map()): Option[Node] = {
        val nodeClass = nodeClassList.find(_.getSimpleName == name)
        nodeClass match {
          case Some(value) =>
            val constructor = value.getConstructors()(0)
            var arguments = AbstractNodeLanguage.this +: args.map {
              case l: Literal => l.toString
              case Some(e)    => e
              case x          => x
            }
            if (constructor.getParameterTypes.last.isAssignableFrom(classOf[Env])) {
              arguments = arguments :+ env
            }
            if (constructor.getParameterTypes.length != arguments.length) {
              throw new NodeStringParseException(s"$name(${args.mkString(", ")})")
            }
            Some(constructor.newInstance(arguments: _*).asInstanceOf[Node])
          case None => None
        }
      }

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

    def toHtml(mode: DisplayMode): TypedTag[String] =
      if (getVisibleChildren(mode).isEmpty) toHtmlAxiom(mode) else toHtmlSubtree(mode)

    def toHtmlAxiom(mode: DisplayMode): TypedTag[String]

    def toHtmlSubtree(mode: DisplayMode): TypedTag[String]

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
      case Some(n) => throw new NodeParentWrongTypeException(classOf[ExprNode], n.getClass)
    }

    override def getParent: Option[ExprNode] = {
      if (!isParentInitialised) markRoot()
      super.getParent match {
        case Some(n: ExprNode) => Some(n)
        case None              => None
        case Some(n)           => throw new NodeParentWrongTypeException(classOf[ExprNode], n.getClass)
      }
    }

    def depth: Int = getParent match {
      case Some(value) => value.depth + 1
      case None        => 0
    }

    private def checkDepthLimitWillBeExceeded(currDepth: Int = 0): Unit = {
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
      childrenFunction: Expr => Map[Variable, T] => List[(Term, Map[Variable, T])],
      parentEnvFunction: ExprNode => Map[Variable, T]
    ): Map[Variable, T] = getParent match {
      case Some(value) =>
        val parentEnv = parentEnvFunction(value)
        val parentExpr = value.getExpr
        val parentChildren = childrenFunction(parentExpr)(parentEnv)
        parentChildren.find(_._1 eq getExpr).map(_._2).getOrElse(parentEnv)
      case None => Map()
    }

    lazy val getEditEnv: Env = getCorrectEnv(_.getChildrenBase, _.getEditEnv)

    lazy val getEvalEnv: Env = getCorrectEnv(_.getChildrenEval, _.getEvalEnv)

    lazy val getTypeEnv: TypeEnv = getCorrectEnv(_.getChildrenTypeCheck, _.getTypeEnv)

    def toHtmlAxiom(mode: DisplayMode): TypedTag[String] = {
      div(
        cls := "subtree axiom" + phantomClassName,
        data("tree-path") := treePathString,
        data("node-string") := toString,
        divByMode(mode, true),
        div(cls := "annotation-axiom", exprName)
      )
    }

    def toHtmlSubtree(mode: DisplayMode): TypedTag[String] = {
      if (mode == DisplayMode.Evaluation && getParent.isEmpty) {
        checkDepthLimitWillBeExceeded()
      }

      div(
        cls := "subtree" + phantomClassName,
        data("tree-path") := treePathString,
        data("node-string") := toString,
        divByMode(mode, false),
        div(cls := "args", getVisibleChildren(mode).map(_.toHtml(mode)), div(cls := "annotation-new", exprName))
      )
    }

    private val divByModeCache = collection.mutable.Map[(DisplayMode, Boolean), TypedTag[String]]()

    private def divByMode(mode: DisplayMode, isAxiom: Boolean): TypedTag[String] = cacheQuery(
      divByModeCache,
      (mode, isAxiom),
      mode match {
        case DisplayMode.Edit       => editDiv(isAxiom)
        case DisplayMode.Evaluation => evalDiv(isAxiom)
        case DisplayMode.TypeCheck  => typeCheckDiv(isAxiom)
      }
    )

    private def editDiv(isAxiom: Boolean): TypedTag[String] = div(
      cls := (if (isAxiom) "expr" else "node"),
      envDiv(DisplayMode.Edit),
      if (isAxiom)
        (if (!isPhantom) toHtmlLine(DisplayMode.Edit) else toHtmlLineReadOnly(DisplayMode.Edit)) (display := "inline")
      else div(cls := "expr", if (!isPhantom) toHtmlLine(DisplayMode.Edit) else toHtmlLineReadOnly(DisplayMode.Edit)), {
        val typeCheckResult = getType
        if (typeCheckResult.isError) List(typeCheckTurnstileSpan, typeCheckResultDiv)
        else {
          val evalResult = getEditValueResult
          if (!evalResult.isError && !evalResult.isPlaceholder) List(evalArrowSpan, editEvalResultDiv)
          else List(typeCheckTurnstileSpan, typeCheckResultDiv)
        }
      }
    )

    private def typeCheckDiv(isAxiom: Boolean): TypedTag[String] = div(
      cls := (if (isAxiom) "expr" else "node"),
      envDiv(DisplayMode.TypeCheck),
      if (isAxiom) toHtmlLine(DisplayMode.TypeCheck)(display := "inline")
      else div(cls := "expr", toHtmlLine(DisplayMode.TypeCheck)),
      typeCheckTurnstileSpan,
      typeCheckResultDiv
    )

    private def evalDiv(isAxiom: Boolean): TypedTag[String] = div(
      cls := (if (isAxiom) "expr" else "node"),
      envDiv(DisplayMode.Evaluation),
      if (isAxiom) {
        (if (!isPhantom) toHtmlLine(DisplayMode.Evaluation) else toHtmlLineReadOnly(DisplayMode.Evaluation)) (
          display := "inline"
        )
      } else {
        div(
          cls := "expr",
          if (!isPhantom) toHtmlLine(DisplayMode.Evaluation) else toHtmlLineReadOnly(DisplayMode.Evaluation)
        )
      },
      evalArrowSpan,
      evalResultDiv
    )

    private lazy val typeCheckTurnstileSpan: TypedTag[String] =
      span(paddingLeft := "0.5ch", paddingRight := "0.5ch", raw(":"))

    private lazy val typeCheckResultDiv: TypedTag[String] =
      div(cls := "type-check-result", display := "inline", getType.toHtml)

    private lazy val evalArrowSpan: TypedTag[String] =
      span(paddingLeft := "1ch", paddingRight := "1ch", raw("&DoubleDownArrow;"))

    private lazy val evalResultDiv: TypedTag[String] = div(cls := "eval-result", display := "inline", getValue.toHtml)

    private lazy val editEvalResultDiv: TypedTag[String] =
      div(cls := "eval-result", display := "inline", getEditValueResult.toHtml)

    private def envDiv(mode: DisplayMode): TypedTag[String] = {
      val env: Env | TypeEnv = mode match {
        case DisplayMode.Edit       => getEditEnv
        case DisplayMode.Evaluation => getEvalEnv
        case DisplayMode.TypeCheck  => getTypeEnv
      }
      val envHtml: String =
        (if (env.nonEmpty)
           env.map((k: String, v: Value | Type) => s"$k &rarr; ${v.toHtml}").mkString("[", ", ", "]")
         else "") +
          (if (mode == DisplayMode.TypeCheck) " &#x22a2;" else if (env.nonEmpty) "," else "")

      div(
        cls := "scoped-variables",
        display := "inline",
        raw(envHtml),
        paddingRight := {
          if (envHtml.isEmpty) "0ch" else "0.5ch"
        }
      )
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

    private def phantomClassName: String = if (isPhantom) " phantom" else ""
  }

  /** Concrete implementation of an expression node.
    */
  case class VariableNode(exprName: String, args: List[InnerNode] = Nil) extends ExprNode {
    override val children: List[OuterNode] = args.flatMap(_.children)

    override def getExpr: Expr = exprOverride.getOrElse(expr)

    private var exprOverride: Option[Expr] = None

    def overrideExpr(e: Expr): Unit = {
      exprOverride = Some(e)
    }

    private lazy val exprClass: Class[Expr] = exprNameToClass(exprName) match {
      case Some(value) => value
      case None =>
        throw new IllegalArgumentException(s"Unknown expression type for ${lang.getClass.getSimpleName}: $exprName")
    }

    lazy val expr: Expr = {
      val constructor = exprClass.getConstructors.head
      val arguments = lang +: args.map {
        case n: SubExprNode => n.node.getExpr
        case n: LiteralNode => n.getLiteral
        case n: SubTypeNode => n.node.getType
      }
      constructor.newInstance(arguments: _*).asInstanceOf[Expr]
    }

    private val htmlLineCache = collection.mutable.Map[DisplayMode, TypedTag[String]]()
    private val htmlLineReadOnlyCache = collection.mutable.Map[DisplayMode, TypedTag[String]]()

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      cacheQuery(htmlLineCache, mode, div(raw(getExprHtmlLine(mode))))

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] =
      cacheQuery(htmlLineReadOnlyCache, mode, div(display := "inline", raw(getExprHtmlLineReadOnly(mode))))

    private def getExprHtmlLine(mode: DisplayMode): String = {
      val constructor = exprClass.getConstructors.head
      val arguments = lang +: args.map {
        case n: SubExprNode => n.getPlaceholder(mode)
        case n: LiteralNode => n.getPlaceholder(mode, false)
        case n: SubTypeNode => n.getPlaceholder(mode)
      }
      constructor.newInstance(arguments: _*).asInstanceOf[Expr].prettyPrint
    }

    private def getExprHtmlLineReadOnly(mode: DisplayMode): String = {
      val constructor = exprClass.getConstructors.head
      val arguments = lang +: args.map(_.getPlaceholder(mode))
      constructor.newInstance(arguments: _*).asInstanceOf[Expr].prettyPrint
    }

    override def toString: String = s"VariableNode(${UtilityFunctions.quote(exprName)}, $args)"

    children.foreach(_.setParent(Some(this)))
    args.foreach(_.setParent(Some(this)))
  }

  object VariableNode {
    def createFromExprName(exprName: String): Option[VariableNode] = {
      try {
        val exprClass = exprNameToClass(exprName).get
        val constructor = exprClass.getConstructors.head
        val innerNodes = constructor.getParameterTypes
          .map {
            case c if classOf[AbstractNodeLanguage] isAssignableFrom c => None
            case c if classOf[Expr] isAssignableFrom c                 => Some(SubExprNode(ExprChoiceNode()))
            case c if classOf[Literal] isAssignableFrom c              => Some(LiteralNode(""))
            case c if classOf[Type] isAssignableFrom c                 => Some(SubTypeNode(TypeChoiceNode()))
            case c => throw new ClickDeduceException(s"Unexpected parameter type in createFromExpr: $c")
          }
          .filter(_.isDefined)
          .map(_.get)
        val result = VariableNode(exprName, innerNodes.toList)
        innerNodes.foreach(_.setParent(Some(result)))
        Some(result)
      } catch {
        case _: Throwable => None
      }
    }

    def fromExpr(e: Expr): ExprNode = e match {
      case blank: BlankExprDropDown => ExprChoiceNode()
      case e =>
        val exprClass = e.getClass
        val constructor = exprClass.getConstructors()(0)
        val innerNodes = e match {
          case e0: Product =>
            val values = e0.productIterator.toList
            values.collect({
              case c: Expr    => SubExprNode(VariableNode.fromExpr(c))
              case c: Literal => LiteralNode(c.toString)
              case c: Type    => SubTypeNode(TypeNode.fromType(c))
            })
        }
        val result = VariableNode(e.getClass.getSimpleName, innerNodes)
        result.overrideExpr(e)
        innerNodes.foreach(_.setParent(Some(result)))
        result
    }
  }

  case class ExprChoiceNode() extends ExprNode {
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
    override def setParent(parentNode: Option[OuterNode]): Unit = parentNode match {
      case Some(n: ExprNode) => super.setParent(Some(n))
      case None              => throw new InnerNodeCannotBeRootException()
      case Some(n)           => throw new NodeParentWrongTypeException(classOf[ExprNode], n.getClass)
    }

    override def getParent: Option[ExprNode] = super.getParent match {
      case Some(n: ExprNode) => Some(n)
      case None              => None
      case Some(n)           => throw new NodeParentWrongTypeException(classOf[ExprNode], n.getClass)
    }

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      node.toHtmlLineReadOnly(mode)

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] = toHtmlLine(mode)

    override def getPlaceholder(mode: DisplayMode, readOnly: Boolean = true): ExprPlaceholder =
      ExprPlaceholder(node.toHtmlLineReadOnly(mode).toString, node.getExpr.needsBrackets)

    override val children: List[ExprNode] = List(node)
  }

  case class LiteralNode(literalText: String) extends InnerNode {
    private val htmlLineShared: TypedTag[String] = input(`type` := "text", cls := "literal", value := literalText)

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

    lazy val getTypeName: String = getType.getClass.getSimpleName

    override def getParent: Option[OuterNode] = {
      if (!isParentInitialised) markRoot()
      super.getParent match {
        case Some(n) => Some(n)
        case None    => None
      }
    }

    def toHtmlAxiom(mode: DisplayMode): TypedTag[String] = div(
      cls := "subtree axiom",
      data("tree-path") := treePathString,
      data("node-string") := toString,
      div(cls := "expr", toHtmlLine(mode)(display := "inline")),
      div(cls := "annotation-axiom", getTypeName)
    )

    def toHtmlSubtree(mode: DisplayMode): TypedTag[String] = div(
      cls := "subtree",
      data("tree-path") := treePathString,
      data("node-string") := toString,
      div(cls := "node", div(cls := "expr", toHtmlLine(mode))),
      div(cls := "args", children.map(_.toHtml(mode)), div(cls := "annotation-new", getTypeName))
    )
  }

  case class TypeNode(typeName: String, args: List[InnerNode]) extends TypeNodeParent {
    override lazy val getType: Type = {
      val constructor = typeClass.getConstructors()(0)
      val arguments = lang +: args.map {
        case tn: SubTypeNode => tn.node.getType
        case ln: LiteralNode => ln.getLiteral
      }
      constructor.newInstance(arguments: _*).asInstanceOf[Type]
    }

    private lazy val typeClass: Class[Type] = typeNameToClass(typeName) match {
      case Some(value) => value
      case None =>
        throw new IllegalArgumentException(s"Unknown expression type for ${lang.getClass.getSimpleName}: $typeName")
    }

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      div(raw(getExprHtmlLine(mode)))(data("tree-path") := treePathString)

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] =
      div(raw(getExprHtmlLineReadOnly(mode)))(readonly, disabled, display := "inline")

    override val children: List[OuterNode] = args.filter(_.isInstanceOf[SubTypeNode]).flatMap(_.children)

    override def toString: String = s"TypeNode(${UtilityFunctions.quote(typeName)}, $args)"

    private def getExprHtmlLine(mode: DisplayMode): String = {
      val constructor = typeClass.getConstructors()(0)
      val arguments = lang +: args.map(_.getPlaceholder(mode))
      constructor.newInstance(arguments: _*).asInstanceOf[Type].prettyPrint
    }

    private def getExprHtmlLineReadOnly(mode: DisplayMode): String = {
      val constructor = typeClass.getConstructors()(0)
      val arguments = lang +: args.map {
        case n: LiteralNode => LiteralAny(n.toHtmlLineReadOnly(mode).toString)
        case n: SubTypeNode => TypePlaceholder(n.node.toHtmlLineReadOnly(mode).toString, n.node.getType.needsBrackets)
      }
      constructor.newInstance(arguments: _*).asInstanceOf[Type].prettyPrint
    }

    children.foreach(_.setParent(Some(this)))
    args.foreach(_.setParent(Some(this)))
  }

  object TypeNode {
    def fromTypeName(typeName: String): Option[TypeNode] = typeNameToClass(typeName) match {
      case Some(typ) =>
        val constructor = typ.getConstructors()(0)
        val arguments = constructor.getParameterTypes
          .map({
            case c if classOf[Type] isAssignableFrom c             => Some(SubTypeNode(TypeChoiceNode()))
            case c if classOf[Literal] isAssignableFrom c          => Some(LiteralNode(""))
            case c if classOf[AbstractLanguage] isAssignableFrom c => None
          })
          .filter(_.isDefined)
          .map(_.get)
          .toList
        Some(TypeNode(typeName, arguments))
      case None => None
    }

    def fromType(typ: Type): TypeNodeParent = typ match {
      case blank: BlankTypeDropDown => TypeChoiceNode()
      case _ =>
        val typeClass = typ.getClass
        val constructor = typeClass.getConstructors()(0)
        val innerNodes = typ match {
          case e0: Product =>
            val values = e0.productIterator.toList
            values.collect({
              case c: Literal => LiteralNode(c.toString)
              case c: Type    => SubTypeNode(TypeNode.fromType(c))
            })
        }
        val result = TypeNode(typ.getClass.getSimpleName, innerNodes)
        innerNodes.foreach(_.setParent(Some(result)))
        result
    }
  }

  case class TypeChoiceNode() extends TypeNodeParent {
    override val args: List[InnerNode] = Nil

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      BlankTypeDropDown().toHtml(data("tree-path") := treePathString)

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] = toHtmlLine(mode)(readonly, disabled)

    override lazy val getType: Type = UnknownType()
  }

  case class SubTypeNode(node: TypeNodeParent) extends InnerNode {
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

  class NodeParentWrongTypeException(expected: Class[_ <: OuterNode], actual: Class[_ <: OuterNode])
      extends ClickDeduceException(s"Node parent has wrong type: expected $expected, got $actual")

  class InnerNodeCannotBeRootException extends ClickDeduceException("Inner node cannot be root")

  // class lists

  private lazy val exprClassList: List[Class[Expr]] = calculateExprClassList

  protected def calculateExprClassList: List[Class[Expr]]

  lazy val nodeLanguageInterface: Class[AbstractNodeLanguage] = findInterface(
    this.getClass, classOf[AbstractNodeLanguage]
  )

  private lazy val typeClassList: List[Class[Type]] = findSubClassesOf(
    getClass, classOf[Type], includeInterfaces = false
  )

  protected def calculateTypeClassList: List[Class[Type]] = calculateTypeClassList

  private lazy val blankClassList: List[Class[BlankSpace]] =
    List(classOf[BlankExprDropDown], classOf[BlankTypeDropDown]).map(_.asInstanceOf[Class[BlankSpace]])

  private lazy val nodeClassList: List[Class[Node]] = findSubClassesOf(nodeLanguageInterface, classOf[Node])

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
        case _ => Nil
      }
    val classes = parentSubclasses ++ subclasses.asInstanceOf[List[Class[T]]]
    classes.filterNot(cls => reflect.Modifier.isAbstract(cls.getModifiers)).filterNot(_.isInterface)
  }

  def findInterface[T](clazz: Class[_ <: AbstractNodeLanguage], interface: Class[T]): Class[T] = {
    clazz.getInterfaces.find(_ == interface) match {
      case Some(value) => value.asInstanceOf[Class[T]]
      case None => clazz.getSuperclass match {
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
