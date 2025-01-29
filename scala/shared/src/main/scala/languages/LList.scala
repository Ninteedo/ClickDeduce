package languages

import convertors.*
import convertors.text.*
import languages.env.*
import languages.previews.*
import languages.terms.*
import languages.terms.builders.*
import languages.terms.errors.*
import languages.terms.exprs.Expr
import languages.terms.literals.*
import languages.terms.types.Type
import languages.terms.values.Value

class LList extends LPoly {
  registerTerms("LList", List(ListNil, Cons, CaseList, ListType, NilV, ConsV))

  private val nilSymbol = TextElement("Nil")

  private val consSymbol = TextElement("::")

  private def listTypeText(elTyp: ConvertableText): ConvertableText = MultiElement(TextElement("List"), SquareBracketedElement(elTyp))

  // expressions
  case class ListNil(elTyp: Type) extends Expr {
    override protected def evalInner(env: ValueEnv): Value = NilV(elTyp)

    override protected def typeCheckInner(tEnv: TypeEnv): Type = ListType(elTyp)

    override def toText: ConvertableText = nilSymbol

    override val needsBrackets: Boolean = false
  }

  object ListNil extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(elTyp: Type) => Some(ListNil(elTyp))
      case Nil               => Some(ListNil(defaultType))
      case _                 => None
    }

    override val aliases: List[String] = List("ListNil")

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .setConclusion(TypeCheckRulePart(nilSymbol, listTypeText(Symbols.tau)))
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(EvalRulePart.reflexive(nilSymbol))
      )
      .buildOption
  }

  private def formatCons(head: ConvertableText, tail: ConvertableText): ConvertableText =
    MultiElement(head, consSymbol.spacesAround, tail)

  case class Cons(head: Expr, tail: Expr) extends Expr {
    override protected def evalInner(env: ValueEnv): Value = (head.eval(env), tail.eval(env)) match {
      case (headV, _) if headV.typ.isError => headV
      case (_, tailV) if tailV.typ.isError => tailV
      case (headV, tailV)                  => ConsV(headV, tailV)
    }

    override protected def typeCheckInner(tEnv: TypeEnv): Type = (head.typeCheck(tEnv), tail.typeCheck(tEnv)) match {
      case (headTyp, _) if headTyp.isError              => headTyp
      case (_, tailTyp) if tailTyp.isError              => tailTyp
      case (elTyp, ListType(elTyp2)) if elTyp == elTyp2 => ListType(elTyp)
      case (headTyp, tailTyp)                           => ListTypeMismatchError(headTyp, tailTyp)
    }

    override def toText: ConvertableText = formatCons(head.toTextBracketed, tail.toTextBracketed)
  }

  object Cons extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(head: Expr, tail: Expr) => Some(Cons(head, tail))
      case Nil                          => Some(Cons(defaultExpr, defaultExpr))
      case _                            => None
    }

    override val aliases: List[String] = List("ListCons", "::")

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(TypeCheckRuleBuilder()
        .setConclusion(TypeCheckRulePart(
          formatCons(TermCommons.e(1), TermCommons.e(2)),
          listTypeText(Symbols.tau)
        )
        )
        .addAssumption(TypeCheckRulePart(TermCommons.e(1), Symbols.tau))
        .addAssumption(TypeCheckRulePart(TermCommons.e(2), listTypeText(Symbols.tau)))
      )
      .addEvaluationRule(EvalRuleBuilder()
        .setConclusion(formatCons(TermCommons.e(1), TermCommons.e(2)), formatCons(TermCommons.v(1), TermCommons.v(2)))
        .addAssumption(EvalRulePart.eToV(1))
        .addAssumption(EvalRulePart.eToV(2))
      )
      .buildOption
  }

  private def formatCaseList(list: ConvertableText, nilCase: ConvertableText, headVar: ConvertableText, tailVar: ConvertableText, consCase: ConvertableText): ConvertableText =
    MultiElement(
      TextElement("case"),
      SpaceAfter(SubscriptElement(TextElement("list"))),
      list,
      TextElement(" of { "),
      nilSymbol,
      SurroundSpaces(Symbols.doubleRightArrow),
      nilCase,
      TextElement("; "),
      headVar,
      consSymbol.spacesAround,
      tailVar,
      SurroundSpaces(Symbols.doubleRightArrow),
      consCase,
      TextElement(" }")
    )


  case class CaseList(
    list: Expr,
    nilCase: Expr,
    headVar: LiteralIdentifierBind,
    tailVar: LiteralIdentifierBind,
    consCase: Expr
  ) extends Expr {
    override protected def evalInner(env: ValueEnv): Value = list.eval(env) match {
      case NilV(_)           => nilCase.eval(env)
      case ConsV(head, tail) => consCase.eval(consEnv(env, head, tail))
      case v                 => ListCaseNotListError(v)
    }

    override protected def typeCheckInner(tEnv: TypeEnv): Type = list.typeCheck(tEnv) match {
      case ListType(elTyp) =>
        (nilCase.typeCheck(tEnv), consCase.typeCheck(consTEnv(tEnv, elTyp))) match {
          case (nilTyp, _) if nilTyp.isError   => nilTyp
          case (_, consTyp) if consTyp.isError => consTyp
          case (nilTyp, consTyp) =>
            if nilTyp.typeCheck(tEnv) != consTyp.typeCheck(tEnv)
            then ListTypeMismatchError(nilTyp, consTyp)
            else nilTyp.typeCheck(tEnv)
        }
      case t => ListCaseNotListTypeError(t)
    }

    private def consEnv(env: ValueEnv, head: Value, tail: Value): ValueEnv =
      env + (headVar -> head) + (tailVar -> tail)
    private def consTEnv(tEnv: TypeEnv, elTyp: Type): TypeEnv =
      tEnv + (headVar -> elTyp) + (tailVar -> ListType(elTyp))

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] = List(
      (list, env),
      (nilCase, env),
      (
        consCase,
        list.eval(env) match {
          case ConsV(head, tail) => consEnv(env, head, tail)
          case _ =>
            list.typeCheck(TypeEnv.fromValueEnv(env)) match {
              case ListType(elTyp) => consEnv(env, HiddenValue(elTyp), HiddenValue(ListType(elTyp)))
              case _               => env
            }
        }
      )
    )

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = List(
      (list, tEnv),
      (nilCase, tEnv),
      (
        consCase,
        list.typeCheck(tEnv) match {
          case ListType(elTyp) => consTEnv(tEnv, elTyp)
          case _               => tEnv
        }
      )
    )

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] = List(
      (list, env),
      list.eval(env) match {
        case ConsV(head, tail) => (consCase, consEnv(env, head, tail))
        case NilV(_)           => (nilCase, env)
        case _                 => (nilCase, env)
      }
    )

    override def toText: ConvertableText = formatCaseList(list.toTextBracketed, nilCase.toTextBracketed, headVar.toText, tailVar.toText, consCase.toTextBracketed)
  }

  object CaseList extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(
            list: Expr,
            nilCase: Expr,
            headVar: LiteralIdentifierBind,
            tailVar: LiteralIdentifierBind,
            consCase: Expr
          ) =>
        Some(CaseList(list, nilCase, headVar, tailVar, consCase))
      case Nil =>
        Some(
          CaseList(defaultExpr, defaultExpr, LiteralIdentifierBind.default, LiteralIdentifierBind.default, defaultExpr)
        )
      case _ => None
    }

    override val aliases: List[String] = List("ListCase")

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .setConclusion(TypeCheckRulePart(
            formatCaseList(TermCommons.e, TermCommons.e(1), TermCommons.x, TermCommons.y, TermCommons.e(2)),
            TermCommons.t(2)
          )
          )
          .addAssumption(TypeCheckRulePart(TermCommons.e, listTypeText(TermCommons.t(1))))
          .addAssumption(TypeCheckRulePart(TermCommons.e(1), TermCommons.t(2)))
          .addAssumption(
            TypeCheckRulePart(
              TermCommons.e(2), TermCommons.t(2), List(
                TypeCheckRuleBind(TermCommons.x, TermCommons.t(1)),
                TypeCheckRuleBind(TermCommons.y, listTypeText(TermCommons.t(1)))
              )
            ),
          )
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(EvalRulePart(
            formatCaseList(TermCommons.e, TermCommons.e(1), TermCommons.x, TermCommons.y, TermCommons.e(2)),
            TermCommons.v(1)
          )
          )
          .addAssumption(EvalRulePart(TermCommons.e, nilSymbol))
          .addAssumption(EvalRulePart(TermCommons.e(1), TermCommons.v(1)))
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(EvalRulePart(
            formatCaseList(TermCommons.e, TermCommons.e(1), TermCommons.x, TermCommons.y, TermCommons.e(2)),
            TermCommons.v(2)
          )
          )
          .addAssumption(EvalRulePart(TermCommons.e, formatCons(TermCommons.v(1), TermCommons.v(2))))
          .addAssumption(EvalRulePart(
            TermCommons.e(2),
            TermCommons.v(2),
            List(EvalRuleBind(TermCommons.x, TermCommons.v(1)), EvalRuleBind(TermCommons.y, TermCommons.v(2)))
          ))
      )
      .buildOption
  }

  // types
  case class ListType(elTyp: Type) extends Type {
    override def typeCheck(tEnv: TypeEnv): Type = ListType(elTyp.typeCheck(tEnv))

    override def toText: ConvertableText = MultiElement(TextElement("List"), SquareBracketedElement(elTyp.toText))

    override val needsBrackets: Boolean = false

    override val isError: Boolean = elTyp.isError
  }

  object ListType extends TypeCompanion {
    override def create(args: BuilderArgs): Option[Type] = args match {
      case List(elTyp: Type) => Some(ListType(elTyp))
      case Nil               => Some(ListType(defaultType))
      case _                 => None
    }
  }

  // values
  trait ListValue extends Value {
    def elems: List[Value]
  }

  case class NilV(elTyp: Type) extends ListValue {
    override def toText: ConvertableText = TextElement("[]")

    override val typ: Type = ListType(elTyp)

    override val needsBrackets: Boolean = false

    override val isError: Boolean = elTyp.isError

    override val elems: List[Value] = List()
  }

  object NilV extends ValueCompanion {}

  case class ConsV(head: Value, tail: Value) extends ListValue {
    override def toText: ConvertableText = ListElement(elems.map(_.toText))

    override val typ: Type = ListType(head.typ)

    override val isError: Boolean = head.isError || tail.isError

    override def elems: List[Value] = tail match {
      case l: ListValue => head :: l.elems
      case _            => List(head)
    }
  }

  object ConsV extends ValueCompanion {}

  // exceptions
  case class ListTypeMismatchError(init: Type, tail: Type) extends TypeError {
    override val message: String = s"Expected matching list types, but got $init and $tail"
  }

  case class ListCaseNotListError(v: Value) extends EvalError {
    override val message: String = s"ListCase expected a list, but got $v"

    override val typ: Type = ListCaseNotListTypeError(v.typ)
  }

  case class ListCaseNotListTypeError(t: Type) extends TypeError {
    override val message: String = s"ListCase expected a list, but got $t"
  }

  // tasks

  setTasks(CreateAListTask)

  private object CreateAListTask extends Task {
    override val name: String = "Create a multi-element list"

    override val description: String = "Create a list of any type with at least two elements, using Cons and ListNil." +
      " The elements must be of matching types. The type of nil has to be explicitly specified."

    override val difficulty: Int = 2

    override def checkFulfilled(expr: Expr): Boolean = {
      def listLength(expr: Expr): Option[Int] = expr match {
        case Cons(_, tail) => tail match {
          case e: Cons => listLength(e).map(_ + 1)
          case e: ListNil => Some(1)
          case _ => None
        }
        case ListNil(_) => Some(0)
        case _ => None
      }

      def checkList(expr: Expr): Boolean = checkCondition(
        expr,
        cond = {
          case e: Cons => listLength(e).exists(_ >= 2)
          case _ => false
        }
      )

      !expr.typeCheck().isError && checkList(expr)
    }
  }

  // parser

  protected class LListParser extends LPolyParser {
    private val nilRegex = "(?i)nil".r

    private def nil: Parser[ListNil] = nilRegex ~> ("[" ~> typ <~ "]") ^^ (ListNil(_)) |
      (nilRegex ~> (":" ~> typ) ^^ (ListNil(_))) |
      nilRegex ^^ (_ => ListNil(defaultType))

    private def caseList: Parser[CaseList] = "case" ~ "list" ~> expr ~ ("of" ~ "{" ~ "(?i)nil".r ~ "=>" ~> expr) ~
      ";" ~ ident ~ "::" ~ ident ~ "=>" ~ expr <~ "}" ^^ {
      case list ~ nilCase ~ _ ~ headVar ~ _ ~ tailVar ~ _ ~ consCase =>
        CaseList(list, nilCase, LiteralIdentifierBind(headVar), LiteralIdentifierBind(tailVar), consCase)
    }

    override protected def exprOperators: List[ExprOperator] = super.exprOperators ++ List(
      BasicBinaryOperator("::", Cons.apply, 1, Associativity.Right),
      SpecialParser(caseList, 1)
    )

    override protected def primitive: Parser[Expr] = nil | super.primitive

    override protected def typPrimitive: Parser[Type] = "(?i)list".r ~ "[" ~> typ <~ "]" ^^ (ListType(_)) | super.typPrimitive
  }

  override protected val exprParser: ExprParser = new LListParser
}

object LList extends LList {}
