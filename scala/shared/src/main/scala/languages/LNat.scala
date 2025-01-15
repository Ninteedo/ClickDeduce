package languages

import convertors.*
import convertors.text.*
import languages.env.*
import languages.terms.*
import languages.terms.builders.*
import languages.terms.errors.*
import languages.terms.exprs.Expr
import languages.terms.literals.*
import languages.terms.types.{Type, UnknownType}
import languages.terms.values.Value

class LNat extends LData {
  registerTerms("LNat", List(Zero, Suc, CaseSuc, NatType, NatV))

  // expressions

  case class Zero() extends Expr {

    override protected def evalInner(env: ValueEnv): Value = NatV(0)

    override protected def typeCheckInner(tEnv: TypeEnv): Type = NatType()

    override def toText: ConvertableText = TextElement("zero")

    override val needsBrackets: Boolean = false
  }

  object Zero extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case Nil => Some(Zero())
      case _   => None
    }
  }

  case class Suc(e: Expr) extends Expr {

    override protected def evalInner(env: ValueEnv): Value = e.eval(env) match {
      case NatV(n) => NatV(n + 1)
      case v       => SucOfNonNatEvalError(v)
    }

    override protected def typeCheckInner(tEnv: TypeEnv): Type = e.typeCheck(tEnv) match {
      case NatType() => NatType()
      case t         => SucOfNonNatTypeError(t)
    }

    override def toText: ConvertableText = MultiElement(TextElement("suc "), e.toTextBracketed)
  }

  object Suc extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e: Expr) => Some(Suc(e))
      case Nil           => Some(Suc(defaultExpr))
      case _             => None
    }
  }

  case class CaseSuc(e: Expr, zeroCase: Expr, x: LiteralIdentifierBind, sucCase: Expr) extends Expr {
    override protected def evalInner(env: ValueEnv): Value = e.eval(env) match {
      case NatV(0) => zeroCase.eval(env)
      case NatV(n) => sucCase.eval(env + (x -> NatV(n - 1)))
      case v       => CaseSucNonNatError(v)
    }

    override protected def typeCheckInner(tEnv: TypeEnv): Type = e.typeCheck(tEnv) match {
      case NatType() =>
        val t1 = zeroCase.typeCheck(tEnv)
        val t2 = sucCase.typeCheck(tEnv + (x -> NatType()))
        if (t1 == t2) t1
        else TypeMismatchType(t1, t2)
      case t => CaseSucNonNatTypeError(t)
    }

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] = {
      val xVal: Value = e.eval(env) match {
        case NatV(0) => HiddenValue(NatType())
        case NatV(n) => NatV(n - 1)
        case _ => e.typeCheck(TypeEnv.fromValueEnv(env)) match {
          case NatType() => HiddenValue(NatType())
          case _ => HiddenValue(UnknownType())
        }
      }
      List((e, env), (zeroCase, env), (sucCase, env + (x -> xVal)))
    }

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] =
      List((e, tEnv), (zeroCase, tEnv), (sucCase, tEnv + (x -> NatType())))

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] = e.eval(env) match {
      case NatV(0) => List((e, env), (zeroCase, env))
      case NatV(n) => List((e, env), (sucCase, env + (x -> NatV(n - 1))))
      case other   => List((e, env))
    }

    override def toText: ConvertableText = MultiElement(
      TextElement("case "),
      e.toTextBracketed,
      TextElement(" { zero"),
      SurroundSpaces(Symbols.doubleRightArrow),
      zeroCase.toTextBracketed,
      TextElement("; suc "),
      x.toText,
      SurroundSpaces(Symbols.doubleRightArrow),
      sucCase.toTextBracketed,
      TextElement(" }")
    )
  }

  object CaseSuc extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e: Expr, zeroCase: Expr, x: LiteralIdentifierBind, sucCase: Expr) => Some(CaseSuc(e, zeroCase, x, sucCase))
      case Nil => Some(CaseSuc(defaultExpr, defaultExpr, LiteralIdentifierBind.default, defaultExpr))
      case _ => None
    }
  }

  // types

  case class NatType() extends Type {
    override def toText: ConvertableText = TextElement("ℕ")

    override val needsBrackets: Boolean = false
  }

  object NatType extends TypeCompanion {
    override def create(args: BuilderArgs): Option[Type] = args match {
      case Nil => Some(NatType())
      case _   => None
    }
  }

  // values

  case class NatV(n: Int) extends Value {
    override val typ: Type = NatType()

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = TextElement(n.toString)
  }

  object NatV extends ValueCompanion {}

  // errors

  class SucOfNonNatEvalError(v: Value) extends EvalError {
    override val message: String = "Suc can only be applied to a Nat, not " + v

    override val typ: Type = SucOfNonNatTypeError(v.typ)
  }

  class SucOfNonNatTypeError(t: Type) extends TypeError {
    override val message: String = "Suc can only be applied to a Nat, not " + t
  }

  class CaseSucNonNatError(v: Value) extends EvalError {
    override val message: String = "CaseSuc can only be applied to a Nat, not " + v

    override val typ: Type = CaseSucNonNatTypeError(v.typ)
  }

  class CaseSucNonNatTypeError(t: Type) extends TypeError {
      override val message: String = "CaseSuc can only be applied to a Nat, not " + t
  }

  // tasks

  setTasks()
}

object LNat extends LNat {}
