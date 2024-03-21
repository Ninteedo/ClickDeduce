package languages

import app.ClickDeduceException
import convertors.*
import scalatags.Text.TypedTag
import scalatags.Text.all.*

import scala.collection.immutable.List

trait AbstractLanguage {
  lang =>

  // <editor-fold desc="Environments">

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

  def envToTypeEnv(env: ValueEnv): TypeEnv = env.mapToEnv((k: String, v: Value) => (k, v.typ))

  def typeVariableEnv(env: ValueEnv | TypeEnv): TypeEnv = {
    Env(env.env.collect({
      case (k, TypeValueContainer(t)) => (k, t)
      case (k, TypeContainer(t))      => (k, t)
    }))
  }

  // </editor-fold>

  trait Term {
    val name: String = toString.takeWhile(_ != '(')

    def getChildrenBase(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] = Nil

    def getChildrenEval(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] = Nil

    def getChildrenTypeCheck(tEnv: TypeEnv = TypeEnv.empty): List[(Term, TypeEnv)] = Nil

    def isPlaceholder: Boolean = false

    val needsBrackets: Boolean = true

    def toText: ConvertableText

    lazy val toHtml: TypedTag[String] = toText.asHtml

    final def prettyPrint: String = toText.asPlainText

    final def prettyPrintBracketed: String = if (needsBrackets) s"($prettyPrint)" else prettyPrint

    final def toTextBracketed: ConvertableText = if (needsBrackets) BracketedElement(toText) else toText
  }

  // <editor-fold desc="Expressions">

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

    private def defaultChildren[EnvContents](env: Env[EnvContents]): List[(Term, Env[EnvContents])] =
      getExprFields(this).zip(LazyList.continually(env))

    override def getChildrenBase(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] = defaultChildren(env)

    override def getChildrenEval(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] = defaultChildren(env)

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = defaultChildren(tEnv)

    /** Perform evaluation using an empty environment. */
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

    /** Function to perform evaluation on this `Expr` in the given environment.
      *
      * This function must be implemented by subclasses.
      * @param env
      *   The environment to evaluate in.
      * @return
      *   The `Value` resulting from evaluating this.
      */
    protected def evalInner(env: ValueEnv): Value

    /** Perform type-checking using an empty environment. */
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

