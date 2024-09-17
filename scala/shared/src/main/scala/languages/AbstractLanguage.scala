package languages

import app.ClickDeduceException
import convertors.*
import scalatags.Text.TypedTag
import scalatags.Text.all.*

import scala.collection.immutable.List

/** Base trait, defining the term structure of all languages.
  *
  * This trait defines the basic structure of a language, including expressions, values, types, and literals.
  *
  * Extended by [[AbstractNodeLanguage]].
  */
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

  /** Companion object for [[Env]].
    */
  object Env {

    /** Create an environment from a list of key-value pairs.
      * @param items
      *   The key-value pairs.
      * @tparam T
      *   The type of the values.
      * @return
      *   The environment.
      */
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

  /** Companion object for [[ValueEnv]].
    */
  object ValueEnv {

    /** An empty value environment.
      */
    val empty = new ValueEnv(Map())
  }

  /** Companion object for [[TypeEnv]].
    */
  object TypeEnv {

    /** An empty type environment.
      */
    val empty = new TypeEnv(Map())
  }

  /** Convert a value environment to a type environment.
    * @param env
    *   The value environment.
    * @return
    *   The type environment.
    */
  def envToTypeEnv(env: ValueEnv): TypeEnv = env.mapToEnv((k: String, v: Value) => (k, v.typ))

  /** Convert a value or type environment to a type environment that only contains type values and variables.
    * @param env
    *   The value or type environment.
    * @return
    *   The type environment.
    */
  def typeVariableEnv(env: ValueEnv | TypeEnv): TypeEnv = {
    Env(env.env.collect({
      case (k, TypeValueContainer(t)) => (k, t)
      case (k, TypeContainer(t))      => (k, t)
    }))
  }

  // </editor-fold>

  /** A term in the language.
    *
    * Base trait for all abstract language features in a language.
    *
    * Terms can be [[Expr]]s, [[Value]]s, [[Type]]s, or [[Literal]]s.
    */
  trait Term {

    /** The name of this term.
      *
      * This is the name of the class by default, but can be overridden.
      */
    val name: String = toString.takeWhile(_ != '(')

    def getChildrenBase(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] = Nil

    def getChildrenEval(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] = Nil

    def getChildrenTypeCheck(tEnv: TypeEnv = TypeEnv.empty): List[(Term, TypeEnv)] = Nil

    /** Whether this term is a placeholder.
      *
      * Placeholders are used when converting terms to text, and are not evaluated or type-checked.
      *
      * Default is false, should only be overridden for placeholder terms.
      *
      * @return
      *   True if this term is a placeholder, false otherwise.
      */
    def isPlaceholder: Boolean = false

    /** Whether this term should be surrounded by brackets when converted to text inside another term.
      *
      * Default is true, should only be overridden for terms that should not be bracketed in any context. This is common
      * when a term is displayed as a single element, for example a number or variable.
      *
      * A term is bracketed if this is true and it is being called by [[toTextBracketed]].
      */
    val needsBrackets: Boolean = true

    /** Convert this term to [[ConvertableText]].
      *
      * This is used as a standard output format for different convertors, like [[HTMLConvertor]] and
      * [[LaTeXConvertor]].
      *
      * @return
      *   This term as [[ConvertableText]].
      */
    def toText: ConvertableText

    /** Convert this term to [[ConvertableText]], surrounded by brackets if [[needsBrackets]] is true.
      */
    final def toTextBracketed: ConvertableText = if (needsBrackets) BracketedElement(toText) else toText

    /** Convert this term to HTML.
      */
    lazy val toHtml: TypedTag[String] = toText.asHtml

    /** Convert this term to plain text.
      * @return
      *   The plain text representation of this term.
      */
    final def prettyPrint: String = toText.asPlainText

    /** Convert this term to plain text, surrounded by brackets if [[needsBrackets]] is true.
      * @return
      *   The plain text representation of this term.
      */
    final def prettyPrintBracketed: String = if (needsBrackets) s"($prettyPrint)" else prettyPrint
  }

  // <editor-fold desc="Expressions">

  /** An unevaluated expression.
    *
    * [[evalInner]] and [[typeCheckInner]] methods need to be implemented. The [[eval]] and [[typeCheck]] methods are
    * provided as wrappers around these methods, which should be used for evaluation and type-checking.
    *
    * The methods for getting children are provided as default implementations, which can be overridden if the
    * expression has a different structure, e.g. if some children have updated environments.
    *
    * The builder for the expression needs to be registered using either the [[ExprCompanion]] trait, or by calling
    * [[addExprBuilder]].
    */
  abstract class Expr extends Term {
    def getExprFields: List[Expr] = {
      this match {
        case e0: Product =>
          val values = e0.productIterator.toList
          values.collect({ case e: Expr => e })
        case _ => Nil
      }
    }

    private def defaultChildren[EnvContents](env: Env[EnvContents]): List[(Term, Env[EnvContents])] =
      getExprFields.zip(LazyList.continually(env))

    /** Children of this expression in [[DisplayMode.Edit]] mode.
      * @param env
      *   The current environment.
      * @return
      *   A list of pairs of children and their environments.
      */
    override def getChildrenBase(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] = defaultChildren(env)

    /** Children of this expression in [[DisplayMode.Evaluation]] mode.
      * @param env
      *   The current environment.
      * @return
      *   A list of pairs of children and their environments.
      */
    override def getChildrenEval(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] = defaultChildren(env)

    /** Children of this expression in [[DisplayMode.TypeCheck]] mode.
      * @param tEnv
      *   The current type environment.
      * @return
      *   A list of pairs of children and their environments.
      */
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

    /** Type-check this `Expr` in the given type environment.
      *
      * @param tEnv
      *   The type environment in which type-checking is done.
      * @return
      *   The `Type` of this expression after type-checking.
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

  /** An expression that is not implemented.
    *
    * Not expected to be used in practice, but provided as an option for expressions that are not yet implemented or
    * should not be used.
    */
  abstract class NotImplementedExpr extends Expr {
    override def evalInner(env: ValueEnv): Value = UnexpectedExpr(toString)

    override def typeCheckInner(tEnv: TypeEnv): Type = UnexpectedExprType(toString)
  }

  /** An expression that should not be used.
    *
    * This can be used for development purposes but is not expected to be used in practice.
    */
  case class MissingExpr() extends NotImplementedExpr {
    override def toText: ConvertableText = TextElement("Missing")
  }

  /** A placeholder for text in an expression.
    *
    * Used when converting expressions to text in a recursive manner.
    *
    * Not intended for use within actual languages and is hidden from the user.
    *
    * @param content
    *   The text content.
    * @param needsBrackets
    *   Whether this placeholder should be surrounded by brackets when converted to text.
    */
  case class ExprPlaceholder(content: ConvertableText, override val needsBrackets: Boolean = false)
      extends NotImplementedExpr {
    override def toText: ConvertableText = content
  }

  /** Companion object for [[ExprPlaceholder]].
    */
  object ExprPlaceholder {

    /** Create an expression placeholder from an expression.
      * @param expr
      *   The expression.
      * @return
      *   The expression placeholder.
      */
    def apply(expr: Expr): ExprPlaceholder = ExprPlaceholder(expr.toText, expr.needsBrackets)
  }

  // </editor-fold>

  // <editor-fold desc="Values">

  /** A value resulting from an [[Expr]] being evaluated.
    *
    * Also has a corresponding [[Type]].
    */
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

    /** Whether this value represents an evaluation error.
      *
      * Defaults to false, overridden in [[EvalError]].
      */
    val isError: Boolean = false

    /** Whether to show the type of the value in the text representation.
      *
      * Default is true, should be overridden for values that should not show their type.
      */
    def valueTextShowType: Boolean = true
  }

  /** A placeholder for text in a value.
    * @param content
    *   The text content.
    * @param needsBrackets
    *   Whether this placeholder should be surrounded by brackets when converted to text.
    */
  case class ValuePlaceholder(content: ConvertableText, override val needsBrackets: Boolean = false) extends Value {
    override val typ: Type = TypePlaceholder(content, needsBrackets)

    override def toText: ConvertableText = content
  }

  /** Companion object for [[ValuePlaceholder]].
    */
  object ValuePlaceholder {

    /** Create a value placeholder from a value.
      * @param value
      *   The value.
      * @return
      *   The value placeholder.
      */
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

  /** An error that occurs due to attempting to evaluate an expression that results in an exception.
    *
    * This is used when an exception is thrown during evaluation, not just when the evaluation results in an error. For
    * example, a stack overflow.
    * @param message
    *   The error message.
    */
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

  /** An error that occurs due to attempting to type-check an expression that results in an exception.
    *
    * This is used when an exception is thrown during type-checking, not just when the type-checking results in an
    * error. For example, a stack overflow.
    *
    * @param message
    *   The error message.
    */
  case class TypeException(override val message: String) extends TypeError

  // </editor-fold>

  // <editor-fold desc="Literals">

  /** A literal term. Entered as a string. */
  abstract class Literal extends Term {
    val value: Any

    override lazy val toHtml: TypedTag[String] = p(value.toString)

    override lazy val toString: String = value.toString
  }

  /** Companion object for [[Literal]].
    */
  object Literal {

    /** Create an appropriate literal from a string.
      *
      * The string is checked for the following types, in order of priority:
      *   - [[LiteralBool]]: `true` or `false` (case-insensitive)
      *   - [[LiteralString]]: Enclosed in double quotes
      *   - [[LiteralInt]]: A sequence of digits, optionally preceded by a minus sign
      *   - [[LiteralIdentifier]]: A sequence of letters, digits, and underscores, starting with a letter or underscore
      *     (non-empty)
      *   - [[LiteralAny]]: Anything else
      *
      * @param s
      *   The string to convert.
      * @return
      *   The literal.
      */
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

  /** A literal integer.
    *
    * Can store extremely large integers (using [[BigInt]]).
    *
    * @param value
    *   The integer value.
    */
  case class LiteralInt(value: BigInt) extends Literal {
    override def toText: ConvertableText = MathElement(value.toString)
  }

  /** A literal boolean.
    * @param value
    *   The boolean value.
    */
  case class LiteralBool(value: Boolean) extends Literal {
    override def toText: ConvertableText = MathElement(value.toString)
  }

  /** A literal string.
    *
    * When entered, surrounded by double quotes. Also surrounded by double quotes when converted to text.
    * @param value
    *   The string value.
    */
  case class LiteralString(value: String) extends Literal {
    override lazy val toString: String = s""""$value""""

    override def toText: ConvertableText = TextElement(toString)
  }

  /** A literal identifier.
    * @param value
    *   The identifier value.
    */
  case class LiteralIdentifier(value: String) extends Literal {
    override lazy val toString: String = value

    override def toText: ConvertableText = ItalicsElement(TextElement(toString))
  }

  /** A literal with no restrictions.
    * @param value
    *   The value.
    */
  case class LiteralAny(value: String) extends Literal {
    override lazy val toString: String = value

    override def toText: ConvertableText = TextElement(toString)
  }

  // </editor-fold>

  // <editor-fold desc="Builders">

  /** Parent trait for all term companions.
    */
  trait TermCompanion {

    /** Whether the term should be hidden from the user.
      *
      * If true, the term will not appear in the list of available terms.
      *
      * Default is false.
      */
    protected val isHidden: Boolean = false

    /** The name of the term.
      *
      * By default, this is the name of the companion object, but can be overridden.
      */
    protected val name: String = toString.dropWhile(_ != '$').drop(1).takeWhile(_ != '$')

    /** The arguments for the default term builder case.
      *
      * Equivalent to `Nil`.
      */
    protected final val defaultArgs: List[Any] = Nil

    /**
     * List of alternate names for the term.
     */
    protected val aliases: List[String] = Nil

    /** Register the term builder.
      *
      * This needs to be called from outside the companion object to register the term builder. Otherwise, the Scala.js
      * compiler will consider the companion object to be unused and remove it.
      */
    def register(): Unit
  }

  /** Trait for expression companions.
    *
    * Should be used by companion objects for expressions.
    *
    * [[createExpr]] needs to be implemented. [[register]] needs to be called from outside the companion object to
    * register the expression builder.
    */
  trait ExprCompanion extends TermCompanion {

    /** Create an expression from a list of arguments.
      *
      * Typically needs to handle 3 cases:
      *   - [[defaultArgs]]: The default version of the expression, with unselected expressions and empty literals.
      *   - Arguments matching the expected structure for this expression: The actual expression.
      *   - Any other arguments: Invalid arguments, should return None.
      * @param args
      *   The arguments.
      * @return
      *   Some expression, or None if the arguments are invalid.
      */
    protected def createExpr(args: List[Any]): Option[Expr]

    final def register(): Unit = addExprBuilder(name, createExpr, hidden = isHidden, aliases = aliases)
  }

  /** Trait for type companions.
    *
    * Should be used by companion objects for types.
    *
    * [[createType]] needs to be implemented. [[register]] needs to be called from outside the companion object to
    * register the type builder.
    */
  trait TypeCompanion extends TermCompanion {

    /** Create a type from a list of arguments.
      *
      * Typically needs to handle 3 cases:
      *   - [[defaultArgs]]: The default version of the type, with unselected types and empty literals.
      *   - Arguments matching the expected structure for this type: The actual type.
      *   - Any other arguments: Invalid arguments, should return None.
      * @param args
      *   The arguments.
      * @return
      *   Some type, or None if the arguments are invalid.
      */
    protected def createType(args: List[Any]): Option[Type]

    final def register(): Unit = addTypeBuilder(name, createType, hidden = isHidden, aliases = aliases)
  }

  /**
   * Trait for value companions.
   *
   * Should be used by companion objects for values.
   *
   * [[createValue]] needs to be implemented.
   * [[register]] needs to be called from outside the companion object to register the value builder.
   */
  trait ValueCompanion extends TermCompanion {
    /** Create a value from a list of arguments.
     *
     * @param args
     *   The arguments.
     * @return
     *   Some value, or None if the arguments are invalid.
     */
    protected def createValue(args: List[Any]): Option[Value]

    final def register(): Unit = addValueBuilder(name, createValue)
  }

  /** A function that takes a list of arguments and returns a constructed expression if valid, or None if invalid.
    */
  private type ExprBuilder = List[Any] => Option[Expr]

  private var exprBuilders: Map[String, ExprBuilder] = Map()

  private var exprBuilderNamesList: List[BuilderName] = List()

  /** Add an expression builder to the language.
    * @param name
    *   The name of the builder.
    * @param builder
    *   The expression builder.
    * @param hidden
    *   Whether the builder should be hidden from the user (won't appear in the expression list), default is false.
    */
  private def addExprBuilder(name: String, builder: ExprBuilder, hidden: Boolean = false, aliases: List[String] = Nil): Unit = {
    exprBuilders += (name -> builder)
    if (!hidden) {
      val entry = if (aliases.isEmpty) name else (name, aliases)
      exprBuilderNamesList = exprBuilderNamesList :+ entry
    }
  }

  type BuilderName = String | (String, List[String])  // either name or (name, aliases)

  /** Returns the names of all expression builders.
    * @return
    *   The list of expression builder names.
    */
  def exprBuilderNames: List[BuilderName] = exprBuilderNamesList

  private type TypeBuilder = List[Any] => Option[Type]

  private var typeBuilders: Map[String, TypeBuilder] = Map()

  private var typeBuilderNamesList: List[BuilderName] = List()

  /** Add a type builder to the language.
    * @param name
    *   The name of the builder.
    * @param builder
    *   The type builder.
    * @param hidden
    *   Whether the builder should be hidden from the user (won't appear in the type list), default is false.
    * @param aliases
    *   The aliases of the builder.
    */
  private def addTypeBuilder(name: String, builder: TypeBuilder, hidden: Boolean = false, aliases: List[String] = Nil): Unit = {
    typeBuilders += (name -> builder)
    if (!hidden) {
      val entry = if (aliases.isEmpty) name else (name, aliases)
      typeBuilderNamesList = typeBuilderNamesList :+ entry
    }
  }

  /** Returns the names of all type builders.
    * @return
    *   The list of type builder names.
    */
  def typeBuilderNames: List[BuilderName] = typeBuilderNamesList

  private type ValueBuilder = List[Any] => Option[Value]

  private var valueBuilders: Map[String, ValueBuilder] = Map()

  private def addValueBuilder(name: String, builder: ValueBuilder): Unit = {
    valueBuilders += (name -> builder)
  }

  /** Get an expression builder by name.
    * @param name
    *   The name of the builder.
    * @return
    *   The builder, or None if not found.
    */
  def getExprBuilder(name: String): Option[ExprBuilder] = exprBuilders.get(name)

  /** Build an expression by name and arguments.
    * @param name
    *   The name of the builder.
    * @param args
    *   The arguments.
    * @return
    *   The expression, or throw an [[UnknownExprBuilder]] exception if the builder is not found.
    */
  def buildExpr(name: String, args: List[Any]): Option[Expr] = getExprBuilder(name) match {
    case Some(builder) => builder.apply(args)
    case None          => throw UnknownExprBuilder(name)
  }

  /** Get a type builder by name.
    * @param name
    *   The name of the builder.
    * @return
    *   The builder, or None if not found.
    */
  def getTypeBuilder(name: String): Option[TypeBuilder] = typeBuilders.get(name)

  /** Build a type by name and arguments.
    * @param name
    *   The name of the builder.
    * @param args
    *   The arguments.
    * @return
    *   The type, or throw an [[UnknownTypeBuilder]] exception if the builder is not found.
    */
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

  /** The default literal, contains the empty string.
    */
  val defaultLiteral: Literal = Literal.fromString("")
}
