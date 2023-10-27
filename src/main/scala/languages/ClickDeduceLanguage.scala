package languages

import app.{FontWidthCalculator, UtilityFunctions}

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
    override lazy val toHtml: String = {
      exprClassListDropdownHtml.replace("select", s"select name='$id'")
    }
  }

  case class BlankChildPlaceholder() extends Expr, BlankSpace {
    override lazy val toHtml: String = {
      s"<span class='blank-child-placeholder' data-blank-id='$id'>?</span>"
    }

    override lazy val childVersion = BlankExprDropDown()
  }

  case class BlankValueInput() extends BlankSpace {
    override lazy val toHtml: String = {
      s"<input name='$id' type='text' placeholder='Term'/>"
    }
  }

  case class BlankExprArg() extends Expr, BlankSpace {
    override lazy val toHtml: String = {
      s"<input name='$id' type='text' placeholder='Term'/>"
    }
  }

  case class BlankLiteral() extends Literal, BlankSpace {
    override lazy val toHtml: String = {
      s"<input name='$id' type='text' placeholder='Term'/>"
    }

    val value: Any = ""
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

  protected lazy val exprClassList: List[Class[Expr]] = {
//    getClass.getClasses.filter(c => classOf[Expr].isAssignableFrom(c)).map(_.asInstanceOf[Class[Expr]]).toList
//    getSubclassesOf(classOf[Expr]).map(_.asInstanceOf[Class[Expr]])
    Nil
  }

  private lazy val exprClassListDropdownHtml: String = {
    val exprClassListHtml = "<option value=\"\">Select Expr...</option>" ++ exprClassList.map(e => {
      s"""<option value="${e.getSimpleName}">${e.getSimpleName}</option>"""
    }).mkString("\n")
    s"""<select class="expr-dropdown" onchange="handleDropdownChange(this)">$exprClassListHtml</select>"""
  }

  protected lazy val blankClassList: List[Class[BlankSpace]] = {
//    getClass.getClasses.filter(c => classOf[BlankSpace].isAssignableFrom(c)).map(_.asInstanceOf[Class[BlankSpace]]).toList
//    getSubclassesOf(classOf[BlankSpace]).map(_.asInstanceOf[Class[BlankSpace]])
    List(classOf[BlankExprDropDown], classOf[BlankChildPlaceholder], classOf[BlankValueInput], classOf[BlankExprArg], classOf[BlankLiteral]).map(_.asInstanceOf[Class[BlankSpace]])
  }

  protected lazy val nodeClassList: List[Class[Node]] = {
//    getClass.getClasses.filter(c => classOf[Node].isAssignableFrom(c)).map(_.asInstanceOf[Class[Node]]).toList
//    getSubclassesOf(classOf[Node]).map(_.asInstanceOf[Class[Node]])
    List(classOf[ConcreteNode], classOf[VariableNode], classOf[ExprChoiceNode], classOf[SubExprNode], classOf[LiteralNode]).map(_.asInstanceOf[Class[Node]])
  }

  /**
   * Function to load an `Expr` from a string.
   * Input must be in the format produced by `Expr.toString`
   *
   * @param s The string to be parsed.
   * @return The `Expr` represented by the string, if successful.
   */
  def readExpr(s: String): Option[Expr] = {
    /**
     * Create an `Expr` given its name and a list of arguments.
     *
     * @param name The name of the `Expr` to be created. Must match the name of a class extending `Expr` in the language.
     * @param args The arguments to be passed to the constructor of the `Expr`.
     * @return The `Expr` created, if successful.
     */
    @tailrec
    def makeExpr(name: String, args: List[Any]): Option[Expr] = {
      val exprClass = exprClassList.find(_.getSimpleName == name)
      exprClass match
        case Some(value) => {
          val constructor = value.getConstructors()(0)
          val arguments = this +: args.map {
            case Some(e) => e
            case x => x
          }
          Some(constructor.newInstance(arguments: _*).asInstanceOf[Expr])
        }
        case None => {
          val blankClass = blankClassList.find(_.getSimpleName == name)
          blankClass match {
            case Some(value) => makeExpr("MissingExpr", Nil)
            case None => None
          }
        }
    }

    object ExprParser extends JavaTokenParsers {
      def expr: Parser[Option[Expr]] = name ~ "(" ~ repsep(arg, "\\s*,\\s*".r) ~ ")" ^^ {
        case name ~ "(" ~ args ~ ")" => makeExpr(name, args)
        case _ => None
      }

      def name: Parser[String] = "[A-Za-z]\\w*".r

      def arg: Parser[Any] = expr | stringLiteral ^^ (s => LiteralString(s)) | wholeNumber ^^ (n => LiteralInt(BigInt(n))) | "true" ^^ (_ => LiteralBool(true)) | "false" ^^ (_ => LiteralBool(false))

      def parseExpr(s: String): ParseResult[Option[Expr]] = parseAll(expr, s.strip())
    }

    ExprParser.parseExpr(s) match {
      case ExprParser.Success(matched, _) => matched
      case _ => None
    }
  }

  def exprNameToClass(name: String): Option[Class[Expr]] = {
    exprClassList.find(_.getSimpleName == name)
  }

  def createTerm(name: String, args: List[Term]) = {
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

  def createUnfilledExpr(name: String) = {
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

  abstract class Node {
    val children: List[OuterNode]

    var parent: Option[OuterNode] = None

    def toHtmlLine: String

    def toHtmlLineReadOnly: String

    var initialTreePath: List[Int] = Nil

    def treePath: List[Int] = parent match {
      case Some(value) => value.treePath :+ value.children.indexWhere(_ eq this)
      case None => initialTreePath
    }

    def treePathString: String = treePath.mkString("-")
  }

  object Node {
    val innerNodeClasses = List(ExprChoiceNode.getClass, SubExprNode.getClass, LiteralNode.getClass)

    val outerNodeClasses = List(ConcreteNode.getClass, VariableNode.getClass)

    def read(s: String): Option[Node] = {
      def makeNode(name: String, args: List[Any]): Option[Node] = {
        val nodeClass = nodeClassList.find(_.getSimpleName == name)
        nodeClass match
          case Some(value) => {
            val constructor = value.getConstructors()(0)
            val arguments = ClickDeduceLanguage.this +: args.map {
              case LiteralString(s) => s.stripPrefix("\"").stripSuffix("\"")
              case Some(e) => e
              case x => x
            }
            Some(constructor.newInstance(arguments: _*).asInstanceOf[Node])
          }
          case None => None
      }

      object NodeParser extends JavaTokenParsers {
        def outerNode: Parser[Option[OuterNode | Expr]] = outerNodeName ~ "(" ~ repsep(outerNodeArg, "\\s*,\\s*".r) ~ ")" ^^ {
          case name ~ "(" ~ args ~ ")" => {
            if (name.endsWith("Node")) {
              val node = makeNode(name, args)
              node match {
                case Some(n: OuterNode) => {
                  n.children.foreach(_.parent = Some(n))
                  Some(n)
                }
                case _ => throw new Exception("Unexpected error in outerNode")
              }
            } else {
              readExpr(s"$name(${args.mkString(", ")})")
            }
          }
          case _ => None
        }

        def name: Parser[String] = "[A-Za-z]\\w*".r

        def outerListParse: Parser[List[Any]] = "Nil" ^^ { _ => Nil} | "List(" ~ repsep(outerNodeArg, "\\s*,\\s*".r) ~ ")" ^^ {
          case _ ~ args ~ _ => args.filter(_ != None).map{
            case Some(e) => e
            case x => x
          }
        }

        def outerNodeName: Parser[String] = "ExprChoiceNode" | "ConcreteNode" | "VariableNode" // outerNodeClasses.map(_.getSimpleName.stripSuffix("$")).mkString("|").r

        def innerNodeName: Parser[String] = "SubExprNode" | "LiteralNode" //  innerNodeClasses.map(_.getSimpleName.stripSuffix("$")).mkString("|").r

        def outerNodeArg: Parser[Any] = outerListParse | innerNode | stringLiteral ^^ (s => LiteralString(s))

        def innerNodeArg: Parser[Any] = outerNode | stringLiteral ^^ (s => LiteralString(s))

        def innerNode: Parser[InnerNode] = innerNodeName ~ "(" ~ repsep(innerNodeArg, "\\s*,\\s*".r) ~ ")" ^^ {
          case name ~ "(" ~ args ~ ")" => {
            val node = makeNode(name, args)
            node.get.asInstanceOf[InnerNode]
          }
          case _ => throw new Exception("Unexpected error in innerNode")
        }

        def parseNode(s: String): ParseResult[Option[Node | Expr]] = parseAll(outerNode, s.strip())
      }

      NodeParser.parseNode(s) match {
        case NodeParser.Success(Some(matched: Node), _) => Some(matched)
        case x =>
          None
      }
    }

    def readPathString(s: String): List[Int] = s.split("-").map(_.toInt).toList
  }

  abstract class OuterNode extends Node {
    def attributes: Map[String, String] = Map("data-tree-path" -> treePathString)

    def toHtml: String = {
      val mainAttributes = s"""${attributes.map({ case (k, v) => s"$k='$v'" }).mkString(" ")}}"""
      if (children.isEmpty) {
        toHtmlAxiom(mainAttributes)
      } else
        toHtmlSubtree(mainAttributes)
    }

    def toHtmlAxiom(mainAttributes: String): String = {
      s"""
      <div class="subtree axiom" $mainAttributes>
        <div class="expr">$toHtmlLine</div>
        <div class="annotation-axiom">$exprName</div>
      </div>
      """
    }

    def toHtmlSubtree(mainAttributes: String): String = {
      s"""
       <div class="subtree" $mainAttributes>
         <div class="node">
           <div class="expr">$toHtmlLineReadOnly</div>
         </div>

         <div class="args">
           ${children.map(_.toHtml).mkString("\n")}

           <div class="annotation-new">$exprName</div>
         </div>
       </div>
      """
    }

    val exprName: String

    /**
     * Find the child of this expression tree at the given path.
     *
     * @param path the path to the child
     * @return the child at the given path, if it exists
     */
    def findChild(path: List[Int]): Option[OuterNode] = path match {
      case Nil => Some(this)
      case head :: tail => children.lift(head).flatMap(_.findChild(tail))
    }

    def replace(path: List[Int], replacement: OuterNode): OuterNode = {
      path match {
        case Nil => replacement
        case head :: tail => {
          val newChildren = children.updated(head, children(head).replace(tail, replacement))
          val node = this match {
            case ConcreteNode(s, _) => ConcreteNode(s, newChildren)
            case VariableNode(s, _) => VariableNode(s, newChildren.asInstanceOf[List[InnerNode]])
          }
          newChildren.foreach(_.parent = Some(node))
          node
        }
      }
    }

    def replaceInner(path: List[Int], replacement: InnerNode): OuterNode = {
      path match {
        case Nil => throw new Exception("Cannot replace inner node with outer node")
        case head :: tail => {
          val newChildren = children.updated(head, children(head).replaceInner(tail, replacement))
          val node = this match {
            case ConcreteNode(s, _) => ConcreteNode(s, newChildren)
            case VariableNode(s, _) => VariableNode(s, newChildren.asInstanceOf[List[InnerNode]])
          }
          newChildren.foreach(_.parent = Some(node))
          node
        }
      }
    }
  }

  case class ConcreteNode(exprString: String, children: List[OuterNode] = Nil) extends OuterNode {
    lazy val expr = readExpr(exprString).get

    override def toHtmlLine: String = expr.toHtml

    override def toHtmlLineReadOnly: String = toHtmlLine

    override def attributes: Map[String, String] = super.attributes + ("data-term" -> expr.toString)

    override val exprName: String = expr.getClass.getSimpleName

    override def toString: String = {
      s"ConcreteNode(${UtilityFunctions.quote(exprString)}, $children)"
    }
  }

  case class VariableNode(exprName: String, args: List[InnerNode] = Nil) extends OuterNode {
//        override def toHtmlLine: String = BlankExprDropDown().toHtml
    override def toHtmlLine: String = s"""${args.map(_.toHtmlLineReadOnly).mkString(" ")}"""

    override def toHtmlLineReadOnly: String = toHtmlLine.replace("select", "select readonly disabled")

    override val children: List[OuterNode] = args.flatMap(_.children)

    lazy val exprClass: Class[Expr] = exprNameToClass(exprName).get

    override def toString: String = {
      s"VariableNode(${UtilityFunctions.quote(exprName)}, $args)"
    }
  }

  object VariableNode {
    def createFromExpr(exprName: String) = {
      val exprClass = exprNameToClass(exprName).get
      val constructor = exprClass.getConstructors()(0)
      val innerNodes = constructor.getParameterTypes.map {
        case c if classOf[Expr] isAssignableFrom c => SubExprNode(ExprChoiceNode())
        case c if classOf[Literal] isAssignableFrom c => LiteralNode("")
        case c => throw new Exception(s"Unexpected parameter type in createFromExpr: $c")
      }
      val result = VariableNode(exprName, innerNodes.toList)
      innerNodes.foreach(_.parent = Some(result))
      result
    }
  }

  case class ExprChoiceNode() extends OuterNode {
    override val children: List[OuterNode] = Nil

    override def toHtmlLine: String = BlankExprDropDown().toHtml

    override def toHtmlLineReadOnly: String = toHtmlLine.replace("select", "select readonly disabled")

    override val exprName: String = "ExprChoice"
  }

  abstract class InnerNode extends Node {

  }

  case class SubExprNode(node: OuterNode) extends InnerNode {
    override def toHtmlLine: String = node.toHtmlLineReadOnly

    override def toHtmlLineReadOnly: String = toHtmlLine

    override val children: List[OuterNode] = List(node)
  }

  case class LiteralNode(literalText: String) extends InnerNode {
    override def toHtmlLine: String = {
      s"""<input type='text' value="$literalText" />"""
    }

    override def toHtmlLineReadOnly: String = {
      s"""<input type='text' readonly disabled value="$literalText" />"""
    }

    override val children: List[OuterNode] = Nil

    override def toString: String = {
      s"LiteralNode(${UtilityFunctions.quote(literalText)})"
    }
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
  case class ExpressionEvalTree(expr: Expr, value: Option[Value], env: Option[Env], children: List[EvalTreeNode]) extends EvalTreeNode {
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
        val envText = env.get.map({ case (name, value): (Variable, Value) => s"$name := ${value.toHtml}" }).mkString(", ")
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
        val childWidths = groupedChildren.values.map(group => group.map(_.treeSvgSize._1).sum + (group.length - 1) * GROUP_X_GAP)
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
      BlankExprDropDown().toHtml
    }
  }

//  case class FillableExprNode(exprClass: Class[Expr], args: List[Expr | Literal], override val children: List[EvalTreeNode]) extends EvalTreeNode {
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
