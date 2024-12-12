package languages.terms.literals

import scala.util.matching.Regex

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

