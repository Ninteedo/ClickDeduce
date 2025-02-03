package languages

import convertors.*
import convertors.text.*
import languages.env.*
import languages.env.Env.Variable
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
    override protected def evalInner(env: ValueEnv): Value = NilV(elTyp.typeCheck(TypeEnv.fromValueEnv(env)))

    override protected def typeCheckInner(tEnv: TypeEnv): Type = ListType(elTyp.typeCheck(tEnv))

    override def toText: ConvertableText = MultiElement(nilSymbol, SquareBracketedElement(elTyp.toText))

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
    def apply(list: Expr, nilCase: Expr, headVar: Variable, tailVar: Variable, consCase: Expr): CaseList =
      CaseList(list, nilCase, LiteralIdentifierBind(headVar), LiteralIdentifierBind(tailVar), consCase)

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

  setTasks(CreateAListTask, MapIntListFunctionTask, PolyFilterFunctionTask)

  private def listOf(elems: List[Expr], elType: Type): Expr = elems match {
    case Nil          => ListNil(elType)
    case head :: tail => Cons(head, listOf(tail, elType))
  }

  private def listValueOf(elems: List[Value], elType: Type): Value = elems match {
    case Nil          => NilV(elType)
    case head :: tail => ConsV(head, listValueOf(tail, elType))
  }

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

  private object MapIntListFunctionTask extends Task {
    override val name: String = "Map Function for Integer Lists"

    override val description: String = "Implement the map function for lists of integers." +
      " The function should first take a function that maps an integer to another integer, and then a list of integers." +
      " It should have a signature of (Int → Int) → List[Int] → List[Int]."

    override val difficulty: Int = 3

    private val testCases: List[(Lambda, List[Int], List[Int])] = List(
      (Lambda("x", IntType(), Plus(Var("x"), Num(10))), List(4, 17, -237, 5661), List(14, 27, -227, 5671)),
      (Lambda("number", IntType(), Times(Var("number"), Var("number"))), List(1, 2, 3, 4, 5), List(1, 4, 9, 16, 25)),
      (Lambda("yep", IntType(), IfThenElse(Equal(Var("yep"), Num(42)), Num(1), Num(0))), List(42, 41, 42, 42, 43), List(1, 0, 1, 1, 0))
    )

    override def checkFulfilled(expr: Expr): Boolean = {
      def checkMap(expr: Expr): Boolean = checkCondition(
        expr,
        { (expr, env) =>
          testCases.forall((f, in, out) => {
            val mapExpr = Apply(Apply(expr, f), listOf(in.map(Num(_)), IntType()))
            !mapExpr.typeCheck(TypeEnv.fromValueEnv(env)).isError && mapExpr.eval(env) == listValueOf(out.map(NumV(_)), IntType())
          })
        },
        ValueEnv.empty
      )

      !expr.typeCheck().isError && checkMap(expr)
    }
  }

  object PolyFilterFunctionTask extends Task {
    override val name: String = "Polymorphic Filter Function"

    override val description: String = "Implement a polymorphic filter function for lists." +
      " The function should be a polymorphic abstraction that accepts a function that maps an element to a boolean, and a list of elements." +
      " It should have a signature of ΛT. (T → Bool) → List[T] → List[T]." +
      " The returned list should contain only the elements for which the function returns true, in the same order as in the input list."

    override val difficulty: Int = 4

    private val testCases: List[(Type, Lambda, Expr, Value)] = List(
      (
        IntType(),
        Lambda("x", IntType(), Equal(Var("x"), Num(475672))),
        listOf(List(Num(475672), Num(3847), Num(0), Num(475672)), IntType()),
        listValueOf(List(NumV(475672), NumV(475672)), IntType())
      ),
      (
        IntType(),
        Lambda("y", IntType(), LessThan(Var("y"), Num(0))),
        listOf(List(Num(-1), Num(0), Num(1), Num(-2), Num(2)), IntType()),
        listValueOf(List(NumV(-1), NumV(-2)), IntType())
      ),
      (
        PairType(IntType(), IntType()),
        Lambda("p", PairType(IntType(), IntType()), Equal(Fst(Var("p")), Snd(Var("p")))),
        listOf(List(
          Pair(Num(1), Num(1)),
          Pair(Num(1), Num(2)),
          Pair(Num(2), Num(2)),
          Pair(Num(2), Num(1)),
          Pair(Num(3), Num(3)),
          Pair(Num(3), Num(4)),
          Pair(Num(4), Num(3)),
          Pair(Num(4), Num(4)),
          Pair(Num(5), Num(5)),
          Pair(Num(5), Num(6)),
          Pair(Num(6), Num(5)),
          Pair(Num(6), Num(6))),
          PairType(IntType(), IntType())),
        listValueOf(List(
          PairV(NumV(1), NumV(1)),
          PairV(NumV(2), NumV(2)),
          PairV(NumV(3), NumV(3)),
          PairV(NumV(4), NumV(4)),
          PairV(NumV(5), NumV(5)),
          PairV(NumV(6), NumV(6))),
          PairType(IntType(), IntType()))
      )
    )

    override def checkFulfilled(expr: Expr): Boolean = {
      def checkFilter(expr: Expr): Boolean = checkCondition(
        expr,
        { (expr, env) =>
          testCases.forall((typ, f, in, out) => {
            val filterExpr = Apply(Apply(ApplyType(expr, typ), f), in)
            val typeCheckResult = filterExpr.typeCheck(TypeEnv.fromValueEnv(env))
            val evalResult = filterExpr.eval(env)
            println(expr)
            println(typeCheckResult)
            println(!typeCheckResult.isError)
            println(evalResult)
            println(evalResult == out)
            !filterExpr.typeCheck(TypeEnv.fromValueEnv(env)).isError && filterExpr.eval(env) == out
          })
        },
        ValueEnv.empty
      )

      !expr.typeCheck().isError && checkFilter(expr)
    }
  }

  // parser

  protected class LListParser extends LPolyParser {
    override protected def keywords: Set[String] = super.keywords ++ Set("list", "caselist", "nil", "of")

    private val nilRegex = "(?i)nil".r

    private def nil: Parser[ListNil] = nilRegex ~> ("[" ~> typ <~ "]") ^^ (ListNil(_)) |
      (nilRegex ~> (":" ~> typ) ^^ (ListNil(_))) |
      nilRegex ^^ (_ => ListNil(defaultType))

    private def caseList: Parser[CaseList] = "case" ~ "list" ~> expr ~ ("of" ~ "{" ~ "(?i)nil".r ~ ("=>" | "⇒") ~> expr) ~
      ";" ~ ident ~ "::" ~ ident ~ ("=>" | "⇒") ~ expr <~ "}" ^^ {
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
