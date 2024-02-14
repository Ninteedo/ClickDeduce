package languages

import convertors.*

class LData extends LRec {
  // expressions

  case class Pair(e1: Expr, e2: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = PairV(e1.eval(env), e2.eval(env))

    override def typeCheckInner(tEnv: TypeEnv): Type = PairType(e1.typeCheck(tEnv), e2.typeCheck(tEnv))

    override def prettyPrint: String = s"(${e1.prettyPrintBracketed}, ${e2.prettyPrintBracketed})"

    override def toText: ConvertableText =
      BracketedElement(MultiElement(e1.toText, SpaceAfter(MathElement.comma), e2.toText))

    override val needsBrackets: Boolean = false
  }

  addExprBuilder(
    "Pair",
    {
      case List(e1: Expr, e2: Expr) => Some(Pair(e1, e2))
      case Nil                      => Some(Pair(defaultExpr, defaultExpr))
      case _                        => None
    }
  )

  case class Fst(e: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = e.eval(env) match {
      case PairV(v1, _) => v1
      case other        => TupleOperationOnNonTupleValue(other)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = e.typeCheck(tEnv) match {
      case PairType(l, _) => l
      case other          => TupleOperationOnNonTupleType(other)
    }

    override def prettyPrint: String = s"fst(${e.prettyPrint})"

    override def toText: ConvertableText = MultiElement(TextElement("fst"), BracketedElement(e.toText))

    override val needsBrackets: Boolean = false
  }

  addExprBuilder(
    "Fst",
    {
      case List(e: Expr) => Some(Fst(e))
      case Nil           => Some(Fst(defaultExpr))
      case _             => None
    }
  )

  case class Snd(e: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = e.eval(env) match {
      case PairV(_, v2) => v2
      case other        => TupleOperationOnNonTupleValue(other)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = e.typeCheck(tEnv) match {
      case PairType(_, r) => r
      case other          => TupleOperationOnNonTupleType(other)
    }

    override def prettyPrint: String = s"snd(${e.prettyPrint})"

    override def toText: ConvertableText = MultiElement(TextElement("snd"), BracketedElement(e.toText))

    override val needsBrackets: Boolean = false
  }

  addExprBuilder(
    "Snd",
    {
      case List(e: Expr) => Some(Snd(e))
      case Nil           => Some(Snd(defaultExpr))
      case _             => None
    }
  )

  case class LetPair(x: Literal, y: Literal, assign: Expr, bound: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = verifyLiteralIdentifierEval(x, y) {
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

    override def prettyPrint: String =
      s"let pair ($x, $y) = ${assign.prettyPrintBracketed} in ${bound.prettyPrintBracketed}"

    override def toText: ConvertableText = MultiElement(
      SpaceAfter(TextElement("let pair")),
      BracketedElement(MultiElement(x.toText, SpaceAfter(MathElement.comma), y.toText)),
      SurroundSpaces(MathElement.equals),
      assign.toTextBracketed,
      SurroundSpaces(TextElement("in")),
      bound.toTextBracketed
    )

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] = List(
      (x, env),
      (y, env),
      (assign, env),
      (bound, env + (x.toString -> Fst(assign).eval(env)) + (y.toString -> Snd(assign).eval(env)))
    )

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] =
      List((assign, env), (bound, env + (x.toString -> Fst(assign).eval(env)) + (y.toString -> Snd(assign).eval(env))))

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = List(
      (assign, tEnv),
      (bound, tEnv + (x.toString -> Fst(assign).typeCheck(tEnv)) + (y.toString -> Snd(assign).typeCheck(tEnv)))
    )
  }

  object LetPair {
    def apply(x: Variable, y: Variable, assign: Expr, bound: Expr): LetPair =
      LetPair(Literal.fromString(x), Literal.fromString(y), assign, bound)
  }

  addExprBuilder(
    "LetPair",
    {
      case List(x: Literal, y: Literal, assign: Expr, bound: Expr) => Some(LetPair(x, y, assign, bound))
      case Nil => Some(LetPair(defaultLiteral, defaultLiteral, defaultExpr, defaultExpr))
      case _   => None
    }
  )

  case class UnitExpr() extends Expr {
    override def evalInner(env: ValueEnv): Value = UnitV()

    override def typeCheckInner(tEnv: TypeEnv): Type = EmptyType()

    override def prettyPrint: String = "()"

    override def toText: ConvertableText = TextElement("()")

    override val needsBrackets: Boolean = false
  }

  addExprBuilder(
    "UnitExpr",
    {
      case Nil => Some(UnitExpr())
      case _   => None
    }
  )

  case class Left(e: Expr, rightType: Type) extends Expr {
    override def evalInner(env: ValueEnv): Value = LeftV(e.eval(env), rightType)

    override def typeCheckInner(tEnv: TypeEnv): Type = UnionType(e.typeCheck(tEnv), rightType)

    override def prettyPrint: String = s"left(${e.prettyPrint})"

    override def toText: ConvertableText = MultiElement(TextElement("left"), BracketedElement(e.toText))

    override val needsBrackets: Boolean = false
  }

  addExprBuilder(
    "Left",
    {
      case List(e: Expr, rightType: Type) => Some(Left(e, rightType))
      case Nil                            => Some(Left(defaultExpr, defaultType))
      case _                              => None
    }
  )

  case class Right(leftType: Type, e: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = RightV(leftType, e.eval(env))

    override def typeCheckInner(tEnv: TypeEnv): Type = UnionType(leftType, e.typeCheck(tEnv))

    override def prettyPrint: String = s"right(${e.prettyPrint})"

    override def toText: ConvertableText = MultiElement(TextElement("right"), BracketedElement(e.toText))

    override val needsBrackets: Boolean = false
  }

  addExprBuilder(
    "Right",
    {
      case List(leftType: Type, e: Expr) => Some(Right(leftType, e))
      case Nil                           => Some(Right(defaultType, defaultExpr))
      case _                             => None
    }
  )

  case class CaseSwitch(e: Expr, l: Literal, r: Literal, lExpr: Expr, rExpr: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = verifyLiteralIdentifierEval(l, r) {
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
      s"case ${e.prettyPrintBracketed} of " +
        s"{ left($l) ⇒ ${lExpr.prettyPrintBracketed}; right($r) ⇒ ${rExpr.prettyPrintBracketed} }"

    override def toText: ConvertableText = MultiElement(
      TextElement("case"),
      SurroundSpaces(e.toText),
      TextElement("of { left"),
      BracketedElement(l.toText),
      SurroundSpaces(DoubleRightArrow()),
      lExpr.toTextBracketed,
      TextElement("; right"),
      BracketedElement(r.toText),
      SurroundSpaces(DoubleRightArrow()),
      SpaceAfter(rExpr.toTextBracketed),
      TextElement("}")
    )

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] = {
      val (lVal, rVal): (Value, Value) = e.eval(env) match {
        case LeftV(v, rTyp)  => (v, HiddenValue(rTyp))
        case RightV(lTyp, v) => (HiddenValue(lTyp), v)
        case _               => (HiddenValue(UnknownType()), HiddenValue(UnknownType()))
      }
      List((e, env), (lExpr, env + (l.toString -> lVal)), (rExpr, env + (r.toString -> rVal)))
    }

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] = e.eval(env) match {
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

  addExprBuilder(
    "CaseSwitch",
    {
      case List(e: Expr, l: Literal, r: Literal, lExpr: Expr, rExpr: Expr) => Some(CaseSwitch(e, l, r, lExpr, rExpr))
      case Nil => Some(CaseSwitch(defaultExpr, defaultLiteral, defaultLiteral, defaultExpr, defaultExpr))
      case _   => None
    }
  )

  // types

  case class PairType(l: Type, r: Type) extends Type {
    override def typeCheck(tEnv: TypeEnv): Type = PairType(l.typeCheck(tEnv), r.typeCheck(tEnv))

    override def prettyPrint: String = s"${l.prettyPrintBracketed} × ${r.prettyPrintBracketed}"

    override def toText: ConvertableText =
      MultiElement(l.toTextBracketed, SurroundSpaces(TimesSymbol()), r.toTextBracketed)
  }

  addTypeBuilder(
    "PairType",
    {
      case List(l: Type, r: Type) => Some(PairType(l, r))
      case Nil                    => Some(PairType(defaultType, defaultType))
      case _                      => None
    }
  )

  case class UnionType(l: Type, r: Type) extends Type {
    override def typeCheck(tEnv: TypeEnv): Type = UnionType(l.typeCheck(tEnv), r.typeCheck(tEnv))

    override def prettyPrint: String = s"${l.prettyPrintBracketed} + ${r.prettyPrintBracketed}"

    override def toText: ConvertableText =
      MultiElement(l.toTextBracketed, SurroundSpaces(MathElement("+")), r.toTextBracketed)
  }

  addTypeBuilder(
    "UnionType",
    {
      case List(l: Type, r: Type) => Some(UnionType(l, r))
      case Nil                    => Some(UnionType(defaultType, defaultType))
      case _                      => None
    }
  )

  case class EmptyType() extends Type {
    override def prettyPrint: String = "()"

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = TextElement("()")
  }

  addTypeBuilder(
    "EmptyType",
    {
      case Nil => Some(EmptyType())
      case _   => None
    }
  )

  case class AnyType() extends Type {
    override def prettyPrint: String = "Any"

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = TextElement("Any")
  }

  // values

  case class PairV(v1: Value, v2: Value) extends Value {
    override val typ: Type = PairType(v1.typ, v2.typ)

    override def prettyPrint: String = s"(${v1.prettyPrintBracketed}, ${v2.prettyPrintBracketed})"

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = BracketedElement(
      MultiElement(v1.toTextBracketed, SpaceAfter(MathElement.comma), v2.toTextBracketed)
    )
  }

  addValueBuilder(
    "PairV",
    {
      case List(v1: Value, v2: Value) => Some(PairV(v1, v2))
      case _                          => None
    }
  )

  case class UnitV() extends Value {
    override val typ: Type = EmptyType()

    override def prettyPrint: String = "()"

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = TextElement("()")
  }

  addValueBuilder(
    "UnitV",
    {
      case Nil => Some(UnitV())
      case _   => None
    }
  )

  case class LeftV(v: Value, rightType: Type) extends Value {
    override val typ: Type = UnionType(v.typ, rightType)

    override def prettyPrint: String = s"left(${v.prettyPrint})"

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = MultiElement(TextElement("left"), BracketedElement(v.toText))
  }

  addValueBuilder(
    "LeftV",
    {
      case List(v: Value, rightType: Type) => Some(LeftV(v, rightType))
      case _                               => None
    }
  )

  case class RightV(leftType: Type, v: Value) extends Value {
    override val typ: Type = UnionType(leftType, v.typ)

    override def prettyPrint: String = s"right(${v.prettyPrint})"

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = MultiElement(TextElement("right"), BracketedElement(v.toText))
  }

  addValueBuilder(
    "RightV",
    {
      case List(leftType: Type, v: Value) => Some(RightV(leftType, v))
      case _                              => None
    }
  )

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
}

object LData extends LData {}