    /** Function to perform type checking on this `Expr` in the given type environment.
      *
      * This function must be implemented by subclasses.
      * @param tEnv
      *   The type environment in which type checking is done.
      * @return
      *   The `Type` of this expression after type checking.
      */
    protected def typeCheckInner(tEnv: TypeEnv): Type
  }

  abstract class NotImplementedExpr extends Expr {
    override def evalInner(env: ValueEnv): Value = UnexpectedExpr(toString)

    override def typeCheckInner(tEnv: TypeEnv): Type = UnexpectedExprType(toString)
  }

  case class MissingExpr() extends NotImplementedExpr {
    override def toText: ConvertableText = TextElement("Missing")
  }

  case class ExprPlaceholder(content: ConvertableText, override val needsBrackets: Boolean = false)
      extends NotImplementedExpr {
    override def toText: ConvertableText = content
  }

  object ExprPlaceholder {
    def apply(expr: Expr): ExprPlaceholder = ExprPlaceholder(expr.toText, expr.needsBrackets)
  }

  // </editor-fold>

  // <editor-fold desc="Values">

  /** A value resulting from an expression being evaluated. */
  abstract class Value extends Term {
    override lazy val toHtml: TypedTag[String] =
      span(cls := ClassDict.TOOLTIP, valueText, div(cls := ClassDict.TOOLTIP_TEXT, tooltipText))

    lazy val tooltipText: String = toString + ": " + typ.toString

    lazy val valueText: TypedTag[String] = div(
      div(toText.asHtml, cls := ClassDict.VALUE),
      if (valueTextShowType) List(span(": "), div(typ.valueText, cls := ClassDict.VALUE_TYPE)) else div()
    )

    /** The type of this value. */
    val typ: Type

    /** Whether this value represents an evaluation error. */
    val isError: Boolean = false

    def valueTextShowType: Boolean = true
  }

  case class ValuePlaceholder(content: ConvertableText, override val needsBrackets: Boolean = false) extends Value {
    override val typ: Type = TypePlaceholder(content, needsBrackets)

    override def toText: ConvertableText = content
  }

  object ValuePlaceholder {
    def apply(value: Value): ValuePlaceholder = ValuePlaceholder(value.toText, value.needsBrackets)
  }

  // </editor-fold>

  // <editor-fold desc="Types">

  /** The type of a value. Can also appear in expressions.
    */
  abstract class Type extends Term {
    override lazy val toHtml: TypedTag[String] =
      span(cls := ClassDict.TOOLTIP, valueText, div(cls := ClassDict.TOOLTIP_TEXT, tooltipText))

    lazy val tooltipText: String = toString

    lazy val valueText: TypedTag[String] = div(toText.asHtml, cls := ClassDict.VALUE_TYPE)

    val isError: Boolean = false

    def typeCheck(tEnv: TypeEnv): Type = this
  }

  case class UnknownType() extends Type {
    override def toText: ConvertableText = TextElement("Unknown")

    override val needsBrackets: Boolean = false
  }

  case class TypePlaceholder(content: ConvertableText, override val needsBrackets: Boolean = true) extends Type {
    override def toText: ConvertableText = content
  }

  object TypePlaceholder {
    def apply(typ: Type): TypePlaceholder = TypePlaceholder(typ.toText, typ.needsBrackets)
  }

  case class TypeContainer(typ: Type) extends Type {
    override def typeCheck(tEnv: TypeEnv): Type = typ

    override def toText: ConvertableText = typ.toText
  }

  case class TypeValueContainer(typ: Type) extends Value {
    override def valueTextShowType: Boolean = false

    override def toText: ConvertableText = typ.toText
  }

  // </editor-fold>

  // <editor-fold desc="Term Errors">

  trait TermError extends Term {
    val message: String = "Error"
  }

  /** An error resulting from an expression being evaluated.
    */
  abstract class EvalError extends Value, TermError {
    override lazy val tooltipText: String = message

    override lazy val valueText: TypedTag[String] = div("error!", cls := ClassDict.ERROR_ORIGIN)

    override val isError: Boolean = true

    override def toText: ConvertableText =
      HtmlElement(span(cls := "error-origin", raw("error!")), TextElement("error!"))

    override val needsBrackets: Boolean = false
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
    override lazy val tooltipText: String = message

    override lazy val valueText: TypedTag[String] = div("error!", cls := ClassDict.ERROR_ORIGIN)

    override val isError: Boolean = true

    override def toText: ConvertableText =
      HtmlElement(span(cls := "error-origin", raw("error!")), TextElement("error!"))

    override val needsBrackets: Boolean = false
  }

  /** An error that occurs due to attempting to process an unknown `Expr`.
    *
    * @param message
    *   The error message.
    */
  case class UnexpectedExprType(override val message: String) extends TypeError

  case class TypeException(override val message: String) extends TypeError

  // </editor-fold>

  // <editor-fold desc="Literals">

  /** A literal term. Entered as a string. */
  abstract class Literal extends Term {
    val value: Any

    override lazy val toHtml: TypedTag[String] = p(value.toString)

    override lazy val toString: String = value.toString
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

  case class LiteralInt(value: BigInt) extends Literal {
    override def toText: ConvertableText = MathElement(value.toString)
  }

  case class LiteralBool(value: Boolean) extends Literal {
    override def toText: ConvertableText = MathElement(value.toString)
  }

  case class LiteralString(value: String) extends Literal {
    override lazy val toString: String = s""""$value""""

    override def toText: ConvertableText = TextElement(toString)
  }

  case class LiteralIdentifier(value: String) extends Literal {
    override lazy val toString: String = value

    override def toText: ConvertableText = ItalicsElement(TextElement(toString))
  }

  case class LiteralAny(value: String) extends Literal {
    override lazy val toString: String = value

    override def toText: ConvertableText = TextElement(toString)
  }

  // </editor-fold>

  // <editor-fold desc="Builders">

  private type ExprBuilder = List[Any] => Option[Expr]

  private var exprBuilders: Map[String, ExprBuilder] = Map()

  private var exprBuilderNamesList: List[String] = List()

  protected def addExprBuilder(name: String, builder: ExprBuilder, hidden: Boolean = false): Unit = {
    exprBuilders += (name -> builder)
    if (!hidden) {
      exprBuilderNamesList = exprBuilderNamesList :+ name
    }
  }

  def exprBuilderNames: List[String] = exprBuilderNamesList

  private type TypeBuilder = List[Any] => Option[Type]

  private var typeBuilders: Map[String, TypeBuilder] = Map()

  private var typeBuilderNamesList: List[String] = List()

  protected def addTypeBuilder(name: String, builder: TypeBuilder, hidden: Boolean = false): Unit = {
    typeBuilders += (name -> builder)
    if (!hidden) {
      typeBuilderNamesList = typeBuilderNamesList :+ name
    }
  }

  def typeBuilderNames: List[String] = typeBuilderNamesList

  private type ValueBuilder = List[Any] => Option[Value]

  private var valueBuilders: Map[String, ValueBuilder] = Map()

  protected def addValueBuilder(name: String, builder: ValueBuilder): Unit = {
    valueBuilders += (name -> builder)
  }

  def getExprBuilder(name: String): Option[ExprBuilder] = exprBuilders.get(name)

  def buildExpr(name: String, args: List[Any]): Option[Expr] = getExprBuilder(name) match {
    case Some(builder) => builder.apply(args)
    case None          => throw UnknownExprBuilder(name)
  }

  def getTypeBuilder(name: String): Option[TypeBuilder] = typeBuilders.get(name)

  def buildType(name: String, args: List[Any]): Option[Type] = getTypeBuilder(name) match {
    case Some(builder) => builder.apply(args)
    case None          => throw UnknownTypeBuilder(name)
  }

  def getValueBuilder(name: String): Option[ValueBuilder] = valueBuilders.get(name)

  def buildValue(name: String, args: List[Any]): Option[Value] = getValueBuilder(name) match {
    case Some(builder) => builder.apply(args)
    case None          => throw UnknownValueBuilder(name)
  }

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
      case List(s: String) => Some(ExprPlaceholder(TextElement(s)))
      case Nil             => Some(ExprPlaceholder(TextElement("")))
      case _               => None
    },
    hidden = true
  )

  addTypeBuilder(
    "TypePlaceholder",
    {
      case List(s: String) => Some(TypePlaceholder(TextElement(s)))
      case Nil             => Some(TypePlaceholder(TextElement("")))
      case _               => None
    },
    hidden = true
  )

  addValueBuilder(
    "ValuePlaceholder",
    {
      case List(s: String) => Some(ValuePlaceholder(TextElement(s)))
      case Nil             => Some(ValuePlaceholder(TextElement("")))
      case _               => None
    }
  )

  addValueBuilder(
    "TypeValueContainer",
    {
      case List(t: Type) => Some(TypeValueContainer(t))
      case Nil           => Some(TypeValueContainer(TypePlaceholder(TextElement(""))))
      case _             => None
    }
  )

  addTypeBuilder(
    "TypeContainer",
    {
      case List(t: Type) => Some(TypeContainer(t))
      case Nil           => Some(TypeContainer(TypePlaceholder(TextElement(""))))
      case _             => None
    },
    hidden = true
  )

  addTypeBuilder(
    "UnknownType",
    {
      case Nil => Some(UnknownType())
      case _   => None
    },
    hidden = true
  )

  // </editor-fold>

  val defaultLiteral: Literal = Literal.fromString("")
}
