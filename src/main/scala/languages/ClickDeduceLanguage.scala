package languages

import app.{FontWidthCalculator, UtilityFunctions}
import scalatags.Text.all.{div, raw, *}
import scalatags.Text.{TypedTag, attrs}

import java.awt.Font
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.tailrec
import scala.reflect.runtime.universe as ru
import scala.util.parsing.combinator.*

/**
 * Parent trait for all languages designed to be loaded in ClickDeduce.
 */
trait ClickDeduceLanguage extends AbstractLanguage {
  lang =>

  val blankIdCount: AtomicInteger = new AtomicInteger(0)

  trait BlankSpace extends Term {
    lazy val id: Int = blankIdCount.incrementAndGet()
  }

  case class BlankExprDropDown() extends Expr, BlankSpace {
    override lazy val toHtml: TypedTag[String] = {
      //      exprClassListDropdownHtml.replace("select", s"select name='$id'")
      //      select(name := id.toString, exprClassList.map(e => {
      //        option(value := e.getSimpleName, e.getSimpleName)
      //      }))
      exprClassListDropdownHtml(name := id.toString)
    }
  }

  case class BlankChildPlaceholder() extends Expr, BlankSpace {
    override lazy val toHtml: TypedTag[String] = {
      span(cls := "blank-child-placeholder", data("blank-id") := id.toString, "?")
    }

    override lazy val childVersion = BlankExprDropDown()
  }

  case class BlankValueInput() extends BlankSpace {
    override lazy val toHtml: TypedTag[String] = {
      input(name := id.toString, `type` := "text", placeholder := "Term")
    }
  }

  case class BlankExprArg() extends Expr, BlankSpace {
    override lazy val toHtml: TypedTag[String] = {
      input(name := id.toString, `type` := "text", placeholder := "Term")
    }
  }

  case class BlankLiteral() extends Literal, BlankSpace {
    override lazy val toHtml: TypedTag[String] = {
      input(name := id.toString, `type` := "text", placeholder := "Term")
    }

    val value: Any = ""
  }

  case class BlankTypeDropDown() extends Type, BlankSpace {
    override lazy val toHtml: TypedTag[String] = {
      typeClassListDropdownHtml(name := id.toString)
    }
  }

  //  def getSubclassesOf[T <: Class[Any]](parentClass: Class[T]): List[T] = {
  //    val runtimeMirror = ru.runtimeMirror(getClass.getClassLoader)
  //    val classSymbol = runtimeMirror.classSymbol(parentClass)
  //    val subclasses = classSymbol.knownDirectSubclasses
  //    subclasses.map(_.asClass).toList.asInstanceOf[List[T]]
  //  }

  def getSubclassesOf(parentClass: Class[_]): List[ru.ClassSymbol] = {
    val runtimeMirror = ru.runtimeMirror(getClass.getClassLoader)
    val classSymbol = runtimeMirror.classSymbol(parentClass)
    val subclasses = classSymbol.knownDirectSubclasses
    subclasses.map(_.asClass).toList
  }

  lazy val exprClassList: List[Class[Expr]] = calculateExprClassList

  protected def calculateExprClassList: List[Class[Expr]]

  lazy val typeClassList: List[Class[Type]] = calculateTypeClassList

  protected def calculateTypeClassList: List[Class[Type]] = List(classOf[UnknownType]).map(_.asInstanceOf[Class[Type]])

  private lazy val exprClassListDropdownHtml: TypedTag[String] = {
    select(
      cls := "expr-dropdown", onchange := "handleDropdownChange(this)",
      option(value := "", "Select Expr..."),
      exprClassList.map(e => {
        option(value := e.getSimpleName, e.getSimpleName)
      }
      )
    )
  }

  private lazy val typeClassListDropdownHtml: TypedTag[String] = {
    select(
      cls := "expr-dropdown", onchange := "handleDropdownChange(this)",
      option(value := "", "Select Expr..."),
      typeClassList.map(e => {
        option(value := e.getSimpleName, e.getSimpleName)
      }
      )
    )
  }

  protected lazy val blankClassList: List[Class[BlankSpace]] = {
    //    getClass.getClasses.filter(c => classOf[BlankSpace].isAssignableFrom(c)).map(_
    //    .asInstanceOf[Class[BlankSpace]]).toList
    //    getSubclassesOf(classOf[BlankSpace]).map(_.asInstanceOf[Class[BlankSpace]])
    List(
      classOf[BlankExprDropDown], classOf[BlankChildPlaceholder], classOf[BlankValueInput], classOf[BlankExprArg],
      classOf[BlankLiteral]
    ).map(_.asInstanceOf[Class[BlankSpace]])
  }

  protected lazy val nodeClassList: List[Class[Node]] = {
    //    getClass.getClasses.filter(c => classOf[Node].isAssignableFrom(c)).map(_.asInstanceOf[Class[Node]]).toList
    //    getSubclassesOf(classOf[Node]).map(_.asInstanceOf[Class[Node]])
    List(
      classOf[ConcreteNode], classOf[VariableNode], classOf[ExprChoiceNode], classOf[SubExprNode], classOf[LiteralNode],
      classOf[TypeNode], classOf[TypeChoiceNode], classOf[SubTypeNode]
    ).map(_.asInstanceOf[Class[Node]])
  }

