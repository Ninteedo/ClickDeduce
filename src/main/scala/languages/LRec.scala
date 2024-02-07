package languages

import scalatags.Text
import scalatags.Text.all.*

class LRec extends LLam {
  // expressions
  case class Rec(f: Literal, v: Literal, inType: Type, outType: Type, e: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = f match {
      case LiteralIdentifier(f_id) =>
        v match {
          case LiteralIdentifier(v_id) =>
            RecV(f, v, inType.typeCheck(envToTypeEnv(env)), outType.typeCheck(envToTypeEnv(env)), e, env)
          case _ => InvalidIdentifierEvalError(v)
        }
      case _ => InvalidIdentifierEvalError(f)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = f match {
      case LiteralIdentifier(f_id) =>
        v match {
          case LiteralIdentifier(v_id) =>
            val properInType = inType.typeCheck(tEnv)
            val properOutType = outType.typeCheck(tEnv)
            val extendedTEnv = tEnv + (f.toString -> Func(inType, outType)) + (v.toString -> inType)
            val determinedOutType = e.typeCheck(extendedTEnv).typeCheck(extendedTEnv)
            if (properOutType == determinedOutType) Func(properInType, determinedOutType)
            else RecursiveFunctionExpressionOutTypeMismatch(properOutType, determinedOutType)
          case _ => InvalidIdentifierTypeError(v)
        }
      case _ => InvalidIdentifierTypeError(f)
    }

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] = List(
      (f, env),
      (v, env),
      (inType, env),
      (outType, env),
      (e, env ++ Env(f.toString -> HiddenValue(Func(inType, outType)), v.toString -> HiddenValue(inType)))
    )

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = List(
      (e, tEnv ++ Env(f.toString -> Func(inType, outType), v.toString -> inType))
    )

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] = Nil

    override def prettyPrint: String = prettyPrintRec(f, v, inType, outType, e)
  }

  object Rec {
    def apply(f: String, v: String, in_typ: Type, out_typ: Type, e: Expr): Rec =
      Rec(Literal.fromString(f), Literal.fromString(v), in_typ, out_typ, e)
  }

  addExprBuilder(
    "Rec",
    {
      case List(f: Literal, v: Literal, inType: Type, outType: Type, e: Expr) => Some(Rec(f, v, inType, outType, e))
      case Nil => Some(Rec(defaultLiteral, defaultLiteral, defaultType, defaultType, defaultExpr))
      case _ => None
    }
  )


  // values
  case class RecV(f: Literal, v: Literal, in_typ: Type, out_typ: Type, e: Expr, env: ValueEnv) extends FunctionValue {
    override val typ: Type = Func(in_typ, out_typ)

    override def getFunctionEvaluation(applyValue: Value): (Expr, ValueEnv) =
      (e, env ++ Env(f.toString -> this, v.toString -> applyValue))

    override def evalApply(value: Value): Value = e.eval(env ++ Env(f.toString -> this, v.toString -> value))

    override def prettyPrint: String = prettyPrintRec(f, v, in_typ, out_typ, e)
  }

  addValueBuilder(
    "RecV",
    {
      case List(f: Literal, v: Literal, inType: Type, outType: Type, e: Expr, env: ValueEnv) =>
        Some(RecV(f, v, inType, outType, e, env))
      case _ => None
    }
  )

  private def prettyPrintRec(f: Literal, v: Literal, in_typ: Type, out_typ: Type, e: Expr) =
    s"rec $f($v: ${in_typ.prettyPrint}): ${out_typ.prettyPrintBracketed}. ${e.prettyPrintBracketed}"

  class RecursiveFunctionExpressionOutTypeMismatch(declared: Type, actual: Type) extends TypeError {
    override val message: String =
      s"Recursive function expression declared return type $declared does not match actual return type $actual"
  }
}

object LRec extends LRec {}
