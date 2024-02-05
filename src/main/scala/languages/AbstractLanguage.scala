package languages

import convertors.ClassDict
import scalatags.Text.TypedTag
import scalatags.Text.all.*

import scala.collection.immutable.List

trait AbstractLanguage {
  lang =>

  /** A variable name.
    *
    * Case sensitive.
    */
  type Variable = String

  /** The evaluation environment at a particular point.
   *
   * Contains variables with bound values.
   */
  case class Env[T](protected val env: Map[Variable, T] = Map()) {
    def get(key: Variable): Option[T] = env.get(key)

    def set(key: Variable, value: T): Env[T] = new Env(env + (key -> value))

    def +(key: Variable, value: T): Env[T] = set(key, value)

    def +(kv: (Variable, T)): Env[T] = set(kv._1, kv._2)

    def ++(other: Env[T]): Env[T] = new Env(env ++ other.env)

    def getOrElse(key: Variable, default: => T): T = env.getOrElse(key, default)

    def isEmpty: Boolean = env.isEmpty

    def nonEmpty: Boolean = env.nonEmpty

    def map[U](f: ((Variable, T)) => U): Iterable[U] = env.map(f)

    def mapToEnv[U](f: ((Variable, T)) => (Variable, U)): Env[U] = new Env(env.map(f))

    def keys: Iterable[Variable] = env.keys
  }

  object Env {
    def apply[T](items: (Variable, T)*): Env[T] = new Env[T](Map(items: _*))
  }

  /** The evaluation environment at a particular point.
   *
   * Contains variables with bound values.
   */
  type ValueEnv = Env[Value]

  /** The type environment at a particular point.
   *
   * Contains variables with bound types.
   */
  type TypeEnv = Env[Type]

  object ValueEnv {
    val empty = new ValueEnv(Map())
  }

  object TypeEnv {
    val empty = new TypeEnv(Map())
  }

  trait Term {
    lazy val toHtml: TypedTag[String] = span(raw(prettyPrint))

    def getChildrenBase(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] = Nil

    def getChildrenEval(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] = Nil

    def getChildrenTypeCheck(tEnv: TypeEnv = TypeEnv.empty): List[(Term, TypeEnv)] = Nil

    def isPlaceholder: Boolean = false

    def prettyPrint: String = toHtml.toString

    final def prettyPrintBracketed: String = if (needsBrackets) s"($prettyPrint)" else prettyPrint

    val needsBrackets: Boolean = true
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

    override def getChildrenBase(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] =
      getExprFields(this).zip(LazyList.continually(env))

    override def getChildrenEval(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] =
      getExprFields(this).zip(LazyList.continually(env))

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] =
      getExprFields(this).zip(LazyList.continually(tEnv))

    lazy val childVersion: Expr = this

    final def eval(): Value = eval(ValueEnv.empty)

    /** Function which evaluates this `Expr` to a `Value`, given an environment.
      *
      * @param env
      *   The environment to evaluate in.
      * @return
      *   The `Value` resulting from evaluating this.
      */
    final def eval(env: ValueEnv): Value = {
      try {
        evalInner(env)
      } catch {
        case e: StackOverflowError => stackOverflowEvalError
      }
    }

    protected def evalInner(env: ValueEnv): Value = UnexpectedExpr(toString)

    final def typeCheck(): Type = typeCheck(TypeEnv.empty)

    /** Function to perform type checking on this `Expr` in the given type environment.
      *
      * @param tEnv
      *   The type environment in which type checking is done.
      * @return
      *   The `Type` of this expression after type checking.
      */
    final def typeCheck(tEnv: TypeEnv): Type = {
      try {
        typeCheckInner(tEnv)
      } catch {
        case e: StackOverflowError => stackOverflowTypeError
      }
    }