  /**
   * Create an `Expr` given its name and a list of arguments.
   *
   * @param name The name of the `Expr` to be created. Must match the name of a class extending `Expr` in the
   *             language.
   * @param args The arguments to be passed to the constructor of the `Expr`.
   * @return The `Expr` created, if successful.
   */
  def parseTerm(s: String): Option[Term] = {
    @tailrec
    def makeTerm(name: String, args: List[Any]): Option[Term] = {
      def constructTermFromArgs[T](termClass: Class[T]): T = {
        val constructor = termClass.getConstructors()(0)
        val arguments = this +: args.map {
          case Some(e) => e
          case x => x
        }
        constructor.newInstance(arguments: _*).asInstanceOf[T]
      }

      val exprClass = exprClassList.find(_.getSimpleName == name)
      exprClass match {
        case Some(value) => {
          Some(constructTermFromArgs(value))
        }
        case None => {
          val blankClass = blankClassList.find(_.getSimpleName == name)
          blankClass match {
            case Some(value) => makeTerm("MissingExpr", Nil)
            case None => typeClassList.find(_.getSimpleName == name) match {
              case Some(value) => {
                Some(constructTermFromArgs(value))
              }
              case None => None
            }
          }
        }
      }
    }

    object TermParser extends JavaTokenParsers {
      def term: Parser[Option[Term]] = name ~ "(" ~ repsep(arg, "\\s*,\\s*".r) ~ ")" ^^ {
        case name ~ "(" ~ args ~ ")" => makeTerm(name, args)
        case _ => None
      }

      def name: Parser[String] = "[A-Za-z]\\w*".r

      def identifier: Parser[String] = "[A-Za-z_$][\\w_$]*".r

      def arg: Parser[Any] = term | stringLiteral ^^ (s => LiteralString(s)) |
        wholeNumber ^^ (n => LiteralInt(BigInt(n))) |
        "true" ^^ (_ => LiteralBool(true)) | "false" ^^ (_ => LiteralBool(false)) |
        identifier ^^ (s => LiteralAny(s))

      def parseTerm(s: String): ParseResult[Option[Term]] = parseAll(term, s.strip())
    }

    TermParser.parseTerm(s) match {
      case TermParser.Success(matched, _) => matched
      case x =>
        None
    }
  }

  /**
   * Function to load an `Expr` from a string.
   * Input must be in the format produced by `Expr.toString`
   *
   * @param s The string to be parsed.
   * @return The `Expr` represented by the string, if successful.
   */
  def readExpr(s: String): Option[Expr] = parseTerm(s) match {
    case Some(e: Expr) => Some(e)
    case _ => None
  }

  def readType(s: String): Option[Type] = parseTerm(s) match {
    case Some(t: Type) => Some(t)
    case _ => None
  }

  def exprNameToClass(name: String): Option[Class[Expr]] = {
    exprClassList.find(_.getSimpleName == name)
  }

  def typeNameToClass(name: String): Option[Class[Type]] = {
    typeClassList.find(_.getSimpleName == name)
  }

  def createTerm(name: String, args: List[Term]): Term = {
    val exprClass = exprNameToClass(name)
    exprClass match {
      case Some(value) => {
        val constructor = value.getConstructors()(0)
        constructor.newInstance(args: _*).asInstanceOf[Expr]
      }
      case None => {
        val blankClass = blankClassList.find(_.getSimpleName == name)
        blankClass match {
          case Some(value) => MissingExpr()
          case None => MissingExpr()
        }
      }
    }
  }

  def createUnfilledExpr(name: String): Expr = {
    val exprClass = exprNameToClass(name)
    exprClass match {
      case Some(value) => {
        val constructor = value.getConstructors()(0)
        val arguments = constructor.getParameterTypes.map {
          case c if classOf[ClickDeduceLanguage].isAssignableFrom(c) => lang
          case c if classOf[Expr].isAssignableFrom(c) => BlankChildPlaceholder()
          case _ => BlankLiteral()
        }
        //        val numArgs = constructor.getParameterCount - 1
        //        val arguments = List(lang) ++ (for {i <- 0 until numArgs} yield {
        //          BlankExprArg()
        //        })
        constructor.newInstance(arguments: _*).asInstanceOf[Expr]
      }
      case None => {
        val blankClass = blankClassList.find(_.getSimpleName == name)
        blankClass match {
          case Some(value) => MissingExpr()
          case None => MissingExpr()
        }
      }
    }
  }

  abstract class EvalTreeNode {
    val children: List[EvalTreeNode]
    var parent: Option[EvalTreeNode] = None

    lazy val toHtml: String

    lazy val toSvg: String = "<svg><text>Unfinished</text></svg>"

    lazy val toSvgGroup: String = "<g><text>Unfinished</text></g>"

    lazy val treeSvgSize: (Float, Float) = (0, 0)

    /**
     * Group the children of this expression tree by level.
     *
     * @return a map where keys are levels (integers) and values are lists of expression trees at that particular level
     */
    lazy val groupChildrenByLevel: Map[Int, List[EvalTreeNode]] = {
      def mergeMaps(offset: Int, maps: Map[Int, List[EvalTreeNode]]*): Map[Int, List[EvalTreeNode]] = {
        maps.foldLeft(Map[Int, List[EvalTreeNode]]()) { (acc, m) =>
          m.foldLeft(acc) { case (a, (level, trees)) =>
            a.updated(level + offset, a.getOrElse(level + offset, List()) ++ trees)
          }
        }
      }

      val currentLevelMap: Map[Int, List[EvalTreeNode]] = Map(0 -> children)
      val childrenMaps: Map[Int, List[EvalTreeNode]] = children.flatMap(child => child.groupChildrenByLevel).toMap

      mergeMaps(1, currentLevelMap, childrenMaps).filter(_._2.nonEmpty)
    }

    var initialTreePath: List[Int] = Nil

