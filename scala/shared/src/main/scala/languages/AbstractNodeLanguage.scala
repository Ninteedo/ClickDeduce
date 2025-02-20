package languages

import convertors.*
import languages.terms.Term
import languages.terms.blanks.{BlankExprDropDown, BlankTypeDropDown}
import languages.terms.builders.BuilderName
import languages.terms.exprs.Expr
import languages.terms.literals.{Literal, LiteralParser}
import languages.terms.types.Type
import nodes.exceptions.NodeStringParseException
import scalatags.Text.TypedTag
import scalatags.Text.all.*

import scala.util.parsing.combinator.*

/** Defines the abstract syntax tree structure.
 *
 * The abstract [[nodes.Node]] class is the parent for all tree nodes.
 */
trait AbstractNodeLanguage extends AbstractLanguage {
  lang =>

  /** The HTML for the expression class list dropdown.
   *
   * A select element with an option for each expression builder.
   */
  lazy val exprClassListDropdownHtml: TypedTag[String] = {
    def createExprOption(exprBuilderName: BuilderName): TypedTag[String] = {
      val langName = exprBuilderName._1

      exprBuilderName._2 match {
        case name: String => option(data("value") := name, name)
        case (name: String, aliases: List[String]) =>
          option(data("value") := name, data("aliases") := aliases.mkString(","), data("lang") := langName, name)
      }
    }

    select(
      cls := ClassDict.EXPR_DROPDOWN,
      option(value := "", "Select Expr..."),
      exprBuilderNames.map(createExprOption),
    )
  }

  /** The HTML for the type class list dropdown.
   *
   * A select element with an option for each type builder.
   */
  lazy val typeClassListDropdownHtml: TypedTag[String] = {
    def createTypeOption(typeBuilderName: BuilderName) = {
      val langName = typeBuilderName._1
      typeBuilderName._2 match {
        case name: String => option(data("value") := name, name)
        case (name: String, aliases: List[String]) =>
          option(data("value") := name, name, data("aliases") := aliases.mkString(","))
      }
    }

    select(
      cls := ClassDict.TYPE_DROPDOWN,
      option(value := "", "Select Type..."),
      typeBuilderNames.map(createTypeOption),
    )
  }

  /** Default expression used where an expression needs to be filled in.
   *
   * @see [[BlankExprDropDown]]
   */
  val defaultExpr: Expr = BlankExprDropDown(lang)

  /** Default type used where a type needs to be filled in.
   *
   * @see [[BlankTypeDropDown]]
   */
  val defaultType: Type = BlankTypeDropDown(lang)

  /** Load an `Expr` from a string.
   *
   * Input must be in the format produced by `Expr.toString`
   *
   * @param s The string to be parsed.
   * @return The `Expr` represented by the string, if successful.
   */
  def readExpr(s: String): Option[Expr] = parseTerm(s) match {
    case Some(e: Expr) => Some(e)
    case _ => None
  }

  def readType(s: String): Option[Type] = parseTerm(s) match {
    case Some(t: Type) => Some(t)
    case _ => None
  }

  /** Create a `Term` given its string representation.
   *
   * @return The `Term` created, if successful.
   */
  private def parseTerm(s: String): Option[Term] = {
    def makeTerm(name: String, args: List[Literal | Option[Term]]): Option[Term] = {
      val parsedArgs: List[Literal | Term] = args.map {
        case Some(e) => e
        case other: Literal => other
        case _ => throw NodeStringParseException(s"$name(${args.mkString(", ")})")
      }
      getExprBuilder(name) match {
        case Some(builder) => builder(parsedArgs)
        case None =>
          getTypeBuilder(name) match {
            case Some(builder) => builder(parsedArgs)
            case None => None
          }
      }
    }

    object TermParser extends JavaTokenParsers with LiteralParser {
      def term: Parser[Option[Term]] = name ~ "(" ~ repsep(arg, "\\s*,\\s*".r) ~ ")" ^^ {
        case name ~ "(" ~ args ~ ")" => makeTerm(name, args)
        case _ => None
      }

      def name: Parser[String] = "[A-Za-z]\\w*".r

      def identifier: Parser[String] = "[A-Za-z_$][\\w_$]*".r

      def arg: Parser[Literal | Option[Term]] = term | literal

      def parseTerm(s: String): ParseResult[Option[Term]] = parseAll(term, s.strip())
    }

    TermParser.parseTerm(s) match {
      case TermParser.Success(matched, _) => matched
      case x => None
    }
  }
}
