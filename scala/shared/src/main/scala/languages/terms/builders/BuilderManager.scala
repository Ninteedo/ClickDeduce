package languages.terms.builders

import languages.terms.Term
import languages.terms.literals.Literal

type BuilderArgs = List[Literal | Term]

type BuilderName = (String, String | (String, List[String])) // langName, then either name or (name, aliases)


/** Manages the defined builders for a particular type of term.
 * @tparam T
 *   The type of term.
 */
class BuilderManager[T <: Term] {
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
    case Some(builder) => builder.apply(args).getOrElse(throw TermBuilderFailed(name, args))
    case None          => throw UnknownTermBuilder(name)
  }

  /** Get the names of all builders.
   * @return
   *   The list of builder names.
   */
  def builderNames: List[BuilderName] = builderNamesList

  def register(c: BuilderCompanion[T], langName: String): Unit = {
    addBuilder(c.name, c.create, langName, c.isHidden, c.aliases)
  }
}