    def treePath: List[Int] = parent match {
      case Some(value) => value.treePath :+ value.children.indexWhere(_ eq this)
      case None => initialTreePath
    }

    def treePathString: String = treePath.mkString("-")

    /**
     * Find the child of this expression tree at the given path.
     *
     * @param path the path to the child
     * @return the child at the given path, if it exists
     */
    def findChild(path: List[Int]): Option[EvalTreeNode] = path match {
      case Nil => Some(this)
      case head :: tail => children.lift(head).flatMap(_.findChild(tail))
    }
  }

  enum NodeDisplayMode:
    case Edit, Evaluation, TypeCheck

  object NodeDisplayMode {
    def fromString(s: String): NodeDisplayMode = s match {
      case "edit" => Edit
      case "eval" => Evaluation
      case "type-check" => TypeCheck
    }
  }

  abstract class Node {
    val children: List[OuterNode] = Nil

    private var parent: Option[OuterNode] = None

    def getParent: Option[OuterNode] = parent

    def setParent(parentNode: OuterNode): Unit = getParent match {
      case None => parent = Some(parentNode)
      case Some(value) => parent = Some(parentNode)
      //        if (!(value eq parentNode))
      //          throw new Exception("Cannot initialise parent multiple times")
    }

    def toHtmlLine(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String]

    def toHtmlLineReadOnly(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String]

    def treePath: List[Int] = getParent match {
      case Some(value) => value.treePath :+ value.args.indexWhere(_ eq this)
      case None => Nil
    }

    def treePathString: String = treePath.mkString("-")
  }

  object Node {
    val innerNodeClasses = List(ExprChoiceNode.getClass, SubExprNode.getClass, LiteralNode.getClass)

    val outerNodeClasses = List(ConcreteNode.getClass, VariableNode.getClass)

    def read(s: String): Option[Node] = {
      def makeNode(name: String, args: List[Any], env: Env | TypeEnv = Map()): Option[Node] = {
        val nodeClass = nodeClassList.find(_.getSimpleName == name)
        nodeClass match {
          case Some(value) => {
            val constructor = value.getConstructors()(0)
            var arguments = ClickDeduceLanguage.this +: args.map {
              case LiteralString(s) => s.stripPrefix("\"").stripSuffix("\"")
              case Some(e) => e
              case x => x
            }
            if (constructor.getParameterTypes.last.isAssignableFrom(classOf[Env])) {
              arguments = arguments :+ env
            }
            Some(constructor.newInstance(arguments: _*).asInstanceOf[Node])
          }
          case None => None
        }
      }

      object NodeParser extends JavaTokenParsers {
        def outerNode: Parser[Option[OuterNode | Expr | Type]] = outerNodeName ~ "(" ~ repsep(
          outerNodeArg, "\\s*,\\s*".r
        ) ~ ")" ^^ {
          case name ~ "(" ~ args ~ ")" => {
            if (name.endsWith("Node")) {
              val node = makeNode(name, args)
              node match {
                case Some(n: OuterNode) => {
                  n.children.foreach(_.setParent(n))
                  Some(n)
                }
                case _ => throw new Exception("Unexpected error in outerNode")
              }
            } else {
              val exprString = s"$name(${args.mkString(", ")})"
              readExpr(exprString) match {
                case Some(expr) => Some(expr)
                case None => readType(exprString)
              }
            }
          }
          case _ => None
        }

        def name: Parser[String] = "[A-Za-z]\\w*".r

        def outerListParse: Parser[List[Any]] = "Nil" ^^ { _ => Nil } | "List(" ~ repsep(
          outerNodeArg, "\\s*,\\s*".r
        ) ~ ")" ^^ {
          case _ ~ args ~ _ => args.filter(_ != None).map {
            case Some(e) => e
            case x => x
          }
        }

        def outerNodeName: Parser[String] = "ExprChoiceNode" | "ConcreteNode" | "VariableNode" | "TypeChoiceNode" |
          "TypeNode" // outerNodeClasses
        // .map(_.getSimpleName.stripSuffix("$")).mkString("|").r

        def innerNodeName: Parser[String] = "SubExprNode" | "LiteralNode" | "SubTypeNode" //
        // innerNodeClasses.map(_.getSimpleName
        // .stripSuffix("$")).mkString("|").r

        def outerNodeArg: Parser[Any] = outerListParse | innerNode | stringLiteral ^^ (s => LiteralString(s))

        def innerNodeArg: Parser[Any] = outerNode | stringLiteral ^^ (s => LiteralString(s))

        def innerNode: Parser[InnerNode] = innerNodeName ~ "(" ~ repsep(innerNodeArg, "\\s*,\\s*".r) ~ ")" ^^ {
          case name ~ "(" ~ args ~ ")" => {
            val node = makeNode(name, args)
            node match {
              case Some(n: InnerNode) => n
              case _ => throw new Exception("Unexpected error in innerNode")
            }
            node.get.asInstanceOf[InnerNode]
          }
          case _ => throw new Exception("Unexpected error in innerNode")
        }

        def parseNode(s: String): ParseResult[Option[Node | Expr | Type]] = parseAll(outerNode, s.strip())
      }

      NodeParser.parseNode(s) match {
        case NodeParser.Success(Some(matched: Node), _) => {
          def parentify(node: Node): Unit = node match {
            case (n: OuterNode) => {
              n.children.foreach({ c =>
                c.setParent(n)
                parentify(c)
              }
              )
            }
            case (n: InnerNode) => {
              n.children.foreach({ c =>
                c.setParent(n.getParent.get)
                parentify(c)
              }
              )
            }
          }

          parentify(matched)
          Some(matched)
        }
        case x =>
          None
      }
    }

