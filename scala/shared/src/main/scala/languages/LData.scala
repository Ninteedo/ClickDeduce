package languages

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
import languages.terms.values.Value

class LData extends LRec {
  registerTerms(
    "LData",
    List(
      Pair,
      Fst,
      Snd,
      LetPair,
      UnitExpr,
      Left,
      Right,
      CaseSwitch,
      PairType,
      UnionType,
      EmptyType,
      PairV,
      UnitV,
      LeftV,
      RightV
    )
  )

  // expressions

  private def formatPair(l: ConvertableText, r: ConvertableText): ConvertableText =
    BracketedElement(MultiElement(l, SpaceAfter(MathElement.comma), r))

  case class Pair(e1: Expr, e2: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = PairV(e1.eval(env), e2.eval(env))

    override def typeCheckInner(tEnv: TypeEnv): Type = PairType(e1.typeCheck(tEnv), e2.typeCheck(tEnv))

    override def toText: ConvertableText = formatPair(e1.toText, e2.toText)

    override val needsBrackets: Boolean = false
  }

  object Pair extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e1: Expr, e2: Expr) => Some(Pair(e1, e2))
      case Nil                      => Some(Pair(defaultExpr, defaultExpr))
      case _                        => None
    }

    override val aliases: List[String] = List("Tuple")

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .setConclusion(
            formatPair(TermCommons.e(1), TermCommons.e(2)), formatPairType(TermCommons.t(1), TermCommons.t(2))
          )
          .addAssumption(TypeCheckRulePart.eToT(1))
          .addAssumption(TypeCheckRulePart.eToT(2))
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(formatPair(TermCommons.e(1), TermCommons.e(2)), formatPair(TermCommons.v(1), TermCommons.v(2)))
          .addAssumption(EvalRulePart.eToV(1))
          .addAssumption(EvalRulePart.eToV(2))
      )
      .buildOption
  }

  private def formatFst(e: ConvertableText): ConvertableText = MultiElement(TextElement("fst").spaceAfter, e)

  case class Fst(e: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = e.eval(env) match {
      case PairV(v1, _) => v1
      case other        => TupleOperationOnNonTupleValue(other)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = e.typeCheck(tEnv) match {
      case PairType(l, _) => l
      case other          => TupleOperationOnNonTupleType(other)
    }

    override def toText: ConvertableText = formatFst(e.toTextBracketed)

    override val needsBrackets: Boolean = false
  }

  object Fst extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e: Expr) => Some(Fst(e))
      case Nil           => Some(Fst(defaultExpr))
      case _             => None
    }

    override val aliases: List[String] = List("First", "1st")

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .addAssumption(TermCommons.e, formatPairType(TermCommons.t(1), TermCommons.t(2)))
          .setConclusion(formatFst(TermCommons.e), TermCommons.t(1))
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .addAssumption(TermCommons.e, formatPair(TermCommons.v(1), TermCommons.v(2)))
          .setConclusion(formatFst(TermCommons.e), TermCommons.v(1))
      )
      .buildOption
  }

  private def formatSnd(e: ConvertableText): ConvertableText = MultiElement(TextElement("snd").spaceAfter, e)

  case class Snd(e: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = e.eval(env) match {
      case PairV(_, v2) => v2
      case other        => TupleOperationOnNonTupleValue(other)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = e.typeCheck(tEnv) match {
      case PairType(_, r) => r
      case other          => TupleOperationOnNonTupleType(other)
    }

    override def toText: ConvertableText = formatSnd(e.toTextBracketed)

    override val needsBrackets: Boolean = false
  }

  object Snd extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e: Expr) => Some(Snd(e))
      case Nil           => Some(Snd(defaultExpr))
      case _             => None
    }

    override val aliases: List[String] = List("Second", "2nd")

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .addAssumption(TermCommons.e, formatPairType(TermCommons.t(1), TermCommons.t(2)))
          .setConclusion(formatSnd(TermCommons.e), TermCommons.t(2))
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .addAssumption(TermCommons.e, formatPair(TermCommons.v(1), TermCommons.v(2)))
          .setConclusion(formatSnd(TermCommons.e), TermCommons.v(2))
      )
      .buildOption
  }

  private def formatLetPair(x: ConvertableText, y: ConvertableText, assign: ConvertableText, bound: ConvertableText): ConvertableText =
    MultiElement(
      SpaceAfter(TextElement("let pair")),
      formatPair(x, y),
      SurroundSpaces(MathElement.equals),
      assign,
      SurroundSpaces(TextElement("in")),
      bound
    )

  case class LetPair(x: LiteralIdentifierBind, y: LiteralIdentifierBind, assign: Expr, bound: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = verifyLiteralIdentifierEval(x, y) {
      assign.eval(env) match {
        case PairV(v1, v2)    => bound.eval(env + (x -> v1) + (y -> v2))
        case error: EvalError => error
        case other            => TupleOperationOnNonTupleValue(other)
      }
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = verifyLiteralIdentifierType(x, y) {
      assign.typeCheck(tEnv) match {
        case PairType(l, r)   => bound.typeCheck(tEnv + (x -> l) + (y -> r))
        case error: TypeError => error
        case other            => TupleOperationOnNonTupleType(other)
      }
    }

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] = List(
      (x, env),
      (y, env),
      (assign, env),
      (bound, env + (x -> Fst(assign).eval(env)) + (y -> Snd(assign).eval(env)))
    )

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] =
      List((assign, env), (bound, env + (x -> Fst(assign).eval(env)) + (y -> Snd(assign).eval(env))))

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = List(
      (assign, tEnv),
      (bound, tEnv + (x -> Fst(assign).typeCheck(tEnv)) + (y -> Snd(assign).typeCheck(tEnv)))
    )

    override def toText: ConvertableText = formatLetPair(x.toText, y.toText, assign.toTextBracketed, bound.toTextBracketed)
  }

  object LetPair extends ExprCompanion {
    def apply(x: Variable, y: Variable, assign: Expr, bound: Expr): LetPair =
      LetPair(LiteralIdentifierBind(x), LiteralIdentifierBind(y), assign, bound)

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(x: LiteralIdentifierBind, y: LiteralIdentifierBind, assign: Expr, bound: Expr) =>
        Some(LetPair(x, y, assign, bound))
      case Nil => Some(LetPair(LiteralIdentifierBind.default, LiteralIdentifierBind.default, defaultExpr, defaultExpr))
      case _   => None
    }

    override val aliases: List[String] = List("LetTuple")

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .setConclusion(formatLetPair(TermCommons.x, TermCommons.y, TermCommons.e(1), TermCommons.e(2)), TermCommons.t)
          .addAssumption(TermCommons.e(1), formatPairType(TermCommons.t(1), TermCommons.t(2)))
          .addAssumption(
            TermCommons.e(2), TermCommons.t, List(
              TypeCheckRuleBind(TermCommons.x, TermCommons.t(1)),
              TypeCheckRuleBind(TermCommons.y, TermCommons.t(2))
            )
          )
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(formatLetPair(TermCommons.x, TermCommons.y, TermCommons.e(1), TermCommons.e(2)), TermCommons.v)
          .addAssumption(TermCommons.e(1), formatPair(TermCommons.v(1), TermCommons.v(2)))
          .addAssumption(
            TermCommons.e(2),
            TermCommons.v,
            List(EvalRuleBind(TermCommons.x, TermCommons.v(1)), EvalRuleBind(TermCommons.y, TermCommons.v(2)))
          )
      )
      .buildOption
  }

  case class UnitExpr() extends Expr {
    override def evalInner(env: ValueEnv): Value = UnitV()

    override def typeCheckInner(tEnv: TypeEnv): Type = EmptyType()

    override def toText: ConvertableText = TextElement("()")

    override val needsBrackets: Boolean = false
  }

  object UnitExpr extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case Nil => Some(UnitExpr())
      case _   => None
    }
  }

  private def formatLeft(e: ConvertableText): ConvertableText = MultiElement(TextElement("left"), BracketedElement(e))

  case class Left(e: Expr, rightType: Type) extends Expr {
    override def evalInner(env: ValueEnv): Value = LeftV(e.eval(env), rightType)

    override def typeCheckInner(tEnv: TypeEnv): Type = UnionType(e.typeCheck(tEnv), rightType)

    override def toText: ConvertableText = formatLeft(e.toText)

    override val needsBrackets: Boolean = false
  }

  object Left extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e: Expr, rightType: Type) => Some(Left(e, rightType))
      case Nil                            => Some(Left(defaultExpr, defaultType))
      case _                              => None
    }

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .addAssumption(TermCommons.e, TermCommons.t(1))
          .setConclusion(formatLeft(TermCommons.e), formatUnionType(TermCommons.t(1), TermCommons.t(2)))
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .addAssumption(TermCommons.e, TermCommons.v)
          .setConclusion(formatLeft(TermCommons.e), formatLeft(TermCommons.v))
      )
      .buildOption
  }

  private def formatRight(e: ConvertableText): ConvertableText = MultiElement(TextElement("right"), BracketedElement(e))

  case class Right(leftType: Type, e: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = RightV(leftType, e.eval(env))

    override def typeCheckInner(tEnv: TypeEnv): Type = UnionType(leftType, e.typeCheck(tEnv))

    override def toText: ConvertableText = formatRight(e.toText)

    override val needsBrackets: Boolean = false
  }

  object Right extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(leftType: Type, e: Expr) => Some(Right(leftType, e))
      case Nil                           => Some(Right(defaultType, defaultExpr))
      case _                             => None
    }

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .addAssumption(TermCommons.e, TermCommons.t(2))
          .setConclusion(formatRight(TermCommons.e), formatUnionType(TermCommons.t(1), TermCommons.t(2)))
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .addAssumption(TermCommons.e, TermCommons.v)
          .setConclusion(formatRight(TermCommons.e), formatRight(TermCommons.v))
      )
      .buildOption
  }

  private def formatCaseSwitch(e: ConvertableText, x: ConvertableText, y: ConvertableText, lExpr: ConvertableText, rExpr: ConvertableText): ConvertableText =
    MultiElement(
      TextElement("case "),
      e,
      TextElement(" of { left"),
      BracketedElement(x),
      SurroundSpaces(Symbols.doubleRightArrow),
      lExpr,
      TextElement("; right"),
      BracketedElement(y),
      SurroundSpaces(Symbols.doubleRightArrow),
      rExpr,
      TextElement(" }")
    )

  case class CaseSwitch(e: Expr, l: LiteralIdentifierBind, r: LiteralIdentifierBind, lExpr: Expr, rExpr: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = verifyLiteralIdentifierEval(l, r) {
      e.eval(env) match {
        case LeftV(v, rightType) => lExpr.eval(env + (l -> v))
        case RightV(leftType, v) => rExpr.eval(env + (r -> v))
        case other               => CaseSwitchOnNonUnionValue(other)
      }
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = verifyLiteralIdentifierType(l, r) {
      e.typeCheck(tEnv) match {
        case UnionType(lType, rType) =>
          val leftResult = lExpr.typeCheck(tEnv + (l -> lType))
          val rightResult = rExpr.typeCheck(tEnv + (r -> rType))
          if (leftResult == rightResult) leftResult
          else TypeMismatchType(leftResult, rightResult)
        case other => CaseSwitchOnNonUnionType(other)
      }
    }

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] = {
      val (lVal, rVal): (Value, Value) = e.eval(env) match {
        case LeftV(v, rTyp)  => (v, HiddenValue(rTyp))
        case RightV(lTyp, v) => (HiddenValue(lTyp), v)
        case _ =>
          e.typeCheck(TypeEnv.fromValueEnv(env)) match {
            case UnionType(l, r) => (HiddenValue(l), HiddenValue(r))
            case _               => (HiddenValue(UnknownType()), HiddenValue(UnknownType()))
          }
      }
      List((e, env), (lExpr, env + (l -> lVal)), (rExpr, env + (r -> rVal)))
    }

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] = e.eval(env) match {
      case LeftV(v, _)  => List((e, env), (lExpr, env + (l -> v)))
      case RightV(_, v) => List((e, env), (rExpr, env + (r -> v)))
      case other        => List((e, env))
    }

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = e.typeCheck(tEnv) match {
      case UnionType(lType, rType) =>
        List((e, tEnv), (lExpr, tEnv + (l -> lType)), (rExpr, tEnv + (r -> rType)))
      case other =>
        List((e, tEnv), (lExpr, tEnv + (l -> UnknownType())), (rExpr, tEnv + (r -> UnknownType())))
    }

    override def toText: ConvertableText = formatCaseSwitch(
      e.toTextBracketed, l.toText, r.toText, lExpr.toTextBracketed, rExpr.toTextBracketed
    )
  }

  object CaseSwitch extends ExprCompanion {
    def apply(e: Expr, l: Variable, r: Variable, lExpr: Expr, rExpr: Expr): CaseSwitch =
      CaseSwitch(e, LiteralIdentifierBind(l), LiteralIdentifierBind(r), lExpr, rExpr)

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e: Expr, l: LiteralIdentifierBind, r: LiteralIdentifierBind, lExpr: Expr, rExpr: Expr) =>
        Some(CaseSwitch(e, l, r, lExpr, rExpr))
      case Nil =>
        Some(CaseSwitch(defaultExpr, LiteralIdentifierBind.default, LiteralIdentifierBind.default, defaultExpr, defaultExpr))
      case _ => None
    }

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .setConclusion(
            formatCaseSwitch(TermCommons.e, TermCommons.x, TermCommons.y, TermCommons.e(1), TermCommons.e(2)),
            TermCommons.t
          )
          .addAssumption(TermCommons.e, formatUnionType(TermCommons.t(1), TermCommons.t(2)))
          .addAssumption(TermCommons.e(1), TermCommons.t, List(TypeCheckRuleBind(TermCommons.x, TermCommons.t(1))))
          .addAssumption(TermCommons.e(2), TermCommons.t, List(TypeCheckRuleBind(TermCommons.y, TermCommons.t(2))))
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(
            formatCaseSwitch(TermCommons.e, TermCommons.x, TermCommons.y, TermCommons.e(1), TermCommons.e(2)),
            TermCommons.v
          )
          .addAssumption(TermCommons.e, formatLeft(TermCommons.v(1)))
          .addAssumption(
            TermCommons.e(1),
            TermCommons.v,
            List(EvalRuleBind(TermCommons.x, TermCommons.v(1)))
          )
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(
            formatCaseSwitch(TermCommons.e, TermCommons.x, TermCommons.y, TermCommons.e(1), TermCommons.e(2)),
            TermCommons.v
          )
          .addAssumption(TermCommons.e, formatRight(TermCommons.v(2)))
          .addAssumption(
            TermCommons.e(2),
            TermCommons.v,
            List(EvalRuleBind(TermCommons.y, TermCommons.v(2)))
          )
      )
      .buildOption
  }

  // types

  private def formatPairType(l: ConvertableText, r: ConvertableText): ConvertableText =
    MultiElement(l, SurroundSpaces(Symbols.times), r)

  case class PairType(l: Type, r: Type) extends Type {
    override def typeCheck(tEnv: TypeEnv): Type = PairType(l.typeCheck(tEnv), r.typeCheck(tEnv))

    override def toText: ConvertableText = formatPairType(l.toTextBracketed, r.toTextBracketed)

    override val isError: Boolean = l.isError || r.isError
  }

  object PairType extends TypeCompanion {
    override def create(args: BuilderArgs): Option[Type] = args match {
      case List(l: Type, r: Type) => Some(PairType(l, r))
      case Nil                    => Some(PairType(defaultType, defaultType))
      case _                      => None
    }

    override val aliases: List[String] = List("TupleType")
  }

  private def formatUnionType(l: ConvertableText, r: ConvertableText): ConvertableText =
    MultiElement(l, SurroundSpaces(MathElement.plus), r)

  case class UnionType(l: Type, r: Type) extends Type {
    override def typeCheck(tEnv: TypeEnv): Type = UnionType(l.typeCheck(tEnv), r.typeCheck(tEnv))

    override def toText: ConvertableText = formatUnionType(l.toTextBracketed, r.toTextBracketed)

    override val isError: Boolean = l.isError || r.isError
  }

  object UnionType extends TypeCompanion {
    override def create(args: BuilderArgs): Option[Type] = args match {
      case List(l: Type, r: Type) => Some(UnionType(l, r))
      case Nil                    => Some(UnionType(defaultType, defaultType))
      case _                      => None
    }
  }

  case class EmptyType() extends Type {
    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = TextElement("()")
  }

  object EmptyType extends TypeCompanion {
    override def create(args: BuilderArgs): Option[Type] = args match {
      case Nil => Some(EmptyType())
      case _   => None
    }
  }

  case class AnyType() extends Type {
    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = TextElement("Any")
  }

  // values

  case class PairV(v1: Value, v2: Value) extends Value {
    override val typ: Type = PairType(v1.typ, v2.typ)

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = BracketedElement(
      MultiElement(v1.toTextBracketed, SpaceAfter(MathElement.comma), v2.toTextBracketed)
    )
  }

  object PairV extends ValueCompanion {}

  case class UnitV() extends Value {
    override val typ: Type = EmptyType()

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = TextElement("()")
  }

  object UnitV extends ValueCompanion {}

  case class LeftV(v: Value, rightType: Type) extends Value {
    override val typ: Type = UnionType(v.typ, rightType)

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = MultiElement(TextElement("left"), BracketedElement(v.toText))
  }

  object LeftV extends ValueCompanion {}

  case class RightV(leftType: Type, v: Value) extends Value {
    override val typ: Type = UnionType(leftType, v.typ)

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = MultiElement(TextElement("right"), BracketedElement(v.toText))
  }

  object RightV extends ValueCompanion {}

  // errors

  case class TupleOperationOnNonTupleType(provided: Type) extends TypeError {
    override val message: String = s"Cannot call a tuple operation on a non-tuple type ($provided)"
  }

  case class TupleOperationOnNonTupleValue(provided: Value) extends EvalError {
    override val message: String = s"Cannot call a tuple operation on a non-tuple value ($provided)"

    override val typ: Type = TupleOperationOnNonTupleType(provided.typ)
  }

  case class CaseSwitchOnNonUnionType(provided: Type) extends TypeError {
    override val message: String = s"Cannot call a case switch on a non-union type ($provided)"
  }

  case class CaseSwitchOnNonUnionValue(provided: Value) extends EvalError {
    override val message: String = s"Cannot call a case switch on a non-union value ($provided)"

    override val typ: Type = CaseSwitchOnNonUnionType(provided.typ)
  }

  private def verifyLiteralIdentifierEval(l: Literal*)(continue: => Value): Value = {
    l.find(!_.isInstanceOf[LiteralIdentifierBind]) match {
      case Some(lit) => InvalidIdentifierEvalError(lit)
      case None      => continue
    }
  }

  private def verifyLiteralIdentifierType(l: Literal*)(continue: => Type): Type = {
    l.find(!_.isInstanceOf[LiteralIdentifierBind]) match {
      case Some(lit) => InvalidIdentifierTypeError(lit)
      case None      => continue
    }
  }

  // tasks
  setTasks(DefinePairDifferentTypesTask, UseCaseSwitchTask, UnionFunctionWithNumber2Task)

  private object DefinePairDifferentTypesTask extends Task {
    override val name: String = "Define a Pair with Different Types"
    override val description: String =
      "Define a pair with different types for the left and right elements. The expression must successfully type-check."
    override val difficulty: Int = 2

    override def checkFulfilled(expr: Expr): Boolean = !expr.typeCheck().isError && checkCondition(
      expr,
      (e, env) => {
        val tEnv = TypeEnv.fromValueEnv(env)
        e match {
          case Pair(e1, e2) => e1.typeCheck(tEnv) != e2.typeCheck(tEnv)
          case _            => false
        }
      },
      ValueEnv.empty
    )
  }

  private object UseCaseSwitchTask extends Task {
    override val name: String = "Use a Case Switch Expression"
    override val description: String =
      "Use a case switch expression to handle a union type. The expression must successfully type-check."
    override val difficulty: Int = 3

    override def checkFulfilled(expr: Expr): Boolean = !expr.typeCheck().isError && checkCondition(
      expr,
      (e, env) =>
        e match {
          case _: CaseSwitch => true
          case _             => false
        },
      ValueEnv.empty
    )
  }

  object UnionFunctionWithNumber2Task extends Task {
    override val name: String = "Union Function using 2"
    override val description: String =
      "Create a lambda function that takes a parameter of the union type of (Int) and (Int -> Int), then returns " +
        "double the left value or the result of applying the right value to 2. " +
        "The expression must successfully type-check."
    override val difficulty: Int = 4

    override def checkFulfilled(expr: Expr): Boolean = {
      val cases: Map[Expr, NumV] = Map(
        Left(Num(3), Func(IntType(), IntType())) -> NumV(6),
        Left(Num(-53), Func(IntType(), IntType())) -> NumV(-106),
        Left(Times(Num(3), Num(4)), Func(IntType(), IntType())) -> NumV(24),
        Right(IntType(), Lambda("x", IntType(), Plus(Var("x"), Num(3)))) -> NumV(5),
        Right(IntType(), Lambda("x", IntType(), Times(Var("x"), Num(120)))) -> NumV(240),
        Right(IntType(), Lambda("x", IntType(), IfThenElse(Equal(Var("x"), Num(2)), Num(1), Num(0)))) -> NumV(1)
      )
      !expr.typeCheck().isError && checkCondition(
        expr,
        (e, env) =>
          e match {
            case Lambda(v, t, e) =>
              val tEnv = TypeEnv.fromValueEnv(env) + (v -> t)
              t == UnionType(IntType(), Func(IntType(), IntType())) &&
              e.typeCheck(tEnv) == IntType() &&
              cases.forall((input, expected) => Apply(Lambda(v, t, e), input).eval(env) == expected)
            case _ => false
          },
        ValueEnv.empty
      )
    }
  }

  protected class LDataParser extends LRecParser {
    override protected def keywords: Set[String] = super.keywords ++ Set("fst", "snd", "left", "right", "pair", "case", "of")

    private def pair: Parser[Pair] = "(" ~ expr ~ "," ~ expr ~ ")" ^^ { case _ ~ e1 ~ _ ~ e2 ~ _ => Pair(e1, e2) }

    private def fst: Parser[Fst] = "fst" ~ expr ^^ { case _ ~ e => Fst(e) }

    private def snd: Parser[Snd] = "snd" ~ expr ^^ { case _ ~ e => Snd(e) }

    private def letPair: Parser[LetPair] = "let" ~ "pair" ~ ident ~ ident ~ "=" ~ expr ~ "in" ~ expr ^^ {
      case _ ~ _ ~ x ~ y ~ _ ~ assign ~ _ ~ bound => LetPair(LiteralIdentifierBind(x), LiteralIdentifierBind(y), assign, bound)
    }

    private def left: Parser[Left] = "left" ~ expr ^^ { case _ ~ e => Left(e, defaultType) }

    private def right: Parser[Right] = "right" ~ expr ^^ { case _ ~ e => Right(defaultType, e) }

    private def caseSwitch: Parser[CaseSwitch] = "case" ~ expr ~ "of" ~ "{" ~ "left" ~ ident ~ "=>" ~ expr ~ ";" ~ "right" ~ ident ~ "=>" ~ expr ~ "}" ^^ {
      case _ ~ e ~ _ ~ _ ~ _ ~ x ~ _ ~ lExpr ~ _ ~ _ ~ y ~ _ ~ rExpr ~ _ =>
        CaseSwitch(e, x, y, lExpr, rExpr)
    }

    override protected def primitive: Parser[Expr] =
      pair | fst | snd | letPair | left | right | caseSwitch | super.primitive
  }

  override protected val exprParser: ExprParser = new LDataParser
}

object LData extends LData {}
