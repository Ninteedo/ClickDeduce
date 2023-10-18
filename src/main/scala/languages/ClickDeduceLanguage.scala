package languages

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
  abstract class Expr

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
   * Function to get the children of an `Expr` in the given environment.
   *
   * @param e   The `Expr` whose children are to be returned.
   * @param env The environment in which the `Expr` is to be evaluated.
   * @return The `Expr`s which are the children of `e`.
   */
  def childrenOf(e: Expr, env: Env): List[Expr]

  /**
   * Overloaded function to get the children of an `Expr` in an empty environment.
   *
   * @param e The `Expr` whose children are to be returned.
   * @return The `Expr`s which are the children of `e`.
   */
  def childrenOf(e: Expr): List[Expr] = {
    childrenOf(e, Map())
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

  class ExpressionEvalTree(val language: ClickDeduceLanguage, val expr: language.Expr, val value: Option[language.Value], val env: Option[language.Env], val children: List[ExpressionEvalTree]) {

    private val XMLNS = "http://www.w3.org/2000/svg"
    private val style = "line {stroke: black; stroke-width: 2; transform: translate(0px, -5px);}" +
      " text {font-family: sans-serif; font-size: 12px; dominant-baseline: hanging}"

    /**
     * Convert this expression tree to a full SVG.
     *
     * @return the SVG string
     */
    def toSvg: String = {
      val svg = new StringBuilder()
      svg.append(s"""<svg xmlns="$XMLNS" width="${size._1}" height="${size._2}" transform="translate(0 50)">""")
      svg.append(s"""<style type="text/css">$style</style>""")
      svg.append(toSvgGroup)
      svg.append("</svg>")
      svg.toString
    }

    /**
     * Convert this expression tree to an SVG group.
     */
    def toSvgGroup: String = {
      def createGroup(content: String, translateAmount: (Float, Float) = (0, 0)) = {
        s"""<g transform="translate$translateAmount">$content</g>"""
      }

      val totalWidth = size._1
      val halfWidth = totalWidth / 2

      val turnstile = "&#x22a2;"
      val arrow = "&DoubleDownArrow;"

      val line = s"""<line x1="-$halfWidth" x2="$halfWidth" y1="0" y2="0" />"""
      val exprText = new StringBuilder()
      if (env.isDefined) {
        val envText = env.get.map({ case (name, value): (Variable, Value) => s"$name := ${prettyPrint(value)}" }).mkString(", ")
        exprText.append(s"[$envText], ")
      }
      exprText.append(language.prettyPrint(expr))
      if (value.isDefined) {
        exprText.append(s" $arrow ${language.prettyPrint(value.get)}")
      }
      val textBlock = s"""<text>${exprText.toString()}</text>"""

      val thisGroup = createGroup(line + textBlock)
      val svg = new StringBuilder()
      svg.append(thisGroup)
      println(children)
      if (children.nonEmpty) {
        val childGroups = for {i <- children.indices} yield {
          val child = children(i)
          val childSvg = child.toSvgGroup
          createGroup(childSvg, (totalWidth * (i - children.length / 2), 20))
        }
        val childGroup = createGroup(childGroups.mkString(""), (0, -20))
        svg.append(childGroup)
      }
      svg.toString()
    }

    /**
     * Calculate the total size of the SVG for this expression tree.
     *
     * @return the size of the SVG in pixels, (width, height)
     */
    def size: (Float, Float) = {
      // TODO: calculate SVG size using font metrics
      val width = 100
      val height = 100
      (width, height)
    }
  }

  object ExpressionEvalTree {
    def exprToTree(e0: Expr): ExpressionEvalTree = {
      def getExprFields(e: Expr): List[Expr] = {
        e match {
          case e0: Product =>
            val values = e0.productIterator.toList
            values.collect({ case e: Expr => e })
          case _ => Nil
        }
      }


      val childExprs = getExprFields(e0)
      val childTrees = childExprs.map(exprToTree)
      val valueResult: Option[Value] = None
      ExpressionEvalTree(lang, e0, Some(lang.eval(e0)), None, childTrees)
    }
  }
}
