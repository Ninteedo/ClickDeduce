package languages.env

import languages.terms.types.{Type, TypeContainer}
import languages.terms.values.{TypeValueContainer, Value}

type TypeEnv = Env[Type]

/** Companion object for [[TypeEnv]].
  */
object TypeEnv {

  /** An empty value environment.
    */
  val empty = new TypeEnv(Map())

  /** Convert a value environment to a type environment.
    *
    * @param env
    *   The value environment.
    * @return
    *   The type environment.
    */
  def fromValueEnv(env: ValueEnv): TypeEnv = env.mapToEnv((k: String, v: Value) => (k, v.typ))

  /** Convert a value or type environment to a type environment that only contains type values and variables.
   *
   * @param env
   * The value or type environment.
   * @return
   * The type environment.
   */
  def typeVariableEnv(env: ValueEnv | TypeEnv): TypeEnv = {
    Env(env.env.collect({
      case (k, TypeValueContainer(t)) => (k, t)
      case (k, TypeContainer(t)) => (k, t)
    }))
  }
}
