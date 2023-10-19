package languages

import app.FontWidthCalculator

import java.awt.Font
import scala.reflect.runtime.universe as ru
import scala.util.parsing.combinator.*

/**
 * Parent trait for all languages designed to be loaded in ClickDeduce.
 */
trait ClickDeduceLanguage {
  lang =>

  /**
   * A variable name.
   *
   * Case sensitive.
   */
  type Variable = String

  /**
   * The evaluation environment at a particular point.
   *
   * Contains variables with bound values.
   */
  type Env = Map[Variable, Value]

  /**
   * The type environment at a particular point.
   *
   * Contains variables with bound types.
   */
  type TypeEnv = Map[Variable, Type]

  /**
   * An unevaluated expression.
   */
  abstract class Expr {
    def children: List[Expr] = {
      def getExprFields(e: Expr): List[Expr] = {
        e match {
          case e0: Product =>
            val values = e0.productIterator.toList
            values.collect({ case e: Expr => e })
          case _ => Nil
        }
      }
      getExprFields(this)
    }
  }

  /**
   * A value resulting from an expression being evaluated.
   */
  abstract class Value

  /**
   * An error resulting from an expression being evaluated.
   */
  abstract class EvalError extends Value

  /**
   * The type of a value.
   */
  abstract class Type

  /**
   * An error resulting from an expression being type checked.
   */
  abstract class TypeError extends Type

  /**
   * Function which evaluates an `Expr` to a `Value`, given an environment.
   *
   * @param e   The `Expr` to evaluate.
   * @param env The environment to evaluate the `Expr` in.
   * @return The `Value` resulting from evaluating the `Expr`.
   */
  def eval(e: Expr, env: Env): Value

  /**
   * Function which evaluates an `Expr` to a `Value`, given an empty environment.
   *
   * Equivalent to calling <code>eval(e, Map()).</code>
   *
   * @param e The `Expr` to evaluate.
   * @return The `Value` resulting from evaluating the `Expr`.
   */
  def eval(e: Expr): Value = {
    eval(e, Map())
  }

  /**
   * Function to perform type checking on an `Expr` in the given type environment.
   *
   * @param e    The `Expr` on which type checking needs to be performed.
   * @param tenv The type environment in which type checking is done.
   * @return The `Type` of the expression after type checking.
   */
  def typeOf(e: Expr, tenv: TypeEnv): Type

  /**
   * Overloaded type checking function that performs type checking on an `Expr` in an empty type environment.
   *
   * Equivalent to calling <code>typeCheck(e, Map()).</code>
   *
   * @param e The `Expr` on which type checking needs to be performed.
   * @return The `Type` of the expression after type checking.
   */
  def typeOf(e: Expr): Type = {
    typeOf(e, Map())
  }

  /**
   * Function to create a human-readable string representation of an `Expr`.
   *
   * @param e The `Expr` to be pretty printed.
   * @return A `String` representing the pretty printed expression.
   */
  def prettyPrint(e: Expr): String

  /**
   * Function to create a human-readable string representation of a `Type`.
   *
   * @param t The `Type` to be pretty printed.
   * @return A `String` representing the pretty printed type.
   */
  def prettyPrint(t: Type): String

  /**
   * Function to create a human-readable string representation of a `Value`.
   *
   * @param v The `Value` to be pretty printed.
   * @return A `String` representing the pretty printed value.
   */
  def prettyPrint(v: Value): String

  /**
   * Function to load an `Expr` from a string.
   * Input must be in the format produced by `Expr.toString`
   *
   * @param s The string to be parsed.
   * @return The `Expr` represented by the string, if successful.
   */
  def readExpr(s: String): Option[Expr] = {
    val exprClassList = getClass.getClasses.filter(c => classOf[Expr].isAssignableFrom(c))

    /**
     * Create an `Expr` given its name and a list of arguments.
     *
     * @param name The name of the `Expr` to be created. Must match the name of a class extending `Expr` in the language.
     * @param args The arguments to be passed to the constructor of the `Expr`.
     * @return The `Expr` created, if successful.
     */
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
        case None => None
    }

    object ExprParser extends JavaTokenParsers {
      def expr: Parser[Option[Expr]] = name ~ "(" ~ repsep(arg, "\\s*,\\s*".r) ~ ")" ^^ {
        case name ~ "(" ~ args ~ ")" => makeExpr(name, args)
        case _ => None
      }

      def name: Parser[String] = "[A-Za-z]\\w*".r

      def arg: Parser[Any] = expr | stringLiteral | wholeNumber ^^ (BigInt(_)) | "true" ^^ (_ => true) | "false" ^^ (_ => false)

      def parseExpr(s: String): ParseResult[Option[Expr]] = parseAll(expr, s.strip())
    }

