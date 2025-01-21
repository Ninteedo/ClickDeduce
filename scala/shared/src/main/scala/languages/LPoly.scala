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
import languages.terms.types.*
import languages.terms.values.*
import scalatags.Text.all.*

class LPoly extends LData {
  registerTerms("LPoly", List(Poly, ApplyType, TypeVar, PolyType, PolyV))

  // expressions

  private def formatPoly(v: ConvertableText, e: ConvertableText): ConvertableText =
    MultiElement(Symbols.lambdaUpper, v, MathElement.period, e)

  case class Poly(v: LiteralIdentifierBind, e: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = PolyV(TypeVar(v), e, env)

    override def typeCheckInner(tEnv: TypeEnv): Type =
      PolyType(TypeVar(v), e.typeCheck(tEnv + (v -> TypeContainer(TypeVar(v)))))

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] =
      List((v, env), (e, env + (v -> TypeValueContainer(TypeVar(v)))))

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] =
      List((v, tEnv), (e, tEnv + (v -> TypeContainer(TypeVar(v)))))

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] =
      List((e, env + (v -> TypeValueContainer(TypeVar(v)))))

    override def toText: ConvertableText = formatPoly(v.toText, e.toTextBracketed)
  }

  object Poly extends ExprCompanion {
    def apply(v: Variable, e: Expr): Poly = Poly(LiteralIdentifierBind(v), e)

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(v: LiteralIdentifierBind, e: Expr) => Some(Poly(v, e))
      case Nil                                     => Some(Poly(LiteralIdentifierBind.default, defaultExpr))
      case _                                       => None
    }

    override val aliases: List[String] = List("Polymorphic", "PolyType")

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .setConclusion(
            formatPoly(TermCommons.A, TermCommons.e),
            formatPolyType(TermCommons.A, TermCommons.t)
          )
          .addAssumption(TermCommons.e, TermCommons.t)
          .addAssumption(TypeCheckRulePart(MultiElement(TermCommons.A, MathElement("#").spacesAround, Symbols.gamma)))
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(EvalRulePart.reflexive(formatPoly(TermCommons.A, TermCommons.e)))
      )
      .buildOption
  }

  private def formatApplyType(e: ConvertableText, t: ConvertableText): ConvertableText =
    MultiElement(e, SquareBracketedElement(t))

  case class ApplyType(e: Expr, typ: Type) extends Expr {
    override def evalInner(env: ValueEnv): Value = e.eval(env) match {
      case PolyV(tv, e, env) => e.eval(env + (tv.v.toBind -> TypeValueContainer(typ)))
      case other => CannotApplyTypeUnlessPolyV(other)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = e.typeCheck(tEnv) match {
      case PolyType(tv, incompleteType) => incompleteType.typeCheck(tEnv + (tv.v.toBind -> typ))
      case other => CannotApplyTypeUnlessPolyType(other)
    }

    override def toText: ConvertableText = formatApplyType(e.toTextBracketed, typ.toText)
  }

  object ApplyType extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e: Expr, t: Type) => Some(ApplyType(e, t))
      case Nil                    => Some(ApplyType(defaultExpr, defaultType))
      case _                      => None
    }

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .setConclusion(
            formatApplyType(TermCommons.e, TermCommons.t(0)),
            MultiElement(TermCommons.t, EvalSubst(TermCommons.t(0), TermCommons.A))
          )
          .addAssumption(TermCommons.e, formatPoly(TermCommons.A, TermCommons.t))
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(formatApplyType(TermCommons.e, TermCommons.t), TermCommons.v)
          .addAssumption(TermCommons.e, formatPoly(TermCommons.A, TermCommons.e(0)))
          .addAssumption(TermCommons.e(0), TermCommons.v, List(EvalRuleBind(TermCommons.A, TermCommons.t)))
      )
      .buildOption
  }

  // types

  case class TypeVar(v: LiteralIdentifierLookup) extends Type {
    override def typeCheck(tEnv: TypeEnv): Type = guardValidIdentifierType(
      v,
      tEnv.get(v) match {
        case None              => UnknownTypeVar(v)
        case Some(TypeVar(t))  => TypeVar(t)
        case Some(other: Type) => other.typeCheck(tEnv)
      }
    )

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = v.toText
  }

  object TypeVar extends TypeCompanion {
    def apply(v: Variable): TypeVar = TypeVar(LiteralIdentifierLookup(v))

    def apply(v: LiteralIdentifierBind): TypeVar = TypeVar(v.toLookup)

    override def create(args: BuilderArgs): Option[Type] = args match {
      case List(v: LiteralIdentifierLookup) => Some(TypeVar(v))
      case Nil                              => Some(TypeVar(LiteralIdentifierLookup.default))
      case _                                => None
    }
  }

  private def formatPolyType(v: ConvertableText, e: ConvertableText): ConvertableText =
    MultiElement(Symbols.forall, v, MathElement.period, e)

  case class PolyType(typeVar: TypeVar, incompleteType: Type) extends Type {
    override def toText: ConvertableText = formatPoly(typeVar.toTextBracketed, incompleteType.toTextBracketed)

    override def typeCheck(tEnv: TypeEnv): Type =
      PolyType(typeVar, incompleteType.typeCheck(tEnv + (typeVar.v.toBind -> TypeContainer(typeVar))))

    override val isError: Boolean = typeVar.isError || incompleteType.isError
  }

  object PolyType extends TypeCompanion {
    override def create(args: BuilderArgs): Option[Type] = args match {
      case List(tv: TypeVar, t: Type) => Some(PolyType(tv, t))
      case Nil                     => Some(PolyType(TypeVar(""), defaultType))
      case _                       => None
    }

    override val isHidden: Boolean = true
  }

  // values

  case class PolyV(typeVar: TypeVar, e: Expr, env: ValueEnv) extends Value {
    override val typ: Type = PolyType(typeVar, e.typeCheck(TypeEnv.fromValueEnv(env) + (typeVar.v.toBind -> TypeContainer(typeVar))))

    override def toText: ConvertableText = MultiElement(
      Symbols.lambdaUpper,
      typeVar.toTextBracketed,
      SpaceAfter(MathElement.period),
      e.toTextBracketed
    )
  }

  object PolyV extends ValueCompanion {}

  // errors

  case class CannotApplyTypeUnlessPolyV(v: Value) extends EvalError {
    override val message: String = s"Cannot apply type to non-polymorphic value $v"

    override val typ: Type = CannotApplyTypeUnlessPolyType(v.typ)
  }

  case class CannotApplyTypeUnlessPolyType(t: Type) extends TypeError {
    override val message: String = s"Cannot apply type to non-polymorphic type $t"
  }

  case class PolyVRequiresTypeVar(v: Type) extends EvalError {
    override val message: String = s"Polymorphic value requires a type variable, not $v"

    override val typ: Type = PolyVRequiresTypeVarType(v)
  }

  case class PolyVRequiresTypeVarType(t: Type) extends TypeError {
    override val message: String = s"Polymorphic value requires a type variable, not $t"
  }

  case class UnknownTypeVar(v: Literal) extends TypeError {
    override val message: String = s"Unknown type variable $v"
  }

  // tasks
  setTasks(CreatePolyFunctionTask)

  object CreatePolyFunctionTask extends Task {
    override val name: String = "Create a Polymorphic Function"
    override val description: String =
      "Use the Poly expression to create a type variable which is then used as the input type for a lambda function." +
        " The function must use the input variable." +
        " It must successfully type-check."
    override val difficulty: Int = 3

    override def checkFulfilled(expr: Expr): Boolean = !expr.typeCheck().isError && checkCondition(
      expr,
      (e, env) =>
        e match {
          case Poly(typVar, e) =>
            checkCondition(
              e,
              (e, env) =>
                e match {
                  case Lambda(lamVar, lamTyp, e) =>
                    checkCondition(
                      lamTyp,
                      (t, env) =>
                        t match {
                          case TypeVar(v) => v.identEquals(typVar)
                          case _          => false
                        },
                      TypeEnv.fromValueEnv(env)
                    ) && checkCondition(
                      e,
                      (e, env) =>
                        e match {
                          case Var(v) => v.identEquals(lamVar)
                          case _      => false
                        },
                      env
                    )
                  case _ => false
                },
              env
            )
          case _ => false
        },
      ValueEnv.empty
    )
  }

  protected class LPolyParser extends LDataParser {

  }

  override protected val exprParser: ExprParser = new LPolyParser
}

object LPoly extends LPoly {}
