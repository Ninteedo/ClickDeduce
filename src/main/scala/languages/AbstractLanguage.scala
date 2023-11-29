package languages

import scalatags.Text.TypedTag
import scalatags.Text.all.*

import scala.collection.immutable.List

trait AbstractLanguage {
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

  trait Term {
    lazy val toHtml: TypedTag[String] = span(raw(prettyPrint(this)))
  }

  /**
   * An unevaluated expression.
   */
  abstract class Expr extends Term {
    def children: List[Expr] = {
      def getExprFields(e: Expr): List[Expr] = {
        e match {
          case e0: Product =>
            val values = e0.productIterator.toList
            values.collect({ case e: Expr => e.childVersion })
          case _ => Nil
        }
      }

      getExprFields(this)
    }

    /**
     * Gets the child expressions of this expression, excluding any children which are not used in evaluation.
     */
    def getEvalChildren(env: Env): List[Expr] = children

    lazy val childVersion: Expr = this

    def childExprEnvs(env: Env): List[Env] = List.fill(children.length)(env)

    def childExprTypeEnvs(tenv: TypeEnv): List[TypeEnv] = List.fill(children.length)(tenv)
  }

  case class MissingExpr() extends Expr

  case class ExprPlaceholder(content: String) extends Expr

  /**
   * A value resulting from an expression being evaluated.
   */
  abstract class Value extends Term {
    override lazy val toHtml: TypedTag[String] = span(cls := "tooltip", valueText, div(cls := "tooltiptext", tooltipText))

    lazy val tooltipText: String = toString + ": " + typ.toString

    lazy val valueText: String = prettyPrint(this) + ": " + prettyPrint(typ)

    val typ: Type

    val isError: Boolean = false
  }


  /**
   * The type of a value.
   */
  abstract class Type extends Term {
    override lazy val toHtml: TypedTag[String] = span(cls := "tooltip", valueText, div(cls := "tooltiptext", tooltipText))

    lazy val tooltipText: String = toString

    lazy val valueText: String = prettyPrint(this)

    val isError: Boolean = false
  }

  case class UnknownType() extends Type {

  }

  trait TermError extends Term {
    val message: String = "Error"
  }

  /**
   * An error resulting from an expression being evaluated.
   */
  abstract class EvalError extends Value, TermError {
    override lazy val toHtml: TypedTag[String] = span(cls := "tooltip", valueText, div(cls := "tooltiptext", tooltipText), cls := "error-origin")

    override lazy val tooltipText: String = message

    override lazy val valueText: String = "?"

    override val isError: Boolean = true
  }

  /**
   * An error resulting from an expression being type checked.
   */
  abstract class TypeError extends Type, TermError {
    override lazy val toHtml: TypedTag[String] = span(
      cls := "tooltip", valueText, div(cls := "tooltiptext", tooltipText), cls := "error-origin"
    )

    override lazy val tooltipText: String = message

    override lazy val valueText: String = "?"

    override val isError: Boolean = true
  }

  abstract class Literal extends Term {
    val value: Any

    override lazy val toHtml: TypedTag[String] = p(value.toString)

    override lazy val toString: String = value.toString
  }

  object Literal {
    def fromString(s: String): Literal = {
      if (List("true", "false").contains(s.toLowerCase)) {
        LiteralBool(s.toBoolean)
      } else if (s.startsWith("\"") && s.endsWith("\"")) {
        LiteralString(s.substring(1, s.length - 1))
      } else if ("-?\\d+".r.matches(s)) {
        LiteralInt(BigInt(s))
      } else {
        LiteralAny(s)
      }
    }
  }

  case class LiteralInt(value: BigInt) extends Literal

  case class LiteralBool(value: Boolean) extends Literal

  case class LiteralString(value: String) extends Literal {
//    override lazy val toHtml: TypedTag[String] = p(s""""$value"""")

    override lazy val toString: String = s""""$value""""
  }

  case class LiteralAny(value: String) extends Literal {
    override lazy val toString: String = value
  }

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
  def prettyPrint(e: Expr): String = e match {
    case ExprPlaceholder(content) => content
    case x => x.toHtml.toString
  }

  /**
   * Function to create a human-readable string representation of a `Type`.
   *
   * @param t The `Type` to be pretty printed.
   * @return A `String` representing the pretty printed type.
   */
  def prettyPrint(t: Type): String = t match {
    case x: TypeError => x.message
    case x: UnknownType => "Unknown"
    case x => throw new NotImplementedError(s"prettyPrint($x)")
  }

  /**
   * Function to create a human-readable string representation of a `Value`.
   *
   * @param v The `Value` to be pretty printed.
   * @return A `String` representing the pretty printed value.
   */
  def prettyPrint(v: Value): String = v match {
    case x => x.toHtml.toString
  }

  def prettyPrint(term: Term): String = {
    term match {
      case e: Expr => prettyPrint(e)
      case t: Type => prettyPrint(t)
      case v: Value => prettyPrint(v)
      case _ => "Unknown Term"
    }
  }

  def envToTypeEnv(env: Env): TypeEnv = env.map((k: String, v: Value) => (k, v.typ))
}