    def readPathString(s: String): List[Int] = s match {
      case "" => Nil
      case s => s.split("-").map(_.toInt).toList
    }
  }

  abstract class OuterNode extends Node {
    val args: List[InnerNode]

    def toHtml(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] =
      if (children.isEmpty) toHtmlAxiom(mode) else toHtmlSubtree(mode)

    def toHtmlAxiom(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String]

    def toHtmlSubtree(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String]

    /**
     * Find the child of this expression tree at the given path.
     *
     * @param path the path to the child
     * @return the child at the given path, if it exists
     */
    def findChild(path: List[Int]): Option[OuterNode] = path match {
      case Nil => Some(this)
      case head :: tail => {
        args(head) match {
          case SubExprNode(node) => node.findChild(tail)
          case _ => None
        }
      }
    }

    def replace(path: List[Int], replacement: Node): OuterNode = path match {
      case Nil => replacement match {
        case n: OuterNode => n
      }
      case head :: tail => {
        val updatedArgs = args.updated(
          head,
          args(head) match {
            case SubExprNode(node) => SubExprNode(node.replace(tail, replacement) match {
              case n: ExprNode => n
            }
            )
            case SubTypeNode(node) => SubTypeNode(node.replace(tail, replacement) match {
              case n: TypeNodeParent => n
            }
            )
            case LiteralNode(literalText) => tail match {
              case Nil => replacement match {
                case n: InnerNode => n
              }
            }
          }
        )

        this match {
          case VariableNode(exprName, _) => VariableNode(exprName, updatedArgs)
          case ConcreteNode(exprString, _) => ConcreteNode(exprString, updatedArgs)
          case TypeNode(typeName, _) => TypeNode(typeName, updatedArgs)
        }
      }
    }

    //    def replace(path: List[Int], replacement: OuterNode): OuterNode
    //
    //    def replaceInner(path: List[Int], replacement: InnerNode): OuterNode

    def insertExpr(newExprName: String, newTreePath: List[Int]): VariableNode = {
      val newNode = VariableNode.createFromExprName(newExprName)
      val oldNode = findChild(newTreePath).get
      oldNode match {
        case x: ExprChoiceNode => {
          replace(newTreePath, newNode) match {
            case v: VariableNode => v
            case x => throw new Exception(s"Unexpected node kind after replaceInner in insertExpr: $x")
          }
        }
        case x => throw new Exception(s"Unexpected node kind in insertExpr: $x")
      }
    }

    override def treePath: List[Int] = getParent match {
      case Some(value) => {
        val index: Int = value.args.indexWhere({
          case SubExprNode(node) => node eq this
          case SubTypeNode(node) => node eq this
          case _ => false
        }
        )
        if (index == -1) {
          throw new Exception("Could not find self in parent node's args")
        }
        value.treePath :+ index
      }
      case None => Nil
    }
  }

  abstract class ExprNode extends OuterNode {
    override def setParent(parentNode: OuterNode): Unit = parentNode match {
      case n: ExprNode => super.setParent(n)
    }

    override def getParent: Option[ExprNode] = super.getParent match {
      case Some(n: ExprNode) => Some(n)
      case None => None
    }

    val exprName: String

    def getExpr: Expr

    def getValue: Value = eval(getExpr, getEnv)

    def getType: Type = typeOf(getExpr, getTypeEnv)

    def getEnv: Env = getParent match {
      case Some(value) =>
        value.getExpr.childExprEnvs(value.getEnv)(value.children.indexWhere(_ eq this))
      case None => Map()
    }

    def getTypeEnv: TypeEnv = getParent match {
      case Some(value) => value.getExpr.childExprTypeEnvs(value.getTypeEnv)(value.children.indexWhere(_ eq this))
      case None => Map()
    }

    def toHtmlAxiom(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] = {
      div(
        cls := "subtree axiom",
        data("tree-path") := treePathString,
        data("term") := getExpr.toString,
        data("node-string") := toString,
        div(
          cls := "expr",
          envDiv(mode),
          toHtmlLine(mode)(display := "inline"),
          if (mode == NodeDisplayMode.TypeCheck) {
            List(typeCheckTurnstileSpan, typeCheckResultDiv)
          } else {
            List(evalArrowSpan, evalResultDiv)
          }
        ),
        div(cls := "annotation-axiom", exprName)
      )
    }

    def toHtmlSubtree(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] = {
      div(
        cls := "subtree",
        data("tree-path") := treePathString,
        data("term") := getExpr.toString,
        data("node-string") := toString,
        div(
          cls := "node",
          envDiv(mode),
          div(cls := "expr", toHtmlLine(mode)),
          if (mode == NodeDisplayMode.TypeCheck) {
            List(typeCheckTurnstileSpan, typeCheckResultDiv)
          } else {
            List(evalArrowSpan, evalResultDiv)
          }
        ),
        div(
          cls := "args",
          getVisibleChildren(mode).map(_.toHtml(mode)),
          div(cls := "annotation-new", exprName)
        )
      )
    }

    def typeCheckTurnstileSpan: TypedTag[String] = span(paddingLeft := "0.5ch", paddingRight := "0.5ch", raw(":"))

    def typeCheckResultDiv: TypedTag[String] = div(cls := "type-check-result", display := "inline", getType.toHtml)

    def evalArrowSpan: TypedTag[String] = span(paddingLeft := "1ch", paddingRight := "1ch", raw("&DoubleDownArrow;"))

    def evalResultDiv: TypedTag[String] = div(cls := "eval-result", display := "inline", getValue.toHtml)

