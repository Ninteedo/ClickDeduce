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
import scalatags.Text
import scalatags.Text.all.*

class LRec extends LLam {
  registerTerms("LRec", List(Rec, RecV))

  private def formatRec(
    f: ConvertableText,
    v: ConvertableText,
    in: ConvertableText,
    out: ConvertableText,
    e: ConvertableText,
    hideTypes: Boolean = false
  ): ConvertableText =
    MultiElement(
      TextElement("rec "),
      f,
      BracketedElement(if hideTypes then v else MultiElement(v, MathElement.colon.spaceAfter, in)),
      if hideTypes then NullElement() else MultiElement(MathElement.colon.spaceAfter, out),
      SpaceAfter(MathElement.period),
      e
    )

  // expressions
  case class Rec(f: LiteralIdentifierBind, v: LiteralIdentifierBind, inType: Type, outType: Type, e: Expr)
      extends Expr {
    override def evalInner(env: ValueEnv): Value = guardValidIdentifierEval(
      f,
      guardValidIdentifierEval(
        v,
        RecV(f, v, inType.typeCheck(TypeEnv.fromValueEnv(env)), outType.typeCheck(TypeEnv.fromValueEnv(env)), e, env)
      )
    )

    override def typeCheckInner(tEnv: TypeEnv): Type = guardValidIdentifierType(
      f,
      guardValidIdentifierType(
        v, {
          val properInType = inType.typeCheck(tEnv)
          val properOutType = outType.typeCheck(tEnv)
          val extendedTEnv = tEnv + (f -> Func(properInType, properOutType)) + (v -> properInType)
          val determinedOutType = e.typeCheck(extendedTEnv).typeCheck(extendedTEnv)
          if (properOutType == determinedOutType) Func(properInType, determinedOutType)
          else RecursiveFunctionExpressionOutTypeMismatch(properOutType, determinedOutType)
        }
      )
    )

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] = List(
      (f, env),
      (v, env),
      (inType, env),
      (outType, env),
      (e, env + (f -> HiddenValue(Func(inType, outType))) + (v -> HiddenValue(inType)))
    )

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = List(
      (e, tEnv + (f -> Func(inType, outType)) + (v -> inType))
    )

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] = Nil

    override def toText: ConvertableText = formatRec(
      f.toText,
      v.toText,
      TypeElement(inType.toTextBracketed),
      TypeElement(outType.toTextBracketed),
      e.toTextBracketed
    )
  }

  object Rec extends ExprCompanion {
    def apply(f: String, v: String, in_typ: Type, out_typ: Type, e: Expr): Rec =
      Rec(LiteralIdentifierBind(f), LiteralIdentifierBind(v), in_typ, out_typ, e)

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(f: LiteralIdentifierBind, v: LiteralIdentifierBind, inType: Type, outType: Type, e: Expr) =>
        Some(Rec(f, v, inType, outType, e))
      case Nil =>
        Some(Rec(LiteralIdentifierBind.default, LiteralIdentifierBind.default, defaultType, defaultType, defaultExpr))
      case _ => None
    }

    override val aliases: List[String] = List("RecursiveFunction")

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .setConclusion(
            formatRec(TermCommons.f, TermCommons.x, TermCommons.t(1), TermCommons.t(2), TermCommons.e),
            formatFuncType(TermCommons.t(1), TermCommons.t(2))
          )
          .addAssumption(
            TermCommons.e, TermCommons.t(2), List(
              TypeCheckRuleBind(TermCommons.f, formatFuncType(TermCommons.t(1), TermCommons.t(2))),
              TypeCheckRuleBind(TermCommons.x, TermCommons.t(1))
            )
          )
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(EvalRulePart(
            formatRec(TermCommons.f, TermCommons.x, TermCommons.t(1), TermCommons.t(2), TermCommons.e, hideTypes = true),
            EvalRuleAbstraction(TermCommons.env, formatRec(TermCommons.f, TermCommons.x, TermCommons.t(1), TermCommons.t(2), TermCommons.e, hideTypes = true))
          ))
      )
      .buildOption
  }

  // values
  case class RecV(
    f: LiteralIdentifierBind,
    v: LiteralIdentifierBind,
    in_typ: Type,
    out_typ: Type,
    e: Expr,
    env: ValueEnv
  ) extends FunctionValue {
    override val typ: Type = Func(in_typ, out_typ)

    override def getFunctionEvaluation(applyValue: Value): (Expr, ValueEnv) =
      (e, env + (f -> this) + (v -> applyValue))

    override def evalApply(value: Value): Value = e.eval(env + (f -> this) + (v -> value))

    override def toText: ConvertableText = MultiElement(
      TextElement("rec "),
      f.toText,
      BracketedElement(v.toText),
//      SpaceAfter(MathElement.colon),
//      TypeElement(out_typ.toTextBracketed),
//      SpaceAfter(MathElement.period),
//      e.toTextBracketed
    )
  }

  object RecV extends ValueCompanion {}

  private def prettyPrintRec(f: Literal, v: Literal, in_typ: Type, out_typ: Type, e: Expr) =
    s"rec $f($v: ${in_typ.prettyPrint}): ${out_typ.prettyPrintBracketed}. ${e.prettyPrintBracketed}"

  class RecursiveFunctionExpressionOutTypeMismatch(declared: Type, actual: Type) extends TypeError {
    override val message: String =
      s"Recursive function expression declared return type $declared does not match actual return type $actual"
  }

  // tasks

  setTasks(ImplementRecursiveFunctionTask, ImplementFactorialFunctionTask)

  private object ImplementRecursiveFunctionTask extends Task {
    override val name: String = "Implement a Recursive Function"
    override val description: String = "Implement a recursive function that calls itself"
    override val difficulty: Int = 3

    override def checkFulfilled(expr: Expr): Boolean = {
      checkCondition(
        expr,
        {
          case Rec(f, v, inType, outType, e) =>
            checkCondition(
              e,
              {
                case Apply(l, r) =>
                  checkCondition(
                    l,
                    {
                      case Var(f) => true
                      case _      => false
                    }
                  )
                case _ => false
              }
            )
          case _ => false
        }
      )
    }
  }

  private object ImplementFactorialFunctionTask extends Task {
    override val name: String = "Implement the Factorial Function"
    override val description: String =
      "Implement the recursive factorial function. It should return 1 for n=0, and is not required to handle negative" +
        " numbers." +
        " The function name does not matter. The expression must successfully type-check."
    override val difficulty: Int = 5

    override def checkFulfilled(expr: Expr): Boolean = {
      val factorialTable = Map(0 -> 1, 1 -> 1, 2 -> 2, 3 -> 6, 4 -> 24, 5 -> 120, 6 -> 720)

      !expr.typeCheck().isError && checkCondition(
        expr,
        { (expr, env) =>
          expr match {
            case f: Rec => factorialTable.forall((n, f_n) => Apply(f, Num(n)).eval(env) == NumV(f_n))
            case _      => false
          }
        },
        ValueEnv.empty
      )
    }
  }

  protected class LRecParser extends LLamParser {
    protected def rec: Parser[Rec] = "rec" ~> ident ~ ("(" ~> ident <~ ":") ~ typ ~ ")" ~ ":" ~ typ ~ "." ~ expr ^^ {
      case f ~ v ~ inType ~ _ ~ _ ~ outType ~ _ ~ e => Rec(f, v, inType, outType, e)
    }

    override protected def primitive: Parser[Expr] = rec | super.primitive
  }

  override protected val exprParser: ExprParser = new LRecParser
}

object LRec extends LRec {}
