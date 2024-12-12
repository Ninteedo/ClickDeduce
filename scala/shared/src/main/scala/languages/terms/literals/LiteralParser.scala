package languages.terms.literals

import app.UtilityFunctions

import scala.util.parsing.combinator.JavaTokenParsers

trait LiteralParser extends JavaTokenParsers {
  def literalName: Parser[String] =
    "LiteralString" | "LiteralInt" | "LiteralBool" | "LiteralIdentifierBind" | "LiteralIdentifierLookup" |
      "LiteralAny"

  def literalArg: Parser[BigInt | String | Boolean] = wholeNumber ^^ (n => BigInt(n))
    | "true" ^^ (_ => true) | "false" ^^ (_ => false)
    | stringLiteral ^^ (s => UtilityFunctions.unquote(s))
    | ident // any identifier
    | ""

  def literal: Parser[Literal] = literalName ~ "(" ~ literalArg ~ ")" ^^ {
    case name ~ "(" ~ arg ~ ")" =>
      (name, arg) match {
        case ("LiteralInt", n: BigInt)              => LiteralInt(n)
        case ("LiteralString", s: String)           => LiteralString(s)
        case ("LiteralBool", b: Boolean)            => LiteralBool(b)
        case ("LiteralIdentifierBind", s: String)   => LiteralIdentifierBind(s)
        case ("LiteralIdentifierLookup", s: String) => LiteralIdentifierLookup(s)
        case ("LiteralAny", s: String)              => LiteralAny(s)
        case _                                      => throw LiteralParseException(s"$name($arg)")
      }
    case other => throw LiteralParseException(other.toString)
  }
}