    def envDiv(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] = {
      val env: Env | TypeEnv = if (mode == NodeDisplayMode.TypeCheck) {
        getTypeEnv
      } else {
        getEnv
      }
      val envHtml: String = if (env.nonEmpty) {
        env.map((k: String, v: Value | Type) => s"$k &rarr; ${v.toHtml}").mkString("[", ", ", "],")
      } else {
        if (mode == NodeDisplayMode.TypeCheck) {
          "&#x22a2;"
        } else {
          ""
        }
      }
      div(
        cls := "scoped-variables", display := "inline",
        raw(envHtml),
        paddingRight := {
          if (envHtml.isEmpty) "0ch" else "0.5ch"
        }
      )
    }

    def getVisibleChildren(mode: NodeDisplayMode): List[OuterNode] = mode match {
      case NodeDisplayMode.Edit => children
      case NodeDisplayMode.Evaluation => {
        val childExprs = getExpr.getEvalChildren(Map())
        children.filter({
          case c: ExprNode => childExprs.contains(c.getExpr)
          case _ => false
        }
        )
      }
      case NodeDisplayMode.TypeCheck => children
    }

    //    def replace(path: List[Int], replacement: OuterNode): ExprNode
    //
    //    def replaceInner(path: List[Int], replacement: InnerNode): ExprNode
  }

  case class ConcreteNode(
    exprString: String,
    override val args: List[InnerNode] = Nil
  ) extends ExprNode {
    lazy val expr: Expr = readExpr(exprString).get

    override def toHtmlLine(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] = expr.toHtml

    override def toHtmlLineReadOnly(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] = toHtmlLine(mode)

    override val exprName: String = expr.getClass.getSimpleName

    override def toString: String = s"ConcreteNode(${UtilityFunctions.quote(exprString)}, $args)"

    override def toHtml(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] =
      super.toHtml(mode)(data("term") := expr.toString)

    override val children: List[OuterNode] = args.flatMap(_.children)

    override def getExpr: Expr = expr

    children.foreach(_.setParent(this))
  }

  case class VariableNode(exprName: String, args: List[InnerNode] = Nil) extends ExprNode {
    override def toHtmlLine(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] =
      div(raw(getExprHtmlLine(mode)))

    override def toHtmlLineReadOnly(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] =
      div(display := "inline", raw(getExprHtmlLineReadOnly(mode)))

    lazy val exprClass: Class[Expr] = exprNameToClass(exprName) match {
      case Some(value) => value
      case None => throw new IllegalArgumentException(
        s"Unknown expression type for ${lang.getClass.getSimpleName}: $exprName"
      )
    }

    override val children: List[OuterNode] = args.flatMap(_.children)

    override def toString: String = s"VariableNode(${UtilityFunctions.quote(exprName)}, $args)"

    override def getExpr: Expr = {
      val constructor = exprClass.getConstructors()(0)
      val arguments = lang +: args.map {
        case n: SubExprNode => n.node.getExpr
        case n: LiteralNode => n.getLiteral
        case n: SubTypeNode => n.node.getType
      }
      constructor.newInstance(arguments: _*).asInstanceOf[Expr]
    }

    def getExprHtmlLine(mode: NodeDisplayMode = NodeDisplayMode.Edit): String = {
      val constructor = exprClass.getConstructors()(0)
      val arguments = lang +: args.map {
        case n: SubExprNode => ExprPlaceholder(n.toHtmlLineReadOnly(mode).toString)
        case n: LiteralNode => LiteralAny(n.toHtmlLine(mode).toString)
        case n: SubTypeNode => TypePlaceholder(n.node.toHtmlLine(mode).toString)
      }
      prettyPrint(constructor.newInstance(arguments: _*).asInstanceOf[Expr])
    }

    def getExprHtmlLineReadOnly(mode: NodeDisplayMode = NodeDisplayMode.Edit): String = {
      val constructor = exprClass.getConstructors()(0)
      val arguments = lang +: args.map {
        case n: SubExprNode => ExprPlaceholder(n.toHtmlLineReadOnly(mode).toString)
        case n: LiteralNode => LiteralAny(n.toHtmlLineReadOnly(mode).toString)
        case n: SubTypeNode => TypePlaceholder(n.node.toHtmlLineReadOnly(mode).toString)
      }
      prettyPrint(constructor.newInstance(arguments: _*).asInstanceOf[Expr])
    }

    def nonErrorEvalResult: Boolean = getValue match {
      case _: EvalError => false
      case _ => true
    }

    //    def replace(path: List[Int], replacement: OuterNode): VariableNode = path match {
    //      case Nil => replacement.asInstanceOf[VariableNode] // TODO: type unsafe
    //      case head :: tail => {
    //        val newArgs: List[InnerNode] = args.updated(
    //          head,
    //          args(head) match {
    //            case SubExprNode(node) => SubExprNode(node.replace(tail, replacement))
    //            case _ => ???
    //          }
    //        )
    //        VariableNode(exprName, newArgs)
    //      }
    //    }
    //
    //    def replaceInner(path: List[Int], replacement: InnerNode): VariableNode = path match {
    //      case Nil => this
    //      case head :: Nil => {
    //        val newArgs = args.updated(head, replacement)
    //        VariableNode(exprName, newArgs)
    //      }
    //      case head :: tail => args(head) match {
    //        case SubExprNode(node) => node match {
    //          case n: VariableNode => {
    //            val newNode = SubExprNode(n.replaceInner(tail, replacement))
    //            val newArgs = args.updated(head, newNode)
    //            VariableNode(exprName, newArgs)
    //          }
    //        }
    //      }
    //    }


    children.foreach(_.setParent(this))
    args.foreach(_.setParent(this))
  }

