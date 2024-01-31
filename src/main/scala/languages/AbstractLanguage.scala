package languages

import scalatags.Text.TypedTag
import scalatags.Text.all.*

import scala.collection.immutable.List

trait AbstractLanguage {

  /** A variable name.
    *
    * Case sensitive.
    */
  type Variable = String

  /** The evaluation environment at a particular point.
    *
    * Contains variables with bound values.
    */
  type Env = Map[Variable, Value]

  /** The type environment at a particular point.
    *
    * Contains variables with bound types.
    */
  type TypeEnv = Map[Variable, Type]

  trait Term {
    lazy val toHtml: TypedTag[String] = span(raw(prettyPrint))

    def getChildrenBase(env: Env = Map()): List[(Term, Env)] = Nil

    def getChildrenEval(env: Env = Map()): List[(Term, Env)] = Nil

    def getChildrenTypeCheck(tEnv: TypeEnv = Map()): List[(Term, TypeEnv)] = Nil

    def isPlaceholder: Boolean = false

    def prettyPrint: String = toHtml.toString
  }

  /** An unevaluated expression.
    */
  abstract class Expr extends Term {
    private def getExprFields(e: Expr): List[Expr] = {
      e match {
        case e0: Product =>
          val values = e0.productIterator.toList
          values.collect({ case e: Expr => e })
        case _ => Nil
      }
    }

    override def getChildrenBase(env: Env = Map()): List[(Term, Env)] =
      getExprFields(this).zip(LazyList.continually(env))

    override def getChildrenEval(env: Env = Map()): List[(Term, Env)] =
      getExprFields(this).zip(LazyList.continually(env))

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] =
      getExprFields(this).zip(LazyList.continually(tEnv))

    lazy val childVersion: Expr = this

    /** Function which evaluates this `Expr` to a `Value`, given an environment.
      *
      * @param env
      *   The environment to evaluate in.
      * @return
      *   The `Value` resulting from evaluating this.
      */
    final def eval(env: Env = Map()): Value = {
      try {
        evalInner(env)
      } catch {
        case e: StackOverflowError => stackOverflowEvalError
      }
    }

    protected def evalInner(env: Env): Value = UnexpectedExpr(toString)

    /** Function to perform type checking on this `Expr` in the given type environment.
      *
      * @param tEnv
      *   The type environment in which type checking is done.
      * @return
      *   The `Type` of this expression after type checking.
      */
    final def typeCheck(tEnv: TypeEnv = Map()): Type = {
      try {
        typeCheckInner(tEnv)
      } catch {
        case e: StackOverflowError => stackOverflowTypeError
      }
    }

    protected def typeCheckInner(tEnv: TypeEnv): Type = UnexpectedExprType(toString)
  }

  case class MissingExpr() extends Expr

  case class ExprPlaceholder(content: String) extends Expr {
    override def prettyPrint: String = content
  }

  /** A value resulting from an expression being evaluated.
    */
  abstract class Value extends Term {
    override lazy val toHtml: TypedTag[String] =
      span(cls := "tooltip", valueText(display := "inline"), div(cls := "tooltip-text", tooltipText))

    lazy val tooltipText: String = toString + ": " + typ.toString

    lazy val valueText: TypedTag[String] = div(prettyPrint + ": " + typ.prettyPrint)

    val typ: Type

    val isError: Boolean = false

    override def prettyPrint: String = toHtml.toString
  }

  case class ValuePlaceholder(content: String) extends Value {
    override def prettyPrint: String = content

    override val typ: Type = TypePlaceholder(content)
  }

  /** The type of a value.
    */
  abstract class Type extends Term {
    override lazy val toHtml: TypedTag[String] =
      span(cls := "tooltip", valueText(display := "inline"), div(cls := "tooltip-text", tooltipText))

    lazy val tooltipText: String = toString

    lazy val valueText: TypedTag[String] = div(prettyPrint)

    val isError: Boolean = false

    def typeCheck(tEnv: TypeEnv): Type = this
  }

  case class UnknownType() extends Type {
    override def prettyPrint: String = "Unknown"
  }

  case class TypePlaceholder(content: String) extends Type {
    override def prettyPrint: String = content
  }

  trait TermError extends Term {
    val message: String = "Error"
  }

  /** An error resulting from an expression being evaluated.
    */
  abstract class EvalError extends Value, TermError {
    override lazy val toHtml: TypedTag[String] =
      span(cls := "tooltip", valueText, div(cls := "tooltip-text", tooltipText), cls := "error-origin")

    override lazy val tooltipText: String = message

    override lazy val valueText: TypedTag[String] = div("?")

    override val isError: Boolean = true

    override def prettyPrint: String = message

    override def toString: String = s"${getClass.getSimpleName.stripSuffix("$")}($message)"
  }

  /** An error that occurs due to attempting to process an unknown `Expr`.
    *
    * @param message
    *   The error message.
    */
  case class UnexpectedExpr(override val message: String) extends EvalError {
    override val typ: Type = UnexpectedExprType(message)
  }

  case class EvalException(override val message: String) extends EvalError {
    override val typ: Type = TypeException(message)
  }

  private val stackOverflowEvalError: EvalError = EvalException("Stack overflow")
  private val stackOverflowTypeError: TypeError = TypeException("Stack overflow")

  /** An error resulting from an expression being type checked.
    */
  abstract class TypeError extends Type, TermError {
    override lazy val toHtml: TypedTag[String] = span(
      cls := "tooltip",
      valueText(display := "inline"),
      div(cls := "tooltip-text", tooltipText),
      cls := "error-origin"
    )

    override lazy val tooltipText: String = message

    override lazy val valueText: TypedTag[String] = div("?")

    override val isError: Boolean = true

    override def prettyPrint: String = message

    override def toString: String = s"${getClass.getSimpleName.stripSuffix("$")}($message)"
  }

  /** An error that occurs due to attempting to process an unknown `Expr`.
    *
    * @param message
    *   The error message.
    */
  case class UnexpectedExprType(override val message: String) extends TypeError

  case class TypeException(override val message: String) extends TypeError

  abstract class Literal extends Term {
    val value: Any

    override lazy val toHtml: TypedTag[String] = p(value.toString)

    override lazy val toString: String = value.toString

    override def prettyPrint: String = toString
  }

  object Literal {
    def fromString(s: String): Literal = {
      if (List("true", "false").contains(s.toLowerCase)) {
        LiteralBool(s.toBoolean)
      } else if (s.startsWith("\"") && s.endsWith("\"") && s.length > 1) {
        LiteralString(s.substring(1, s.length - 1))
      } else if ("-?\\d+".r.matches(s)) {
        LiteralInt(BigInt(s))
      } else if ("[A-Za-z_$][\\w_$]*".r.matches(s)) {
        LiteralIdentifier(s)
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

  case class LiteralIdentifier(value: String) extends Literal {
    override lazy val toString: String = value
  }

  case class LiteralAny(value: String) extends Literal {
    override lazy val toString: String = value
  }

  def envToTypeEnv(env: Env): TypeEnv = env.map((k: String, v: Value) => (k, v.typ))

  enum DisplayMode:
    case Edit, Evaluation, TypeCheck

  object DisplayMode {
    def fromString(s: String): DisplayMode = s match {
      case "edit"       => Edit
      case "eval"       => Evaluation
      case "type-check" => TypeCheck
      case _            => throw new IllegalArgumentException(s"Unknown display mode: $s")
    }
  }
}
