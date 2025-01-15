package languages

import languages.previews.RulePreview
import languages.terms.Term
import languages.terms.builders.*
import languages.terms.exprs.Expr
import languages.terms.types.{Type, TypeContainer, UnknownType}
import languages.terms.values.Value

import scala.collection.immutable.List

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

  def parseExpr(exprText: String): Option[Expr]

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
