package languages

import app.ClickDeduceException
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
  case class Env[T](env: Map[Variable, T] = Map()) {
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
    val name: String = toString.takeWhile(_ != '(')

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
      val valueInstance = getValueBuilder(name) match {
        case Some(builder) =>
          builder.apply(arguments) match {
            case Some(v) => v
            case None    => throw InvalidValueBuilderArgs(name, arguments)
          }
        case None => throw UnknownValueBuilder(name)
      }
      div(
        div(raw(valueInstance.prettyPrint), cls := ClassDict.VALUE),
        if (valueTextShowType) List(span(": "), div(typ.valueText, cls := ClassDict.VALUE_TYPE)) else div()
      )
    }

    val typ: Type

    val isError: Boolean = false

    def valueTextShowType: Boolean = true

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
      val valueInstance = getTypeBuilder(name) match {
        case Some(builder) =>
          builder.apply(arguments) match {
            case Some(t) => t
            case None    => throw InvalidTypeBuilderArgs(name, arguments)
          }
        case None => throw UnknownTypeBuilder(name)
      }
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

  case class TypeContainer(typ: Type) extends Type {
    override def prettyPrint: String = typ.prettyPrint

    override def typeCheck(tEnv: TypeEnv): Type = typ
  }

  case class TypeValueContainer(typ: Type) extends Value {
    override def prettyPrint: String = typ.prettyPrint

    override def valueTextShowType: Boolean = false
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

  def typeVariableEnv(env: ValueEnv | TypeEnv): TypeEnv = {
    Env(env.env.collect({
      case (k, TypeValueContainer(t)) => (k, t)
      case (k, TypeContainer(t))      => (k, t)
    }))
  }

  // new class lists

  type ExprBuilder = List[Any] => Option[Expr]

  private var exprBuilders: Map[String, ExprBuilder] = Map()

  private var exprBuilderNamesList: List[String] = List()

  protected def addExprBuilder(name: String, builder: ExprBuilder): Unit = {
    exprBuilders += (name -> builder)
    exprBuilderNamesList = exprBuilderNamesList :+ name
  }

  def exprBuilderNames: List[String] = exprBuilderNamesList

  type TypeBuilder = List[Any] => Option[Type]

  private var typeBuilders: Map[String, TypeBuilder] = Map()

  private var typeBuilderNamesList: List[String] = List()

  protected def addTypeBuilder(name: String, builder: TypeBuilder): Unit = {
    typeBuilders += (name -> builder)
    typeBuilderNamesList = typeBuilderNamesList :+ name
  }

  def typeBuilderNames: List[String] = typeBuilderNamesList

  type ValueBuilder = List[Any] => Option[Value]

  private var valueBuilders: Map[String, ValueBuilder] = Map()

  protected def addValueBuilder(name: String, builder: ValueBuilder): Unit = {
    valueBuilders += (name -> builder)
  }

  def getExprBuilder(name: String): Option[ExprBuilder] = exprBuilders.get(name)

  def buildExpr(name: String, args: List[Any]): Option[Expr] = getExprBuilder(name).flatMap(_.apply(args))

  def getTypeBuilder(name: String): Option[TypeBuilder] = typeBuilders.get(name)

  def buildType(name: String, args: List[Any]): Option[Type] = getTypeBuilder(name).flatMap(_.apply(args))

  def getValueBuilder(name: String): Option[ValueBuilder] = valueBuilders.get(name)

  def buildValue(name: String, args: List[Any]): Option[Value] = getValueBuilder(name).flatMap(_.apply(args))

  case class UnknownExprBuilder(name: String) extends ClickDeduceException(s"Unknown expression builder: $name")

  case class InvalidExprBuilderArgs(name: String, args: List[Any])
      extends ClickDeduceException(s"Invalid arguments for expression builder: $name, $args")

  case class UnknownTypeBuilder(name: String) extends ClickDeduceException(s"Unknown type builder: $name")

  case class InvalidTypeBuilderArgs(name: String, args: List[Any])
      extends ClickDeduceException(s"Invalid arguments for type builder: $name, $args")

  case class UnknownValueBuilder(name: String) extends ClickDeduceException(s"Unknown value builder: $name")

  case class InvalidValueBuilderArgs(name: String, args: List[Any])
      extends ClickDeduceException(s"Invalid arguments for value builder: $name, $args")

  addExprBuilder(
    "ExprPlaceholder",
    {
      case List(s: String) => Some(ExprPlaceholder(s))
      case Nil             => Some(ExprPlaceholder(""))
      case _               => None
    }
  )

  addTypeBuilder(
    "TypePlaceholder",
    {
      case List(s: String) => Some(TypePlaceholder(s))
      case Nil             => Some(TypePlaceholder(""))
      case _               => None
    }
  )

  addValueBuilder(
    "ValuePlaceholder",
    {
      case List(s: String) => Some(ValuePlaceholder(s))
      case Nil             => Some(ValuePlaceholder(""))
      case _               => None
    }
  )

  addValueBuilder(
    "TypeValueContainer",
    {
      case List(t: Type) => Some(TypeValueContainer(t))
      case Nil           => Some(TypeValueContainer(TypePlaceholder("")))
      case _             => None
    }
  )

  addTypeBuilder(
    "TypeContainer",
    {
      case List(t: Type) => Some(TypeContainer(t))
      case Nil           => Some(TypeContainer(TypePlaceholder("")))
      case _             => None
    }
  )

  addTypeBuilder(
    "UnknownType",
    {
      case Nil => Some(UnknownType())
      case _   => None
    }
  )

  val defaultLiteral: Literal = Literal.fromString("")
}
