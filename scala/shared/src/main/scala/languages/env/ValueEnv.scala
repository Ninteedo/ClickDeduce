package languages.env

import languages.terms.values.Value

type ValueEnv = Env[Value]

/** Companion object for [[ValueEnv]].
 */
object ValueEnv {

  /** An empty value environment.
   */
  val empty = new ValueEnv(Map())
}