    ExprParser.parseExpr(s) match {
      case ExprParser.Success(matched, _) => matched
      case _ => None
    }
  }

  /**
   * Tree node representing an expression in this language.
   * Can be converted to an SVG.
   * @param expr the expression
   * @param value the value of the expression (optional)
   * @param env the environment in which the expression was evaluated (optional)
   * @param children the child nodes of the expression
   */
  class ExpressionEvalTree(val expr: Expr, val value: Option[Value], val env: Option[Env], val children: List[ExpressionEvalTree]) {

    private val XMLNS = "http://www.w3.org/2000/svg"
    private val FONT_NAME = "Courier New"
    private val FONT_SIZE = 16
    private val style = s"""line {stroke: black; stroke-width: 2; transform: translate(0px, -5px);}
      text {font-family: $FONT_NAME; font-size: ${FONT_SIZE}px; dominant-baseline: hanging}"""

    val HEIGHT_PER_ROW = 20
    val FONT = new Font(FONT_NAME, Font.PLAIN, FONT_SIZE)
    val GROUP_X_GAP = 20

    /**
     * Convert this expression tree to a full SVG.
     *
     * @return the SVG string
     */
    lazy val toSvg: String = {
      val svg = new StringBuilder()
      svg.append(s"""<svg xmlns="$XMLNS" width="${size._1 + 15}" height="${size._2 + 20}">""")
      svg.append(s"""<style type="text/css">$style</style>""")
      svg.append(s"""<g transform="translate(5, ${size._2 - HEIGHT_PER_ROW + 8})">""")
      svg.append(toSvgGroup)
      svg.append("</g>")
      svg.append("</svg>")
      svg.toString
    }

    /**
     * Convert this expression tree to an SVG group.
     */
    lazy val toSvgGroup: String = {
      def createGroup(content: String, translateAmount: (Float, Float) = (0, 0)) = {
        s"""<g transform="translate$translateAmount">$content</g>"""
      }

      val totalWidth = exprTextWidth
      val halfWidth = totalWidth / 2

      val childGroup = if (children.nonEmpty) {
        var currWidth = 0f
        val childGroups = for {i <- children.indices} yield {
          val child = children(i)
          val childSvg = child.toSvgGroup
          val x_translate = currWidth
          currWidth += child.size._1 + GROUP_X_GAP
          createGroup(childSvg, (x_translate, 0))
        }
        createGroup(childGroups.mkString(""), (0, -HEIGHT_PER_ROW))
      } else {
        ""
      }

      val textBlock = s"""<text>$exprText</text>"""
      val line = s"""<line x1="0" x2="$totalWidth" y1="0" y2="0" />"""

      val thisGroup = createGroup(line + textBlock + childGroup)
      val svg = new StringBuilder()
      svg.append(thisGroup)
      svg.toString()
    }

    /**
     * Convert the base of this expression tree to a string.
     * Includes HTML entities for certain Unicode characters.
     * @return the string representation of the expression
     */
    lazy val exprText: String = {
      val turnstile = "&#x22a2;"
      val arrow = "&DoubleDownArrow;"

      val sb = new StringBuilder()
      if (env.isDefined) {
        val envText = env.get.map({ case (name, value): (Variable, Value) => s"$name := ${prettyPrint(value)}" }).mkString(", ")
        sb.append(s"[$envText], ")
      }
      sb.append(lang.prettyPrint(expr))
      if (value.isDefined) {
        sb.append(s" $arrow ${lang.prettyPrint(value.get)}")
      }
      sb.toString
    }

    /**
     * Calculate the width of the text representation of the text of this expression.
     * @return the width of the text in pixels
     */
    lazy val exprTextWidth: Float = {
      FontWidthCalculator.calculateWidth(exprText, FONT)
    }

    /**
     * Calculate the total size of the SVG for this expression tree.
     *
     * @return the size of the SVG in pixels, (width, height)
     */
    lazy val size: (Float, Float) = {
      val groupedChildren = groupChildrenByLevel
      val height = HEIGHT_PER_ROW * groupedChildren.length
      val width = groupedChildren.map(group => group.map(_.exprTextWidth).sum + (group.length - 1) * GROUP_X_GAP).max
      (width, height)
    }

    /**
     * Group the children of this expression tree by level.
     * @return a list of lists of expression trees, where each list contains all the expression trees at a particular level
     */
    lazy val groupChildrenByLevel: List[List[ExpressionEvalTree]] = {
      var childrenLevels: Map[ExpressionEvalTree, Int] = Map()
      def visit(tree: ExpressionEvalTree, level: Int): Unit = {
        childrenLevels += tree -> level
        tree.children.foreach(visit(_, level + 1))
      }
      visit(this, 0)
      val maxLevel = childrenLevels.values.max
      val groupedChildren = for {i <- 0 to maxLevel} yield {
        childrenLevels.filter({ case (_, level) => level == i }).keys.toList
      }
      groupedChildren.toList
    }
  }

  object ExpressionEvalTree {
    def exprToTree(e0: Expr): ExpressionEvalTree = {
      val childTrees = e0.children.map(exprToTree)
      val valueResult: Option[Value] = Some(lang.eval(e0))
      ExpressionEvalTree(e0, valueResult, None, childTrees)
    }
  }
}
