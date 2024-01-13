package languages

import app.UtilityFunctions
import scalatags.Text.TypedTag
import scalatags.Text.all.{div, raw, s, *}

import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.tailrec
import scala.util.parsing.combinator.*

trait AbstractNodeLanguage extends AbstractLanguage {
  lang =>

  private val blankIdCount: AtomicInteger = new AtomicInteger(0)

  trait BlankSpace extends Term {
    lazy val id: Int = blankIdCount.incrementAndGet()
  }

  case class BlankExprDropDown() extends Expr, BlankSpace {
    override lazy val toHtml: TypedTag[String] = exprClassListDropdownHtml(name := id.toString)
  }

  case class BlankChildPlaceholder() extends Expr, BlankSpace {
    override lazy val toHtml: TypedTag[String] = {
      span(cls := "blank-child-placeholder", data("blank-id") := id.toString, "?")
    }

    override lazy val childVersion: Expr = BlankExprDropDown()
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
    override lazy val toHtml: TypedTag[String] = typeClassListDropdownHtml(name := id.toString)
  }

  lazy val exprClassList: List[Class[Expr]] = calculateExprClassList

  protected def calculateExprClassList: List[Class[Expr]]

  lazy val typeClassList: List[Class[Type]] = calculateTypeClassList

  protected def calculateTypeClassList: List[Class[Type]] = List(classOf[UnknownType]).map(_.asInstanceOf[Class[Type]])

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

  private lazy val exprClassListDropdownHtml: TypedTag[String] = {
    select(
      cls := "expr-dropdown", onchange := "handleDropdownChange(this, \"expr\")",
      option(value := "", "Select Expr..."),
      exprClassList.map(e => {
        option(value := e.getSimpleName, e.getSimpleName)
      }
      )
    )
  }

  private lazy val typeClassListDropdownHtml: TypedTag[String] = {
    select(
      cls := "expr-dropdown", onchange := "handleDropdownChange(this, \"type\")",
      option(value := "", "Select Type..."),
      typeClassList.map(e => {
        option(value := e.getSimpleName, e.getSimpleName)
      }
      )
    )
  }

