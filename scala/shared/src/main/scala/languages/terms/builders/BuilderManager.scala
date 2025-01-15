package languages.terms.builders

import languages.terms.Term
import languages.terms.literals.Literal

type BuilderArgs = List[Literal | Term]

type BuilderName = (String, String | (String, List[String])) // langName, then either name or (name, aliases)


/** Manages the defined builders for a particular type of term.
 * @tparam T
 *   The type of term.
 */
class BuilderManager[T <: Term, C <: BuilderCompanion[T]] {
  private var companions: Map[String, C] = Map()
  private var builderNamesList: List[BuilderName] = List()

  protected def getCompanion(name: String): Option[C] = companions.get(name)

  /** Get a builder by name.
   * @param name
   *   The name of the builder. Does not match aliases.
   * @return
   *   Some builder, or None if not found.
   */
  def getBuilder(name: String): Option[BuilderArgs => Option[T]] = getCompanion(name).map(c => c.create)

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

  def register(c: C, langName: String): Unit = {
    companions += (c.name -> c)
    if (!c.isHidden) {
      val entry = if c.aliases.isEmpty then c.name else (c.name, c.aliases)
      builderNamesList = builderNamesList :+ (langName, entry)
    }
  }
}
