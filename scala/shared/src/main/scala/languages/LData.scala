package languages

import convertors.*
import languages.env.*
import languages.env.Env.Variable
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

  case class Pair(e1: Expr, e2: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = PairV(e1.eval(env), e2.eval(env))

    override def typeCheckInner(tEnv: TypeEnv): Type = PairType(e1.typeCheck(tEnv), e2.typeCheck(tEnv))

    override def toText: ConvertableText =
      BracketedElement(MultiElement(e1.toText, SpaceAfter(MathElement.comma), e2.toText))

    override val needsBrackets: Boolean = false
  }

  object Pair extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e1: Expr, e2: Expr) => Some(Pair(e1, e2))
      case Nil                      => Some(Pair(defaultExpr, defaultExpr))
      case _                        => None
    }

    override val aliases: List[String] = List("Tuple")
  }

  case class Fst(e: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = e.eval(env) match {
      case PairV(v1, _) => v1
      case other        => TupleOperationOnNonTupleValue(other)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = e.typeCheck(tEnv) match {
      case PairType(l, _) => l
      case other          => TupleOperationOnNonTupleType(other)
    }

    override def toText: ConvertableText = MultiElement(TextElement("fst"), BracketedElement(e.toText))

    override val needsBrackets: Boolean = false
  }

  object Fst extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e: Expr) => Some(Fst(e))
      case Nil           => Some(Fst(defaultExpr))
      case _             => None
    }

    override val aliases: List[String] = List("First", "1st")
  }

  case class Snd(e: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = e.eval(env) match {
      case PairV(_, v2) => v2
      case other        => TupleOperationOnNonTupleValue(other)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = e.typeCheck(tEnv) match {
      case PairType(_, r) => r
      case other          => TupleOperationOnNonTupleType(other)
    }

    override def toText: ConvertableText = MultiElement(TextElement("snd"), BracketedElement(e.toText))

    override val needsBrackets: Boolean = false
  }

  object Snd extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e: Expr) => Some(Snd(e))
      case Nil           => Some(Snd(defaultExpr))
      case _             => None
    }

    override val aliases: List[String] = List("Second", "2nd")
  }

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

    override def toText: ConvertableText = MultiElement(
      TextElement("let pair "),
      BracketedElement(MultiElement(x.toText, SpaceAfter(MathElement.comma), y.toText)),
      SurroundSpaces(MathElement.equals),
      assign.toTextBracketed,
      TextElement(" in "),
      bound.toTextBracketed
    )

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

  case class Left(e: Expr, rightType: Type) extends Expr {
    override def evalInner(env: ValueEnv): Value = LeftV(e.eval(env), rightType)

    override def typeCheckInner(tEnv: TypeEnv): Type = UnionType(e.typeCheck(tEnv), rightType)

    override def toText: ConvertableText = MultiElement(TextElement("left"), BracketedElement(e.toText))

    override val needsBrackets: Boolean = false
  }

  object Left extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e: Expr, rightType: Type) => Some(Left(e, rightType))
      case Nil                            => Some(Left(defaultExpr, defaultType))
      case _                              => None
    }
  }

  case class Right(leftType: Type, e: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = RightV(leftType, e.eval(env))

    override def typeCheckInner(tEnv: TypeEnv): Type = UnionType(leftType, e.typeCheck(tEnv))

    override def toText: ConvertableText = MultiElement(TextElement("right"), BracketedElement(e.toText))

    override val needsBrackets: Boolean = false
  }

  object Right extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(leftType: Type, e: Expr) => Some(Right(leftType, e))
      case Nil                           => Some(Right(defaultType, defaultExpr))
      case _                             => None
    }
  }

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

    override def toText: ConvertableText = MultiElement(
      TextElement("case "),
      e.toText,
      TextElement(" of { left"),
      BracketedElement(l.toText),
      SurroundSpaces(Symbols.doubleRightArrow),
      lExpr.toTextBracketed,
      TextElement("; right"),
      BracketedElement(r.toText),
      SurroundSpaces(Symbols.doubleRightArrow),
      rExpr.toTextBracketed,
      TextElement(" }")
    )

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
  }

  // types

  case class PairType(l: Type, r: Type) extends Type {
    override def typeCheck(tEnv: TypeEnv): Type = PairType(l.typeCheck(tEnv), r.typeCheck(tEnv))

    override def toText: ConvertableText =
      MultiElement(l.toTextBracketed, SurroundSpaces(Symbols.times), r.toTextBracketed)

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

  case class UnionType(l: Type, r: Type) extends Type {
    override def typeCheck(tEnv: TypeEnv): Type = UnionType(l.typeCheck(tEnv), r.typeCheck(tEnv))

    override def toText: ConvertableText =
      MultiElement(l.toTextBracketed, SurroundSpaces(MathElement("+")), r.toTextBracketed)

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
}

object LData extends LData {}