  object VariableNode {
    def createFromExprName(exprName: String): VariableNode = {
      val exprClass = exprNameToClass(exprName).get
      val constructor = exprClass.getConstructors()(0)
      val innerNodes = constructor.getParameterTypes.map {
        case c if classOf[ClickDeduceLanguage] isAssignableFrom c => None
        case c if classOf[Expr] isAssignableFrom c => Some(SubExprNode(ExprChoiceNode()))
        case c if classOf[Literal] isAssignableFrom c => Some(LiteralNode(""))
        case c if classOf[Type] isAssignableFrom c => Some(SubTypeNode(TypeChoiceNode()))
        case c => throw new Exception(s"Unexpected parameter type in createFromExpr: $c")
      }.filter(_.isDefined).map(_.get)
      val result = VariableNode(exprName, innerNodes.toList)
      innerNodes.foreach(_.setParent(result))
      result
    }
  }

  case class ExprChoiceNode() extends ExprNode {
    override val args: List[InnerNode] = Nil

    override val children: List[OuterNode] = Nil

    override def toHtmlLine(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] =
      BlankExprDropDown().toHtml(data("tree-path") := treePathString)

    override def toHtmlLineReadOnly(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] =
      toHtmlLine(mode)(readonly, disabled)

    override val exprName: String = "ExprChoice"

    override def getExpr: Expr = BlankExprDropDown()
  }

  abstract class InnerNode extends Node {
  }

  case class SubExprNode(node: ExprNode) extends InnerNode {
    override def setParent(parentNode: OuterNode): Unit = parentNode match {
      case n: ExprNode => super.setParent(n)
    }

    override def getParent: Option[ExprNode] = super.getParent match {
      case Some(n: ExprNode) => Some(n)
      case None => None
    }

    override def toHtmlLine(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] =
      node.toHtmlLineReadOnly(mode)

    override def toHtmlLineReadOnly(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] = toHtmlLine(mode)

    override val children: List[ExprNode] = List(node)

    //    children.foreach(_.setParent(this.getParent.get))
  }

  case class LiteralNode(literalText: String) extends InnerNode {
    override def toHtmlLine(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] = {
      input(
        `type` := "text",
        width := Math.max(2, literalText.length) + "ch",
        data("tree-path") := treePathString,
        value := literalText
      )
    }

    override def toHtmlLineReadOnly(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] = {
      input(
        `type` := "text",
        readonly,
        disabled,
        width := Math.max(1, literalText.length) + "ch",
        value := literalText
      )
    }

    override val children: List[OuterNode] = Nil

    override def toString: String = {
      s"LiteralNode(${UtilityFunctions.quote(literalText)})"
    }

    override def treePath: List[Int] = getParent match {
      case Some(value: VariableNode) => value.treePath :+ value.args.indexWhere(_ eq this)
      case _ => Nil
    }

    def getLiteral: Literal = Literal.fromString(literalText)
  }

  abstract class TypeNodeParent extends OuterNode {
    def getType: Type

    def getTypeName: String = getType.getClass.getSimpleName

    def toHtmlAxiom(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] = {
      div(
        cls := "subtree axiom",
        data("tree-path") := treePathString,
        data("term") := getType.toString,
        data("node-string") := toString,
        div(
          cls := "expr",
          toHtmlLine(mode)(display := "inline"),
        ),
        div(cls := "annotation-axiom", getTypeName)
      )
    }

    def toHtmlSubtree(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] = {
      div(
        cls := "subtree",
        data("tree-path") := treePathString,
        data("term") := getType.toString,
        data("node-string") := toString,
        div(
          cls := "node",
          div(cls := "expr", toHtmlLine(mode)),
        ),
        div(
          cls := "args",
          children.map(_.toHtml(mode)),
          div(cls := "annotation-new", getTypeName)
        )
      )
    }
  }

  case class TypeNode(typeName: String, args: List[InnerNode]) extends TypeNodeParent {
    override def getType: Type = {
      val constructor = typeClass.getConstructors()(0)
      val arguments = lang +: args.map {
        case tn: SubTypeNode => tn.node.getType
        case ln: LiteralNode => ln.getLiteral
      }
      constructor.newInstance(arguments: _*).asInstanceOf[Type]
    }

    protected lazy val typeClass: Class[Type] = typeNameToClass(typeName) match {
      case Some(value) => value
      case None => throw new IllegalArgumentException(
        s"Unknown expression type for ${lang.getClass.getSimpleName}: $typeName"
      )
    }

    override def toHtmlLine(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] =
      getType.toHtml(data("tree-path") := treePathString)

    override def toHtmlLineReadOnly(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] =
      toHtmlLine(mode)(readonly, disabled)

    override val children: List[OuterNode] = args.filter(_.isInstanceOf[SubTypeNode]).flatMap(_.children)
  }

  object TypeNode {
    def fromTypeName(typeName: String): TypeNode = {
      typeNameToClass(typeName) match {
        case Some(typ) => {
          val constructor = typ.getConstructors()(0)
          val arguments = constructor.getParameterTypes.map({
            case c if classOf[Type] isAssignableFrom c => Some(SubTypeNode(TypeChoiceNode()))
            case c if classOf[Literal] isAssignableFrom c => Some(LiteralNode(""))
            case c if classOf[ClickDeduceLanguage] isAssignableFrom c => None
          }
          ).filter(_.isDefined).map(_.get).toList
          TypeNode(typeName, arguments)
        }
        case None => throw new IllegalArgumentException(
          s"Unknown expression type for ${lang.getClass.getSimpleName}: $typeName"
        )
      }
    }
  }

