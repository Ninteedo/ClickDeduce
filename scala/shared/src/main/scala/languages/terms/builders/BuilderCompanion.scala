package languages.terms.builders

/** Companion object for terms, to be extended by the term companion objects.
 */
trait BuilderCompanion[T] {

  /** Whether the term should be hidden from the user.
   *
   * If true, the term will not appear in the list of available terms.
   *
   * Default is false.
   */
  val isHidden: Boolean = false

  /** The name of the term.
   *
   * By default, this is the name of the companion object, but can be overridden.
   */
  val name: String = getClass.getSimpleName.takeWhile(_ != '$')

  /** The arguments for the default term builder case.
   *
   * Equivalent to `Nil`.
   */
  protected final val defaultArgs: List[Any] = Nil

  /** List of alternate names for the term.
   */
  val aliases: List[String] = Nil

  /** Create a term from a list of arguments.
   *
   * Typically needs to handle 3 cases:
   *   - [[defaultArgs]]: The default version of the term, with unselected sub-terms and empty literals.
   *   - Arguments matching the expected structure for this term: The actual term.
   *   - Any other arguments: Invalid arguments; should return None.
   *
   * @param args
   * The arguments.
   * @return
   * Some expression, or None if the arguments are invalid.
   */
  def create(args: BuilderArgs): Option[T]
}
