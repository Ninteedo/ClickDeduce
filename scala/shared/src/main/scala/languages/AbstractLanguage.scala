package languages

import app.{ClickDeduceException, HTMLHelper, UtilityFunctions}
import convertors.*
import scalatags.Text.TypedTag
import scalatags.Text.all.*

import scala.annotation.targetName
import scala.collection.immutable.List
import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers

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
    private def readKey(key: Variable | LiteralIdentifier): Variable = key match {
      case v: Variable          => v
      case l: LiteralIdentifier => l.getIdentifier
    }

    def get(key: Variable | LiteralIdentifierLookup): Option[T] = env.get(readKey(key))

    def set(key: Variable | LiteralIdentifierBind, value: T): Env[T] = new Env(env + (readKey(key) -> value))

    @targetName("setVariable")
    def +(key: Variable | LiteralIdentifierBind, value: T): Env[T] = {
      set(key, value)
    }

    @targetName("setVariableTuple")
    def +(kv: (Variable | LiteralIdentifierBind, T)): Env[T] = {
      val (k, v) = kv
      this + (k, v)
    }

    @targetName("setVariables")
    def ++(other: Env[T]): Env[T] = new Env(env ++ other.env)

    def getOrElse(key: Variable | LiteralIdentifierLookup, default: => T): T = env.getOrElse(readKey(key), default)

    def isEmpty: Boolean = env.isEmpty

    def nonEmpty: Boolean = env.nonEmpty

    def map[U](f: ((Variable, T)) => U): Iterable[U] = env.map(f)

    def mapToEnv[U](f: ((Variable, T)) => (Variable, U)): Env[U] = new Env(env.map(f))

    def keys: Iterable[Variable] = env.keys

    def toMap: Map[Variable, T] = env
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

    /** Whether to show variables with this value when displaying a list of bound variables.
      */
    val showInValueLookupList: Boolean = true
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

    private def getTypeFields: List[Type] = this match {
      case t0: Product => t0.productIterator.toList.collect({ case t: Type => t })
      case _           => Nil
    }

    private def defaultChildren(env: TypeEnv): List[(Term, TypeEnv)] = getTypeFields.zip(LazyList.continually(env))

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = defaultChildren(tEnv)
  }

  case class UnknownType() extends Type {
    override def toText: ConvertableText = TextElement("Unknown")

    override val needsBrackets: Boolean = false
  }

  object UnknownType extends TypeCompanion {
    override protected val isHidden: Boolean = true

    override def create(args: BuilderArgs): Option[Type] = args match {
      case Nil => Some(UnknownType())
      case _   => None
    }
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

  object TypeContainer extends TypeCompanion {
    override protected val isHidden: Boolean = true

    override def create(args: BuilderArgs): Option[Type] = args match {
      case List(t: Type) => Some(TypeContainer(t))
      case Nil           => Some(TypeContainer(TypePlaceholder(TextElement(""))))
      case _             => None
    }
  }

  case class TypeValueContainer(typ: Type) extends Value {
    override def valueTextShowType: Boolean = false

    override def toText: ConvertableText = typ.toText

    override val showInValueLookupList: Boolean = false
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

    override lazy val toHtml: TypedTag[String] = p(getValue)

    def toHtmlInput(treePath: String, env: ValueEnv | TypeEnv): TypedTag[String] =
      HTMLHelper.literalInputBase(treePath, getValue)

    def toHtmlInputReadOnly(originPath: String): TypedTag[String] =
      HTMLHelper.literalInputBaseReadOnly(originPath, getValue)

    val defaultContents: String = ""

    private var overrideContents: Option[String] = None

    def getValue: String = overrideContents.getOrElse(value.toString)

    def setOverrideContents(contents: String): Unit = overrideContents = Some(contents)
  }

  /** Companion object for [[Literal]].
    */
  object Literal {
    def fromStringOfType(s: String, l: Class[_ <: Literal]): Literal = l match {
      case c if c == classOf[LiteralBool]             => LiteralBool(s.toBoolean)
      case c if c == classOf[LiteralString]           => LiteralString(s)
      case c if c == classOf[LiteralInt]              => LiteralInt.fromString(s)
      case c if c == classOf[LiteralIdentifierBind]   => LiteralIdentifierBind(s)
      case c if c == classOf[LiteralIdentifierLookup] => LiteralIdentifierLookup(s)
      case c if c == classOf[LiteralAny]              => LiteralAny(s)
      case _                                          => throw LiteralParseException(s)
    }
  }

  protected trait LiteralParser extends JavaTokenParsers {
    def literalName: Parser[String] =
      "LiteralString" | "LiteralInt" | "LiteralBool" | "LiteralIdentifierBind" | "LiteralIdentifierLookup" |
        "LiteralAny"

    def literalArg: Parser[BigInt | String | Boolean] = wholeNumber ^^ (n => BigInt(n))
      | "true" ^^ (_ => true) | "false" ^^ (_ => false)
      | stringLiteral ^^ (s => UtilityFunctions.unquote(s))
      | ident // any identifier
      | ""

    def literal: Parser[Literal] = literalName ~ "(" ~ literalArg ~ ")" ^^ {
      case name ~ "(" ~ arg ~ ")" =>
        (name, arg) match {
          case ("LiteralInt", n: BigInt)              => LiteralInt(n)
          case ("LiteralString", s: String)           => LiteralString(s)
          case ("LiteralBool", b: Boolean)            => LiteralBool(b)
          case ("LiteralIdentifierBind", s: String)   => LiteralIdentifierBind(s)
          case ("LiteralIdentifierLookup", s: String) => LiteralIdentifierLookup(s)
          case ("LiteralAny", s: String)              => LiteralAny(s)
          case _                                      => throw LiteralParseException(s"$name($arg)")
        }
      case other => throw LiteralParseException(other.toString)
    }
  }

  private case class LiteralParseException(message: String) extends ClickDeduceException(message)

  /** A literal integer.
    *
    * Can store extremely large integers (using [[BigInt]]).
    *
    * @param value
    *   The integer value.
    */
  case class LiteralInt(value: BigInt) extends Literal {
    override def toText: ConvertableText = MathElement(getValue)

    override def toHtmlInput(treePath: String, env: ValueEnv | TypeEnv): TypedTag[String] = HTMLHelper
      .literalInputBase(treePath, getValue, inputKind = "number", extraClasses = "integer")
  }

  object LiteralInt {
    def fromString(s: String): LiteralInt = try {
      LiteralInt(BigInt(s))
    } catch {
      case _: NumberFormatException =>
        throw LiteralParseException(s"LiteralInt only accepts integer values, not \"$s\"")
    }

    val default: LiteralInt = LiteralInt(0)
  }

  /** A literal boolean.
    * @param value
    *   The boolean value.
    */
  case class LiteralBool(value: Boolean) extends Literal {
    override def toText: ConvertableText = MathElement(getValue)

    override def toHtmlInput(treePath: String, env: ValueEnv | TypeEnv): TypedTag[String] = div(
      cls := "literal-checkbox-container",
      input(
        `type` := "checkbox",
        data("tree-path") := treePath,
        cls := ClassDict.LITERAL + " " + "boolean",
        if (getValue.toBoolean) checked else ()
      )
    )

    override def toHtmlInputReadOnly(originPath: String): TypedTag[String] =
      HTMLHelper.literalInputBaseReadOnly(originPath, getValue, extraClasses = "boolean")
  }

  /** A literal string.
    *
    * When entered, surrounded by double quotes. Also surrounded by double quotes when converted to text.
    * @param value
    *   The string value.
    */
  case class LiteralString(value: String) extends Literal {
    override lazy val toString: String = s"LiteralString(${UtilityFunctions.quote(value)})"

    override def toText: ConvertableText = TextElement(getValue)
  }

  trait LiteralIdentifier extends Literal {

    /** Whether the identifier is valid.
      */
    def validIdentifier: Boolean = identifierRegex.matches(getIdentifier)

    protected val identifierRegex: Regex = "[A-Za-z_$][\\w_$]*".r

    def getIdentifier: String = getValue

    def identEquals(other: Any): Boolean = other match {
      case LiteralIdentifierBind(s)   => s == getIdentifier
      case LiteralIdentifierLookup(s) => s == getIdentifier
      case _                          => false
    }
  }

  /** A literal identifier used for binding a variable.
    * @param value
    *   The identifier value.
    */
  case class LiteralIdentifierBind(value: String) extends LiteralIdentifier {
    override lazy val toString: String = s"LiteralIdentifierBind(${UtilityFunctions.quote(value)})"

    override def toText: ConvertableText = TextElement(getValue)

    /** Convert this to a [[LiteralIdentifierLookup]] with the same identifier.
      */
    def toLookup: LiteralIdentifierLookup = LiteralIdentifierLookup(value)
  }

  object LiteralIdentifierBind {
    val default: LiteralIdentifierBind = LiteralIdentifierBind("")
  }

  /** A literal identifier lookup.
    * @param value
    *   The identifier value.
    */
  case class LiteralIdentifierLookup(value: String) extends LiteralIdentifier {
    override lazy val toString: String = s"LiteralIdentifierLookup(${UtilityFunctions.quote(value)})"

    override def toText: ConvertableText = ItalicsElement(TextElement(getValue))

    /** Create an input for this literal identifier, with a dropdown showing bound variables in the environment.
      * @param treePath
      *   The path to this literal in the tree.
      * @param env
      *   The environment.
      * @return
      *   The HTML input.
      */
    override def toHtmlInput(treePath: String, env: ValueEnv | TypeEnv): TypedTag[String] = {
      div(
        cls := "literal-identifier-container",
        HTMLHelper.literalInputBase(treePath, getValue, extraClasses = "identifier-lookup"),
        div(
          cls := "dropdown",
          ul(
            cls := "identifier-suggestions",
            env.toMap
              .filter({
                case (_, v: Value) => v.showInValueLookupList
                case _             => true
              })
              .map((k, v) => li(data("value") := k, data("filter") := k, span(k, ": ", v.toHtml)))
              .toSeq
          )
        )
      )
    }

    /** Convert this to a [[LiteralIdentifierBind]] with the same identifier.
      */
    def toBind: LiteralIdentifierBind = LiteralIdentifierBind(value)
  }

  object LiteralIdentifierLookup {
    val identifierRegex: Regex = "[A-Za-z_$][\\w_$]*".r

    val default: LiteralIdentifierLookup = LiteralIdentifierLookup("")
  }

  /** A literal with no restrictions.
    * @param value
    *   The value.
    */
  case class LiteralAny(value: String) extends Literal {
    override lazy val toString: String = s"LiteralAny(${UtilityFunctions.quote(value)})"

    override def toText: ConvertableText = TextElement(getValue)
  }

  protected def placeholderOfLiteral(literal: Literal, contents: String): Literal = {
    val copy = literal match {
      case LiteralInt(n)              => LiteralInt(n)
      case LiteralBool(b)             => LiteralBool(b)
      case LiteralString(s)           => LiteralString(s)
      case LiteralIdentifierBind(s)   => LiteralIdentifierBind(s)
      case LiteralIdentifierLookup(s) => LiteralIdentifierLookup(s)
      case LiteralAny(s)              => LiteralAny(s)
    }
    copy.setOverrideContents(contents)
    copy
  }

  // </editor-fold>

  // <editor-fold desc="Builders">

  /** Register a list of terms for a particular language.
    *
    * Should be called at the start of every language class.
    * @param langName
    *   The name of the language.
    * @param terms
    *   The companion objects of the terms to register.
    */
  protected def registerTerms(langName: String, terms: List[ExprCompanion | TypeCompanion | ValueCompanion]): Unit = {
    terms.foreach(_.register(langName))
  }

  /** Manages the defined builders for a particular type of term.
    * @tparam T
    *   The type of term.
    */
  protected class BuilderManager[T <: Term] {
    private var builders: Map[String, BuilderArgs => Option[T]] = Map()
    private var builderNamesList: List[BuilderName] = List()

    /** Add a builder to the manager.
      * @param name
      *   The name of the builder.
      * @param builder
      *   The builder function.
      * @param langName
      *   The name of the language.
      * @param hidden
      *   Whether the term should be hidden from the user.
      * @param aliases
      *   List of alternate names for the term.
      */
    private def addBuilder(
      name: String,
      builder: BuilderArgs => Option[T],
      langName: String,
      hidden: Boolean = false,
      aliases: List[String] = Nil
    ): Unit = {
      builders += (name -> builder)
      if (!hidden) {
        val entry = if (aliases.isEmpty) name else (name, aliases)
        builderNamesList = builderNamesList :+ (langName, entry)
      }
    }

    /** Get a builder by name.
      * @param name
      *   The name of the builder. Does not match aliases.
      * @return
      *   Some builder, or None if not found.
      */
    def getBuilder(name: String): Option[BuilderArgs => Option[T]] = builders.get(name)

    /** Build a term by name and arguments.
      * @param name
      *   The name of the builder.
      * @param args
      *   The arguments.
      * @return
      *   The result from the builder, either some term or None.
      * @throws UnknownTermBuilder
      *   If the builder is not found.
      */
    def build(name: String, args: BuilderArgs): T = getBuilder(name) match {
      case Some(builder) =>
        val res = builder.apply(args)
        if (res.isEmpty) throw TermBuilderFailed(name, args)
        res.get
      case None => throw UnknownTermBuilder(name)
    }

    /** Get the names of all builders.
      * @return
      *   The list of builder names.
      */
    def builderNames: List[BuilderName] = builderNamesList

    /** Companion object for terms, to be extended by the term companion objects.
      */
    trait Companion {

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

      /** List of alternate names for the term.
        */
      protected val aliases: List[String] = Nil

      /** Create a term from a list of arguments.
        *
        * Typically needs to handle 3 cases:
        *   - [[defaultArgs]]: The default version of the term, with unselected sub-terms and empty literals.
        *   - Arguments matching the expected structure for this term: The actual term.
        *   - Any other arguments: Invalid arguments; should return None.
        *
        * @param args
        *   The arguments.
        * @return
        *   Some expression, or None if the arguments are invalid.
        */
      def create(args: BuilderArgs): Option[T]

      /** Register the term builder.
        * @param langName
        *   The name of the language.
        */
      final def register(langName: String): Unit = {
        addBuilder(name, create, langName, isHidden, aliases)
      }
    }

  }

  protected val exprBuilderManager = new BuilderManager[Expr]
  protected val typeBuilderManager = new BuilderManager[Type]
  protected val valueBuilderManager = new BuilderManager[Value]

  protected trait ExprCompanion extends exprBuilderManager.Companion
  protected trait TypeCompanion extends typeBuilderManager.Companion
  protected trait ValueCompanion extends valueBuilderManager.Companion {

    /** Value builders are not currently used, so this method always returns None.
      */
    override final def create(args: BuilderArgs): Option[Value] = None
  }

  protected type BuilderArgs = List[Literal | Term]

  protected type BuilderName =
    (String, String | (String, List[String])) // langName, then either name or (name, aliases)

  /** Returns the names of all expression builders.
    * @return
    *   The list of expression builder names.
    */
  def exprBuilderNames: List[BuilderName] = exprBuilderManager.builderNames

  /** Returns the names of all type builders.
    * @return
    *   The list of type builder names.
    */
  def typeBuilderNames: List[BuilderName] = typeBuilderManager.builderNames

  /** Get an expression builder by name.
    * @param name
    *   The name of the builder.
    * @return
    *   The builder, or None if not found.
    */
  def getExprBuilder(name: String): Option[BuilderArgs => Option[Expr]] = exprBuilderManager.getBuilder(name)

  /** Build an expression by name and arguments.
    * @param name
    *   The name of the builder.
    * @param args
    *   The arguments.
    * @return
    *   The expression, or throw an [[UnknownTermBuilder]] exception if the builder is not found.
    */
  def buildExpr(name: String, args: BuilderArgs): Expr = exprBuilderManager.build(name, args)

  /** Get a type builder by name.
    * @param name
    *   The name of the builder.
    * @return
    *   The builder, or None if not found.
    */
  def getTypeBuilder(name: String): Option[BuilderArgs => Option[Type]] = typeBuilderManager.getBuilder(name)

  /** Build a type by name and arguments.
    * @param name
    *   The name of the builder.
    * @param args
    *   The arguments.
    * @return
    *   The type, or throw an [[UnknownTypeBuilder]] exception if the builder is not found.
    */
  def buildType(name: String, args: BuilderArgs): Type = typeBuilderManager.build(name, args)

  private case class UnknownTermBuilder(name: String) extends ClickDeduceException(s"Unknown term builder: $name")

  private case class TermBuilderFailed(name: String, args: BuilderArgs)
      extends ClickDeduceException(s"Failed to build $name with args $args")

  registerTerms("AbstractLanguage", List(UnknownType, TypeContainer))

  // </editor-fold>
}