  case class TypeChoiceNode() extends TypeNodeParent {
    override val args: List[InnerNode] = Nil

    override def toHtmlLine(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] =
      BlankTypeDropDown().toHtml(data("tree-path") := treePathString)

    override def toHtmlLineReadOnly(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] =
      toHtmlLine(mode)(readonly, disabled)

    override def getType: Type = UnknownType()
  }

  case class SubTypeNode(node: TypeNodeParent) extends InnerNode {
    override val children: List[OuterNode] = List(node)

    override def toHtmlLine(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] =
      node.toHtmlLineReadOnly(mode)

    override def toHtmlLineReadOnly(mode: NodeDisplayMode = NodeDisplayMode.Edit): TypedTag[String] = toHtmlLine(mode)
  }


  def getActionClass(actionName: String): Class[Action] = (actionName match {
    case "SelectExprAction" => classOf[SelectExprAction]
    case "EditLiteralAction" => classOf[EditLiteralAction]
    case "DeleteAction" => classOf[DeleteAction]
    case "InsertAction" => classOf[InsertAction]
    case "PasteAction" => classOf[PasteAction]
    case "IdentityAction" => classOf[IdentityAction]
  }).asInstanceOf[Class[Action]]

  def createAction(
    actionName: String,
    nodeString: String,
    treePathString: String,
    extraArgs: List[String],
    modeName: String = "edit",
  ): Action = {
    val node = Node.read(nodeString).get
    val treePath = Node.readPathString(treePathString)
    val actionClass = getActionClass(actionName)
    val constructor = actionClass.getConstructors()(0)
    var remainingExtraArgs = extraArgs
    val arguments = constructor.getParameterTypes.map {
      case c if classOf[ClickDeduceLanguage] isAssignableFrom c => this
      case c if classOf[Node] isAssignableFrom c => node
      case c if classOf[List[Int]] isAssignableFrom c => treePath
      case c if classOf[String] isAssignableFrom c => {
        val arg = remainingExtraArgs.head
        remainingExtraArgs = remainingExtraArgs.tail
        arg
      }
      case c => throw new Exception(s"Unexpected parameter type in createAction: $c")
    }
    val result = constructor.newInstance(arguments: _*)
    result.asInstanceOf[Action]
  }

  abstract class Action(val originalTree: OuterNode, val treePath: List[Int]) {
    val newTree: OuterNode
  }