    protected def typeCheckInner(tEnv: TypeEnv): Type = UnexpectedExprType(toString)
  }

  case class MissingExpr() extends Expr

  case class ExprPlaceholder(content: String, override val needsBrackets: Boolean = false) extends Expr {
    override def prettyPrint: String = content
  }

  object ExprPlaceholder {
    def apply(expr: Expr): ExprPlaceholder = ExprPlaceholder(expr.toHtml.toString, expr.needsBrackets)
  }

  /** A value resulting from an expression being evaluated.
    */
  abstract class Value extends Term {
    override lazy val toHtml: TypedTag[String] =
      span(cls := ClassDict.TOOLTIP, valueText, div(cls := ClassDict.TOOLTIP_TEXT, tooltipText))

    lazy val tooltipText: String = toString + ": " + typ.toString

    lazy val valueText: TypedTag[String] = {
      val constructor = getClass.getConstructors.head
      val arguments = this match {
        case v0: Product =>
          v0.productIterator.toList.collect({
            case v: Value   => ValuePlaceholder(v.valueText.toString, v.needsBrackets)
            case t: Type    => TypePlaceholder(t.valueText.toString, t.needsBrackets)
            case e: Expr    => ExprPlaceholder(e)
            case s: String  => s
            case l: Literal => l
            case other      => other
          })
      }
      val valueInstance = constructor.newInstance(lang +: arguments: _*).asInstanceOf[Value]
      div(
        div(raw(valueInstance.prettyPrint), cls := ClassDict.VALUE),
        span(": "),
        div(typ.valueText, cls := ClassDict.VALUE_TYPE)
      )
    }

    val typ: Type

    val isError: Boolean = false

    override def prettyPrint: String = toHtml.toString
  }

  case class ValuePlaceholder(content: String, override val needsBrackets: Boolean = false) extends Value {
    override def prettyPrint: String = content

    override val typ: Type = TypePlaceholder(content, needsBrackets)
  }

  object ValuePlaceholder {
    def apply(value: Value): ValuePlaceholder = ValuePlaceholder(value.toHtml.toString, value.needsBrackets)
  }

  /** The type of a value.
    */
  abstract class Type extends Term {
    override lazy val toHtml: TypedTag[String] =
      span(cls := ClassDict.TOOLTIP, valueText, div(cls := ClassDict.TOOLTIP_TEXT, tooltipText))

    lazy val tooltipText: String = toString

    lazy val valueText: TypedTag[String] = {
      val constructor = getClass.getConstructors.head
      val arguments = this match {
        case v0: Product =>
          v0.productIterator.toList.collect({
            case v: Value   => ValuePlaceholder(v.valueText.toString, v.needsBrackets)
            case t: Type    => TypePlaceholder(t.valueText.toString, t.needsBrackets)
            case e: Expr    => ExprPlaceholder(e)
            case s: String  => s
            case l: Literal => l
            case other      => other
          })
      }
      val valueInstance = constructor.newInstance(lang +: arguments: _*).asInstanceOf[Type]
      div(raw(valueInstance.prettyPrint), cls := ClassDict.VALUE_TYPE)
    }

    val isError: Boolean = false

    def typeCheck(tEnv: TypeEnv): Type = this
  }

  case class UnknownType() extends Type {
    override def prettyPrint: String = "Unknown"
  }

  case class TypePlaceholder(content: String, override val needsBrackets: Boolean = true) extends Type {
    override def prettyPrint: String = content
  }

  object TypePlaceholder {
    def apply(typ: Type): TypePlaceholder = TypePlaceholder(typ.toHtml.toString, typ.needsBrackets)
  }

  trait TermError extends Term {
    val message: String = "Error"
  }

  /** An error resulting from an expression being evaluated.
    */
  abstract class EvalError extends Value, TermError {
    override lazy val toHtml: TypedTag[String] =
      span(
        cls := ClassDict.TOOLTIP,
        valueText,
        div(cls := ClassDict.TOOLTIP_TEXT, tooltipText),
        cls := ClassDict.ERROR_ORIGIN
      )

    override lazy val tooltipText: String = message

    override lazy val valueText: TypedTag[String] = div("!", cls := ClassDict.ERROR_ORIGIN)

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
      cls := ClassDict.TOOLTIP,
      valueText,
      div(cls := ClassDict.TOOLTIP_TEXT, tooltipText),
      cls := ClassDict.ERROR_ORIGIN
    )

    override lazy val tooltipText: String = message

    override lazy val valueText: TypedTag[String] = div("!", cls := ClassDict.ERROR_ORIGIN)

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
    def fromString(s: String): Literal = if (List("true", "false").contains(s.toLowerCase)) {
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

  def envToTypeEnv(env: ValueEnv): TypeEnv = env.mapToEnv((k: String, v: Value) => (k, v.typ))
}
