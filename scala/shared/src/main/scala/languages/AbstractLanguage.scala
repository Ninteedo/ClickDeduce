package languages

import languages.previews.RulePreview
import languages.terms.Term
import languages.terms.builders.*
import languages.terms.exprs.Expr
import languages.terms.types.{Type, TypeContainer, UnknownType}
import languages.terms.values.Value

import scala.collection.immutable.List
import scala.util.parsing.combinator.JavaTokenParsers

/** Base trait, defining the term structure of all languages.
  *
  * This trait defines the basic structure of a language, including expressions, values, types, and literals.
  *
  * Extended by [[AbstractNodeLanguage]].
  */
trait AbstractLanguage {
  lang =>

  // <editor-fold desc="Builders">

  private val exprBuilderManager = new ExprBuilderManager
  private val typeBuilderManager = new BuilderManager[Type, TypeCompanion]
  private val valueBuilderManager = new BuilderManager[Value, ValueCompanion]

  /** Register a list of terms for a particular language.
    *
    * Should be called at the start of every language class.
    * @param langName
    *   The name of the language.
    * @param terms
    *   The companion objects of the terms to register.
    */
  protected def registerTerms(langName: String, terms: List[BuilderCompanion[_ <: Term]]): Unit = {
    terms.foreach {
      case companion: ExprCompanion => exprBuilderManager.register(companion, langName)
      case companion: TypeCompanion => typeBuilderManager.register(companion, langName)
      case companion: ValueCompanion => valueBuilderManager.register(companion, langName)
    }
  }

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

  def getExprRulePreview(name: String): Option[RulePreview] = exprBuilderManager.getRulePreview(name)

  protected trait ExprParser extends JavaTokenParsers {
    def expr: Parser[Expr]

    protected trait ExprOperator {
      def precedence: Int
    }

    protected case class BasicBinaryOperator(
      op: String,
      apply: (Expr, Expr) => Expr,
      precedence: Int,
      associativity: Associativity
    ) extends ExprOperator

    case class SpecialParser(parser: Parser[Expr], precedence: Int) extends ExprOperator

    case class LeftRecursiveOperator(
      parser: Parser[Expr => Expr],
      precedence: Int
    ) extends ExprOperator

    protected enum Associativity {
      case Left, Right
    }

    protected def exprOperators: List[ExprOperator] = List.empty

    protected def precedenceParser(base: Parser[Expr], operators: List[ExprOperator]): Parser[Expr] = {
      if (operators.isEmpty) base
      else {
        val groupedByPrecedence = operators.groupBy(_.precedence).toList.sortBy(-_._1)
        groupedByPrecedence.foldLeft(base) {
          case (acc, (_, ops)) =>
            val leftAssocOps = ops.collect { case b: BasicBinaryOperator if b.associativity == Associativity.Left => b }
            val rightAssocOps = ops.collect { case b: BasicBinaryOperator if b.associativity == Associativity.Right => b }
            val leftRecursiveParsers = ops.collect { case l: LeftRecursiveOperator => l.parser }
            val specialParsers = ops.collect { case s: SpecialParser => s.parser }

            val leftParser = if (leftAssocOps.isEmpty) acc else {
              val opParser = leftAssocOps.map { case BasicBinaryOperator(op, fn, _, _) => op ^^^ fn }.reduce(_ | _)
              acc ~ rep(opParser ~ acc) ^^ { case start ~ rest =>
                rest.foldLeft(start) { case (lhs, fn ~ rhs) => fn(lhs, rhs) }
              }
            }

            val rightParser = if (rightAssocOps.isEmpty) leftParser else {
              val opParser = rightAssocOps.map { case BasicBinaryOperator(op, fn, _, _) => op ^^^ fn }.reduce(_ | _)
              acc ~ opt(opParser ~ precedenceParser(acc, rightAssocOps)) ^^ {
                case lhs ~ Some(fn ~ rhs) => fn(lhs, rhs)
                case lhs ~ None => lhs
              }
            }

            val leftRecursiveParser = if (leftRecursiveParsers.isEmpty) rightParser else {
              rightParser ~ rep(leftRecursiveParsers.reduce(_ | _)) ^^ {
                case base ~ transformations => transformations.foldLeft(base)((expr, transform) => transform(expr))
              }
            }

            val specialParser = if (specialParsers.isEmpty) leftRecursiveParser else {
              leftRecursiveParser | specialParsers.reduce(_ | _)
            }

            specialParser
        }
      }
    }
  }

  protected val exprParser: ExprParser

  def parseExpr(exprText: String): Either[(String, Int), Expr] = exprParser.parseAll(exprParser.expr, exprText) match {
    case exprParser.Success(result, _) => Right(result)
    case exprParser.Failure(msg, next) => Left(msg, next.pos.column)
    case exprParser.Error(msg, next)   => Left(msg, next.pos.column)
  }

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

  // </editor-fold>

  registerTerms("AbstractLanguage", List(UnknownType, TypeContainer))
}