  case class SelectExprAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    exprChoiceName: String
  ) extends Action(originalTree, treePath) {
    override val newTree: OuterNode = originalTree.insertExpr(exprChoiceName, treePath)
  }

  case class EditLiteralAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    newLiteralText: String
  ) extends Action(originalTree, treePath) {
    override val newTree: OuterNode = originalTree.replace(treePath, LiteralNode(newLiteralText))
  }

  case class SelectTypeAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    typeChoiceName: String
  ) extends Action(originalTree, treePath) {
    override val newTree: OuterNode = originalTree.replace(treePath, TypeNode.fromTypeName(typeChoiceName))
  }

  //  case class CompleteEvaluationAction(override val originalTree: OuterNode, override val treePath: List[Int])
  //    extends Action(originalTree, treePath) {
  //    override val newTree: OuterNode = {
  //      originalTree match {
  //        case ConcreteNode(exprString, args) => {
  //          val expr = readExpr(exprString).get
  //          ???
  //          ConcreteNode(exprString, newArgs)
  //        }
  //      }
  //    }
  //  }

  case class DeleteAction(override val originalTree: OuterNode, override val treePath: List[Int])
    extends Action(originalTree, treePath) {
    override val newTree: OuterNode = originalTree.replace(treePath, ExprChoiceNode())
  }

  case class InsertAction(override val originalTree: OuterNode, override val treePath: List[Int], insertTree: OuterNode)
    extends Action(originalTree, treePath) {
    override val newTree: OuterNode = originalTree.replace(treePath, insertTree)
  }

  case class PasteAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    pasteNodeString: String
  )
    extends Action(originalTree, treePath) {
    private val pasteNode: Node = Node.read(pasteNodeString).get

    override val newTree: OuterNode = pasteNode match {
      case n: OuterNode => originalTree.replace(treePath, n)
      case n: InnerNode => originalTree.replace(treePath, n)
    }
  }

  case class IdentityAction(override val originalTree: OuterNode, override val treePath: List[Int])
    extends Action(originalTree, treePath) {
    override val newTree: OuterNode = originalTree
  }

  /**
   * Tree node representing an expression in this language.
   * Can be converted to an SVG.
   *
   * @param expr     the expression
   * @param value    the value of the expression (optional)
   * @param env      the environment in which the expression was evaluated (optional)
   * @param children the child nodes of the expression
   */
  case class ExpressionEvalTree(expr: Expr, value: Option[Value], env: Option[Env], children: List[EvalTreeNode])
    extends EvalTreeNode {
    private val XMLNS = "http://www.w3.org/2000/svg"
    private val FONT_NAME = "Courier New"
    private val FONT_SIZE = 16
    private val style =
      s"""line {stroke: black; stroke-width: 2; transform: translate(0px, -5px);}
      text {font-family: $FONT_NAME; font-size: ${FONT_SIZE}px; dominant-baseline: hanging}"""

    val HEIGHT_PER_ROW = 20
    val FONT = new Font(FONT_NAME, Font.PLAIN, FONT_SIZE)
    val GROUP_X_GAP = 20

    /**
     * Convert this expression tree to a full SVG.
     *
     * @return the SVG string
     */
    override lazy val toSvg: String = {
      val svg = new StringBuilder()
      svg.append(s"""<svg xmlns="$XMLNS" width="${treeSvgSize._1 + 15}" height="${treeSvgSize._2 + 20}">""")
      svg.append(s"""<style type="text/css">$style</style>""")
      svg.append(s"""<g transform="translate(5, ${treeSvgSize._2 - HEIGHT_PER_ROW + 8})">""")
      svg.append(toSvgGroup)
      svg.append("</g>")
      svg.append("</svg>")
      svg.toString
    }

    /**
     * Convert this expression tree to an SVG group.
     */
    override lazy val toSvgGroup: String = {
      def createGroup(content: String, translateAmount: (Float, Float) = (0, 0)) = {
        s"""<g transform="translate$translateAmount">$content</g>"""
      }

      val totalWidth = exprTextWidth + exprNameWidth
      val lineWidth = treeSvgSize._1 - exprNameWidth

      val childGroup = if (children.nonEmpty) {
        var currWidth = 0f
        val childGroups = for {i <- children.indices} yield {
          val child = children(i)
          val childSvg = child.toSvgGroup
          val x_translate = currWidth
          currWidth += child.treeSvgSize._1 + GROUP_X_GAP
          createGroup(childSvg, (x_translate, 0))
        }
        createGroup(childGroups.mkString(""), (0, -HEIGHT_PER_ROW))
      } else {
        ""
      }

      val xOffset = (treeSvgSize._1 - localSize._1 - exprNameWidth) / 2

      val textBlock = s"""<text x="$xOffset">$exprText</text>"""
      val line = s"""<line x1="0" x2="${lineWidth}" y1="0" y2="0" />"""
      val exprNameBlock = s"""<text x="${lineWidth + 2}" y="${FONT_SIZE / -2}">$exprName</text>"""

      val thisGroup = createGroup(line + textBlock + childGroup + exprNameBlock)
      val svg = new StringBuilder()
      svg.append(thisGroup)
      svg.toString()
    }

    /**
     * Convert this expression tree to an HTML representation.
     */
    override lazy val toHtml: String = {
      val mainAttributes = s"""data-tree-path="$treePathString" data-term="${expr.toString}""""
      if (children.isEmpty) {
        s"""
          <div class="subtree axiom" $mainAttributes>
            <div class="expr">$exprText</div>
            <div class="annotation-axiom">$exprName</div>
          </div>
          """
      } else
        s"""
         <div class="subtree" $mainAttributes>
           <div class="node">
             <div class="expr">$exprText</div>
           </div>

           <div class="args">
             ${children.map(_.toHtml).mkString("\n")}

             <div class="annotation-new">$exprName</div>
           </div>
         </div>
        """
    }

    /**
     * Convert the base of this expression tree to a string.
     * Includes HTML entities for certain Unicode characters.
     *
     * @return the string representation of the expression
     */
    lazy val exprText: String = {
      val turnstile = "&#x22a2;"
      val arrow = "&DoubleDownArrow;"

      val sb = new StringBuilder()
      if (env.isDefined) {
        val envText = env.get.map({ case (name, value): (Variable, Value) => s"$name := ${value.toHtml}" })
          .mkString(", ")
        sb.append(s"[$envText], ")
      }
      sb.append(expr.toHtml)
      if (value.isDefined) {
        sb.append(s" $arrow ${value.get.toHtml}")
      }
      sb.toString
    }

    /**
     * Calculate the width of the text representation of the text of this expression.
     *
     * @return the width of the text in pixels
     */
    lazy val exprTextWidth: Float = {
      FontWidthCalculator.calculateWidth(exprText, FONT)
    }

    lazy val exprName: String = {
      s"""(${expr.getClass.getSimpleName})"""
    }

    lazy val exprNameWidth: Float = {
      FontWidthCalculator.calculateWidth(exprName, FONT)
    }

    /**
     * Calculate the total size of the SVG for this expression tree.
     *
     * @return the size of the SVG in pixels, (width, height)
     */
    override lazy val treeSvgSize: (Float, Float) = {
      val groupedChildren = groupChildrenByLevel
      val height = HEIGHT_PER_ROW * (groupedChildren.size + 1)
      val width = {
        val childWidths = groupedChildren.values
          .map(group => group.map(_.treeSvgSize._1).sum + (group.length - 1) * GROUP_X_GAP)
        math.max(
          childWidths.maxOption.getOrElse(0f),
          localSize._1 + exprNameWidth
        )
      }
      (width, height)
    }

    lazy val localSize: (Float, Float) = {
      val height = HEIGHT_PER_ROW
      val width = exprTextWidth
      (width, height)
    }
  }


  case class ExprSelectNode() extends EvalTreeNode {
    override val children: List[EvalTreeNode] = Nil

    override lazy val toHtml: String = {
      BlankExprDropDown().toHtml.toString
    }
  }

  //  case class FillableExprNode(exprClass: Class[Expr], args: List[Expr | Literal], override val
  //  children: List[EvalTreeNode]) extends EvalTreeNode {
  //    override lazy val toHtml: String = {
  //
  //    }
  //  }

  object ExpressionEvalTree {
    def exprToTree(e0: Expr): ExpressionEvalTree = {
      val childTrees = e0.children.map(exprToTree)
      val valueResult: Option[Value] = lang.eval(e0) match {
        case e: EvalError => None
        case v => Some(v)
      }
      val result = ExpressionEvalTree(e0, valueResult, None, childTrees)
      childTrees.foreach(_.parent = Some(result))
      result
    }
  }
}
