package languages.terms.builders

import languages.terms.values.Value

class ValueCompanion extends BuilderCompanion[Value] {
  /** Value builders are not currently used, so this method always returns None.
   */
  override final def create(args: BuilderArgs): Option[Value] = None
}
