package languages

import scalatags.Text
import scalatags.Text.TypedTag
import scalatags.Text.all.{div, raw}

class LData extends LRec {
  // expressions

  case class Pair(e1: Expr, e2: Expr) extends Expr {
    override def evalInner(env: Env): Value = PairV(e1.eval(env), e2.eval(env))

    override def typeCheckInner(tEnv: TypeEnv): Type = PairType(e1.typeCheck(tEnv), e2.typeCheck(tEnv))

    override def prettyPrint: String = s"(${e1.prettyPrint}, ${e2.prettyPrint})"
  }

  case class Fst(e: Expr) extends Expr {
    override def evalInner(env: Env): Value = e.eval(env) match {
      case PairV(v1, _) => v1
      case other        => TupleOperationOnNonTupleValue(other)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = e.typeCheck(tEnv) match {
      case PairType(l, _) => l
      case other          => TupleOperationOnNonTupleType(other)
    }

    override def prettyPrint: String = s"fst ${e.prettyPrint}"
  }

  case class Snd(e: Expr) extends Expr {
    override def evalInner(env: Env): Value = e.eval(env) match {
      case PairV(_, v2) => v2
      case other        => TupleOperationOnNonTupleValue(other)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = e.typeCheck(tEnv) match {
      case PairType(_, r) => r
      case other          => TupleOperationOnNonTupleType(other)
    }

    override def prettyPrint: String = s"snd ${e.prettyPrint}"
  }

  case class LetPair(x: Literal, y: Literal, assign: Expr, bound: Expr) extends Expr {
    override def evalInner(env: Env): Value = verifyLiteralIdentifierEval(x, y) {
      assign.eval(env) match {
        case PairV(v1, v2)    => bound.eval(env + (x.toString -> v1) + (y.toString -> v2))
        case error: EvalError => error
        case other            => TupleOperationOnNonTupleValue(other)
      }
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = verifyLiteralIdentifierType(x, y) {
      assign.typeCheck(tEnv) match {
        case PairType(l, r)   => bound.typeCheck(tEnv + (x.toString -> l) + (y.toString -> r))
        case error: TypeError => error
        case other            => TupleOperationOnNonTupleType(other)
      }
    }

    override def prettyPrint: String = s"let pair ($x, $y) = ${assign.prettyPrint} in ${bound.prettyPrint}"

    override def getChildrenBase(env: Env): List[(Term, Env)] = List(
      (x, env),
      (y, env),
      (assign, env),
      (bound, env + (x.toString -> Fst(assign).eval(env)) + (y.toString -> Snd(assign).eval(env)))
    )

    override def getChildrenEval(env: Env): List[(Term, Env)] =
      List((assign, env), (bound, env + (x.toString -> Fst(assign).eval(env)) + (y.toString -> Snd(assign).eval(env))))

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] =
      List(
        (assign, tEnv),
        (bound, tEnv + (x.toString -> Fst(assign).typeCheck(tEnv)) + (y.toString -> Snd(assign).typeCheck(tEnv)))
      )
  }

  object LetPair {
    def apply(x: Variable, y: Variable, assign: Expr, bound: Expr): LetPair =
      LetPair(Literal.fromString(x), Literal.fromString(y), assign, bound)
  }

  case class UnitExpr() extends Expr {
    override def evalInner(env: Env): Value = UnitV()

    override def typeCheckInner(tEnv: TypeEnv): Type = EmptyType()

    override def prettyPrint: String = "()"
  }

  case class Left(e: Expr, rightType: Type) extends Expr {
    override def evalInner(env: Env): Value = LeftV(e.eval(env), rightType)

    override def typeCheckInner(tEnv: TypeEnv): Type = UnionType(e.typeCheck(tEnv), rightType)

    override def prettyPrint: String = s"left(${e.prettyPrint})"
  }

  case class Right(leftType: Type, e: Expr) extends Expr {
    override def evalInner(env: Env): Value = RightV(leftType, e.eval(env))

    override def typeCheckInner(tEnv: TypeEnv): Type = UnionType(leftType, e.typeCheck(tEnv))

    override def prettyPrint: String = s"right(${e.prettyPrint})"
  }

  case class CaseSwitch(e: Expr, l: Literal, r: Literal, lExpr: Expr, rExpr: Expr) extends Expr {
    override def evalInner(env: Env): Value = verifyLiteralIdentifierEval(l, r) {
      e.eval(env) match {
        case LeftV(v, rightType) => lExpr.eval(env + (l.toString -> v))
        case RightV(leftType, v) => rExpr.eval(env + (r.toString -> v))
        case other               => CaseSwitchOnNonUnionValue(other)
      }
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = verifyLiteralIdentifierType(l, r) {
      e.typeCheck(tEnv) match {
        case UnionType(lType, rType) =>
          val leftResult = lExpr.typeCheck(tEnv + (l.toString -> lType))
          val rightResult = rExpr.typeCheck(tEnv + (r.toString -> rType))
          if (leftResult == rightResult) leftResult
          else TypeMismatchType(leftResult, rightResult)
        case other => CaseSwitchOnNonUnionType(other)
      }
    }

    override def prettyPrint: String =
      s"case ${e.prettyPrint} of { left($l) ⇒ ${lExpr.prettyPrint}; right($r) ⇒ ${rExpr.prettyPrint} }"

    override def getChildrenBase(env: Env): List[(Term, Env)] = {
      val (lVal, rVal): (Value, Value) = e.eval(env) match {
        case LeftV(v, rTyp)  => (v, HiddenValue(rTyp))
        case RightV(lTyp, v) => (HiddenValue(lTyp), v)
        case _               => (HiddenValue(UnknownType()), HiddenValue(UnknownType()))
      }
      List((e, env), (lExpr, env + (l.toString -> lVal)), (rExpr, env + (r.toString -> rVal)))
    }

    override def getChildrenEval(env: Env): List[(Term, Env)] = e.eval(env) match {
      case LeftV(v, _)  => List((e, env), (lExpr, env + (l.toString -> v)))
      case RightV(_, v) => List((e, env), (rExpr, env + (r.toString -> v)))
      case other        => List((e, env))
    }

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = e.typeCheck(tEnv) match {
      case UnionType(lType, rType) =>
        List((e, tEnv), (lExpr, tEnv + (l.toString -> lType)), (rExpr, tEnv + (r.toString -> rType)))
      case other =>
        List((e, tEnv), (lExpr, tEnv + (l.toString -> UnknownType())), (rExpr, tEnv + (r.toString -> UnknownType())))
    }
  }

  object CaseSwitch {
    def apply(e: Expr, l: Variable, r: Variable, lExpr: Expr, rExpr: Expr): CaseSwitch =
      CaseSwitch(e, Literal.fromString(l), Literal.fromString(r), lExpr, rExpr)
  }

  // types

  case class PairType(l: Type, r: Type) extends Type {
    override def prettyPrint: String = s"(${l.prettyPrint}, ${r.prettyPrint})"

    override lazy val valueText: TypedTag[String] = div(l.toHtml, raw(", "), r.toHtml)
  }

  case class UnionType(l: Type, r: Type) extends Type {
    override def prettyPrint: String = s"(${l.prettyPrint} | ${r.prettyPrint})"

    override lazy val valueText: TypedTag[String] = div(l.toHtml, raw(" | "), r.toHtml)
  }

  case class EmptyType() extends Type {
    override def prettyPrint: String = "()"
  }

  case class AnyType() extends Type {
    override def prettyPrint: String = "Any"
  }

  // values

  case class PairV(v1: Value, v2: Value) extends Value {
    override val typ: Type = PairType(v1.typ, v2.typ)

    override def prettyPrint: String = s"(${v1.prettyPrint}, ${v2.prettyPrint})"

    override lazy val valueText: TypedTag[String] = div(
      raw(PairV(ValuePlaceholder(v1.toHtml.toString), ValuePlaceholder(v2.toHtml.toString)).prettyPrint)
    )
  }

  case class UnitV() extends Value {
    override val typ: Type = EmptyType()

    override def prettyPrint: String = "()"
  }

  case class LeftV(v: Value, rightType: Type) extends Value {
    override val typ: Type = UnionType(v.typ, rightType)

    override def prettyPrint: String = s"left(${v.prettyPrint})"

    override lazy val valueText: TypedTag[String] =
      div(raw(LeftV(ValuePlaceholder(v.toHtml.toString), TypePlaceholder(rightType.toHtml.toString)).prettyPrint))
  }

  case class RightV(leftType: Type, v: Value) extends Value {
    override val typ: Type = UnionType(leftType, v.typ)

    override def prettyPrint: String = s"right(${v.prettyPrint})"

    override lazy val valueText: TypedTag[String] =
      div(raw(RightV(TypePlaceholder(leftType.toHtml.toString), ValuePlaceholder(v.toHtml.toString)).prettyPrint))
  }

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
    l.find(!_.isInstanceOf[LiteralIdentifier]) match {
      case Some(lit) => InvalidIdentifierEvalError(lit)
      case None      => continue
    }
  }

  private def verifyLiteralIdentifierType(l: Literal*)(continue: => Type): Type = {
    l.find(!_.isInstanceOf[LiteralIdentifier]) match {
      case Some(lit) => InvalidIdentifierTypeError(lit)
      case None      => continue
    }
  }

  override def calculateExprClassList: List[Class[Expr]] = super.calculateExprClassList ++ List(
    classOf[Pair],
    classOf[Fst],
    classOf[Snd],
    classOf[LetPair],
    classOf[UnitExpr],
    classOf[Left],
    classOf[Right],
    classOf[CaseSwitch]
  ).map(_.asInstanceOf[Class[Expr]])

  override def calculateTypeClassList: List[Class[Type]] =
    super.calculateTypeClassList ++ List(classOf[PairType], classOf[UnionType], classOf[EmptyType], classOf[AnyType])
      .map(_.asInstanceOf[Class[Type]])
}

object LData extends LData {}
