package languages.env

import languages.terms.literals.{LiteralIdentifier, LiteralIdentifierBind, LiteralIdentifierLookup}

import scala.annotation.targetName

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