  /**
   * Create an `Term` given its string representation.
   *
   * @return The `Term` created, if successful.
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
        identifier ^^ (s => LiteralIdentifier(s))

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

    def toHtmlLine(mode: DisplayMode): TypedTag[String]

    def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String]

    def treePath: List[Int] = getParent match {
      case Some(value) => value.treePath :+ value.args.indexWhere(_ eq this)
      case None => Nil
    }

    def treePathString: String = treePath.mkString("-")
  }

  object Node {
    val innerNodeClasses: List[Class[_ <: Object]] = List(
      ExprChoiceNode.getClass, SubExprNode.getClass, LiteralNode.getClass
    )

    val outerNodeClasses: List[Class[_ <: Object]] = List(ConcreteNode.getClass, VariableNode.getClass)

    def read(s: String): Option[Node] = {
      def makeNode(name: String, args: List[Any], env: Env | TypeEnv = Map()): Option[Node] = {
        val nodeClass = nodeClassList.find(_.getSimpleName == name)
        nodeClass match {
          case Some(value) => {
            val constructor = value.getConstructors()(0)
            var arguments = AbstractNodeLanguage.this +: args.map {
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
          }
          case _ => throw new Exception("Unexpected error in innerNode")
        }

        def parseNode(s: String): ParseResult[Option[Node | Expr | Type]] = parseAll(outerNode, s.strip())
      }

      NodeParser.parseNode(s) match {
        case NodeParser.Success(Some(matched: Node), _) => {
          def parentify(node: Node): Unit = node match {
            case n: OuterNode => {
              n.children.foreach({ c =>
                c.setParent(n)
                parentify(c)
              }
              )
            }
            case n: InnerNode => {
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

    def toHtml(mode: DisplayMode): TypedTag[String] = if (children.isEmpty) toHtmlAxiom(mode) else toHtmlSubtree(mode)

    def toHtmlAxiom(mode: DisplayMode): TypedTag[String]

    def toHtmlSubtree(mode: DisplayMode): TypedTag[String]

    /**
     * Find the child of this expression tree at the given path.
     *
     * @param path the path to the child
     * @return the child at the given path, if it exists
     */
    def findChild(path: List[Int]): Option[Node] = path match {
      case Nil => Some(this)
      case head :: tail => {
        args(head) match {
          case SubExprNode(node) => node.findChild(tail)
          case SubTypeNode(node) => node.findChild(tail)
          case n: LiteralNode => tail match {
            case Nil => Some(n)
            case _ => throw new Exception(s"LiteralNode has no children, but path is not empty: $path")
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

    override def treePath: List[Int] = getParent match {
      case Some(value) => {
        val index: Int = value.args.indexWhere({
          case SubExprNode(node) => node eq this
          case SubTypeNode(node) => node eq this
          case _ => false
        }
        )
        if (index == -1) {
          if (isPhantom) Nil else throw new Exception("Could not find self in parent node's args")
        } else value.treePath :+ index
      }
      case None => Nil
    }

    def isPhantom: Boolean = false
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

    def getEditValueResult: Value = getExpr.eval(getEditEnv)

    def getValue: Value = getExpr.eval(getEnv)

    def getType: Type = getExpr.typeCheck(getTypeEnv)

    def getEditEnv: Env = getParent match {
      case Some(value) => {
        val parentExpressions: List[(Term, Env)] = value.getExpr.getChildrenBase(value.getEditEnv)
        parentExpressions.find(_._1 eq getExpr).map(_._2).getOrElse(Map())
      }
      case None => Map()
    }

    def getEnv: Env = getParent match {
      case Some(value) => {
        val parentExpressions: List[(Term, Env)] = value.getExpr.getChildrenEval(value.getEnv)
        parentExpressions.find(_._1 eq getExpr).map(_._2).getOrElse(Map())
      }
      case None => Map()
    }

    def getTypeEnv: TypeEnv = getParent match {
      case Some(value) => {
        val parentExpressions: List[(Term, TypeEnv)] = value.getExpr.getChildrenTypeCheck(value.getTypeEnv)
        parentExpressions.find(_._1 eq getExpr).map(_._2).getOrElse(Map())
      }
      case None => Map()
    }

    def toHtmlAxiom(mode: DisplayMode): TypedTag[String] = {
      div(
        cls := "subtree axiom" + phantomClassName,
        data("tree-path") := treePathString,
        data("term") := getExpr.toString,
        data("node-string") := toString,
        divByMode(mode, true),
        div(cls := "annotation-axiom", exprName)
      )
    }

    def toHtmlSubtree(mode: DisplayMode): TypedTag[String] = {
      div(
        cls := "subtree" + phantomClassName,
        data("tree-path") := treePathString,
        data("term") := getExpr.toString,
        data("node-string") := toString,
        divByMode(mode, false),
        div(
          cls := "args",
          getVisibleChildren(mode).map(_.toHtml(mode)),
          div(cls := "annotation-new", exprName)
        )
      )
    }

    def divByMode(mode: DisplayMode, isAxiom: Boolean): TypedTag[String] = mode match {
      case DisplayMode.Edit => editDiv(isAxiom)
      case DisplayMode.Evaluation => evalDiv(isAxiom)
      case DisplayMode.TypeCheck => typeCheckDiv(isAxiom)
    }

    def editDiv(isAxiom: Boolean): TypedTag[String] = div(
      cls := (if (isAxiom) "expr" else "node"),
      envDiv(DisplayMode.Edit),
      if (isAxiom) {
        (if (!isPhantom) toHtmlLine(DisplayMode.Edit) else toHtmlLineReadOnly(DisplayMode.Edit))(display := "inline")
      } else {
        div(cls := "expr", if (!isPhantom) toHtmlLine(DisplayMode.Edit) else toHtmlLineReadOnly(DisplayMode.Edit))
      },
      {
        val evalResult = getEditValueResult
        if (!evalResult.isError && !evalResult.isPlaceholder) {
          List(evalArrowSpan, editEvalResultDiv)
        } else {
          List(typeCheckTurnstileSpan, typeCheckResultDiv)
        }
      }
    )

    def typeCheckDiv(isAxiom: Boolean): TypedTag[String] = div(
      cls := (if (isAxiom) "expr" else "node"),
      envDiv(DisplayMode.TypeCheck),
      if (isAxiom) {
        toHtmlLine(DisplayMode.TypeCheck)(display := "inline")
      } else {
        div(cls := "expr", toHtmlLine(DisplayMode.TypeCheck))
      },
      typeCheckTurnstileSpan,
      typeCheckResultDiv
    )

    def evalDiv(isAxiom: Boolean): TypedTag[String] = div(
      cls := (if (isAxiom) "expr" else "node"),
      envDiv(DisplayMode.Evaluation),
      if (isAxiom) {
        (if (!isPhantom) toHtmlLine(DisplayMode.Evaluation) else toHtmlLineReadOnly(DisplayMode.Evaluation))(
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

    def typeCheckTurnstileSpan: TypedTag[String] = span(paddingLeft := "0.5ch", paddingRight := "0.5ch", raw(":"))

    def typeCheckResultDiv: TypedTag[String] = div(cls := "type-check-result", display := "inline", getType.toHtml)

    def evalArrowSpan: TypedTag[String] = span(paddingLeft := "1ch", paddingRight := "1ch", raw("&DoubleDownArrow;"))

    def evalResultDiv: TypedTag[String] = div(cls := "eval-result", display := "inline", getValue.toHtml)

    def editEvalResultDiv: TypedTag[String] = div(cls := "eval-result", display := "inline", getEditValueResult.toHtml)

    def envDiv(mode: DisplayMode): TypedTag[String] = {
      val env: Env | TypeEnv = mode match {
        case DisplayMode.Edit => getEditEnv
        case DisplayMode.Evaluation => getEnv
        case DisplayMode.TypeCheck => getTypeEnv
      }
      val envHtml: String = (if (env.nonEmpty)
        env.map((k: String, v: Value | Type) => s"$k &rarr; ${v.toHtml}").mkString("[", ", ", "]") else "") +
        (if (mode == DisplayMode.TypeCheck) " &#x22a2;" else if (env.nonEmpty) "," else "")

      div(
        cls := "scoped-variables", display := "inline",
        raw(envHtml),
        paddingRight := {
          if (envHtml.isEmpty) "0ch" else "0.5ch"
        }
      )
    }

    def getVisibleChildren(mode: DisplayMode): List[OuterNode] = mode match {
      case DisplayMode.Edit => children
      case DisplayMode.Evaluation => {
        val childExprs = getExpr.getChildrenEval(getEnv).map(_._1)
        var unconsumedChildren = children

        childExprs.flatMap({
          case expr: Expr => {
            val matchingChild = unconsumedChildren.collectFirst {
              case c: ExprNode if c.getExpr eq expr => c
              case c: ExprChoiceNode if c.getExpr == expr && !c.isPhantom => c
            }

            matchingChild match {
              case Some(childNode) => {
                unconsumedChildren = unconsumedChildren.filter(_ ne childNode)
                Some(childNode)
              }
              case None => {
                val newNode = VariableNode.fromExpr(expr)
                newNode.setParent(this)
                newNode.markPhantom()
                Some(newNode)
              }
            }
          }
        }
        )
      }
      case DisplayMode.TypeCheck => children
    }

    var isPhantomStore = false

    def markPhantom(): Unit = {
      isPhantomStore = true
    }

    override def isPhantom: Boolean = isPhantomStore

    def phantomClassName: String = if (isPhantom) " phantom" else ""
  }

  case class ConcreteNode(
    exprString: String,
    override val args: List[InnerNode] = Nil
  ) extends ExprNode {
    lazy val expr: Expr = readExpr(exprString).get

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] = expr.toHtml

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] = toHtmlLine(mode)

    override val exprName: String = expr.getClass.getSimpleName

    override def toString: String = s"ConcreteNode(${UtilityFunctions.quote(exprString)}, $args)"

    override def toHtml(mode: DisplayMode): TypedTag[String] =
      super.toHtml(mode)(data("term") := expr.toString)

    override val children: List[OuterNode] = args.flatMap(_.children)

    override def getExpr: Expr = expr

    children.foreach(_.setParent(this))
  }

  case class VariableNode(exprName: String, args: List[InnerNode] = Nil) extends ExprNode {
    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      div(raw(getExprHtmlLine(mode)))

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] =
      div(display := "inline", raw(getExprHtmlLineReadOnly(mode)))

    lazy val exprClass: Class[Expr] = exprNameToClass(exprName) match {
      case Some(value) => value
      case None => throw new IllegalArgumentException(
        s"Unknown expression type for ${lang.getClass.getSimpleName}: $exprName"
      )
    }

    override val children: List[OuterNode] = args.flatMap(_.children)

    override def toString: String = s"VariableNode(${UtilityFunctions.quote(exprName)}, $args)"

    lazy val expr: Expr = {
      val constructor = exprClass.getConstructors.head
      val arguments = lang +: args.map {
        case n: SubExprNode => n.node.getExpr
        case n: LiteralNode => n.getLiteral
        case n: SubTypeNode => n.node.getType
      }
      constructor.newInstance(arguments: _*).asInstanceOf[Expr]
    }

    var exprOverride: Option[Expr] = None

    override def getExpr: Expr = exprOverride.getOrElse(expr)

    def overrideExpr(e: Expr): Unit = {
      exprOverride = Some(e)
    }

    def getExprHtmlLine(mode: DisplayMode): String = {
      val constructor = exprClass.getConstructors.head
      val arguments = lang +: args.map {
        case n: SubExprNode => ExprPlaceholder(n.toHtmlLineReadOnly(mode).toString)
        case n: LiteralNode => LiteralAny(n.toHtmlLine(mode).toString)
        case n: SubTypeNode => TypePlaceholder(n.node.toHtmlLineReadOnly(mode).toString)
      }
      prettyPrint(constructor.newInstance(arguments: _*).asInstanceOf[Expr])
    }

    def getExprHtmlLineReadOnly(mode: DisplayMode): String = {
      val constructor = exprClass.getConstructors.head
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

    children.foreach(_.setParent(this))
    args.foreach(_.setParent(this))
  }

  object VariableNode {
    def createFromExprName(exprName: String): VariableNode = {
      val exprClass = exprNameToClass(exprName).get
      val constructor = exprClass.getConstructors()(0)
      val innerNodes = constructor.getParameterTypes.map {
        case c if classOf[AbstractNodeLanguage] isAssignableFrom c => None
        case c if classOf[Expr] isAssignableFrom c => Some(SubExprNode(ExprChoiceNode()))
        case c if classOf[Literal] isAssignableFrom c => Some(LiteralNode(""))
        case c if classOf[Type] isAssignableFrom c => Some(SubTypeNode(TypeChoiceNode()))
        case c => throw new Exception(s"Unexpected parameter type in createFromExpr: $c")
      }.filter(_.isDefined).map(_.get)
      val result = VariableNode(exprName, innerNodes.toList)
      innerNodes.foreach(_.setParent(result))
      result
    }

    def fromExpr(e: Expr): ExprNode = e match {
      case blank: BlankExprDropDown => {
        val result = ExprChoiceNode()
        result
      }
      case e => {
        val exprClass = e.getClass
        val constructor = exprClass.getConstructors()(0)
        val innerNodes = e match {
          case e0: Product => {
            val values = e0.productIterator.toList
            values.collect({
              case c: Expr => SubExprNode(VariableNode.fromExpr(c))
              case c: Literal => LiteralNode(c.toString)
              case c: Type => SubTypeNode(TypeNode.fromType(c))
            }
            )
          }
        }
        val result = VariableNode(e.getClass.getSimpleName, innerNodes)
        result.overrideExpr(e)
        innerNodes.foreach(_.setParent(result))
        result
      }
    }
  }

  case class ExprChoiceNode() extends ExprNode {
    override val args: List[InnerNode] = Nil

    override val children: List[OuterNode] = Nil

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      BlankExprDropDown().toHtml(data("tree-path") := treePathString)

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] =
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

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      node.toHtmlLineReadOnly(mode)

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] = toHtmlLine(mode)

    override val children: List[ExprNode] = List(node)

    //    children.foreach(_.setParent(this.getParent.get))
  }

  case class LiteralNode(literalText: String) extends InnerNode {
    override def toHtmlLine(mode: DisplayMode): TypedTag[String] = {
      input(
        `type` := "text",
        width := Math.max(2, literalText.length) + "ch",
        data("tree-path") := treePathString,
        value := literalText
      )
    }

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] = {
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

    def toHtmlAxiom(mode: DisplayMode): TypedTag[String] = {
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

    def toHtmlSubtree(mode: DisplayMode): TypedTag[String] = {
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

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      getType.toHtml(data("tree-path") := treePathString)

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] =
      toHtmlLine(mode)(readonly, disabled)

    override val children: List[OuterNode] = args.filter(_.isInstanceOf[SubTypeNode]).flatMap(_.children)

    override def toString: String = s"TypeNode(${UtilityFunctions.quote(typeName)}, $args)"

    def getExprHtmlLine(mode: DisplayMode): String = {
      val constructor = typeClass.getConstructors()(0)
      val arguments = lang +: args.map {
        case n: LiteralNode => LiteralAny(n.toHtmlLine(mode).toString)
        case n: SubTypeNode => TypePlaceholder(n.node.toHtmlLine(mode).toString)
      }
      prettyPrint(constructor.newInstance(arguments: _*).asInstanceOf[Expr])
    }

    def getExprHtmlLineReadOnly(mode: DisplayMode): String = {
      val constructor = typeClass.getConstructors()(0)
      val arguments = lang +: args.map {
        case n: LiteralNode => LiteralAny(n.toHtmlLineReadOnly(mode).toString)
        case n: SubTypeNode => TypePlaceholder(n.node.toHtmlLineReadOnly(mode).toString)
      }
      prettyPrint(constructor.newInstance(arguments: _*).asInstanceOf[Expr])
    }

    children.foreach(_.setParent(this))
    args.foreach(_.setParent(this))
  }

  object TypeNode {
    def fromTypeName(typeName: String): TypeNode = {
      typeNameToClass(typeName) match {
        case Some(typ) => {
          val constructor = typ.getConstructors()(0)
          val arguments = constructor.getParameterTypes.map({
            case c if classOf[Type] isAssignableFrom c => Some(SubTypeNode(TypeChoiceNode()))
            case c if classOf[Literal] isAssignableFrom c => Some(LiteralNode(""))
            case c if classOf[AbstractNodeLanguage] isAssignableFrom c => None
          }
          ).filter(_.isDefined).map(_.get).toList
          TypeNode(typeName, arguments)
        }
        case None => throw new IllegalArgumentException(
          s"Unknown expression type for ${lang.getClass.getSimpleName}: $typeName"
        )
      }
    }

    def fromType(typ: Type): TypeNode = {
      val typeClass = typ.getClass
      val constructor = typeClass.getConstructors()(0)
      val innerNodes = typ match {
        case e0: Product => {
          val values = e0.productIterator.toList
          values.collect({
            case c: Literal => LiteralNode(c.toString)
            case c: Type => SubTypeNode(TypeNode.fromType(c))
          }
          )
        }
      }
      val result = TypeNode(typ.getClass.getSimpleName, innerNodes)
      innerNodes.foreach(_.setParent(result))
      result
    }
  }

  case class TypeChoiceNode() extends TypeNodeParent {
    override val args: List[InnerNode] = Nil

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      BlankTypeDropDown().toHtml(data("tree-path") := treePathString)

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] =
      toHtmlLine(mode)(readonly, disabled)

    override def getType: Type = UnknownType()
  }

  case class SubTypeNode(node: TypeNodeParent) extends InnerNode {
    override val children: List[OuterNode] = List(node)

    override def toHtmlLine(mode: DisplayMode): TypedTag[String] =
      node.toHtmlLineReadOnly(mode)

    override def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] =
      toHtmlLine(mode)(readonly, disabled)
  }

}