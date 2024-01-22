package languages

import scalatags.Text
import scalatags.Text.TypedTag
import scalatags.Text.all.*

class LRec extends LLam {
  // expressions
  case class Rec(f: Literal, v: Literal, inType: Type, outType: Type, e: Expr) extends Expr {
    override def evalInner(env: Env): Value = f match {
      case LiteralIdentifier(f_id) =>
        v match {
          case LiteralIdentifier(v_id) => RecV(f, v, inType, outType, e, env)
          case _                       => InvalidIdentifierEvalError(v)
        }
      case _ => InvalidIdentifierEvalError(f)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = f match {
      case LiteralIdentifier(f_id) =>
        v match {
          case LiteralIdentifier(v_id) =>
            val determinedOutType = e.typeCheck(tEnv + (f.toString -> Func(inType, outType)) + (v.toString -> inType))
            if (outType == determinedOutType) Func(inType, outType)
            else RecursiveFunctionExpressionOutTypeMismatch(outType, determinedOutType)
          case _ => InvalidIdentifierTypeError(v)
        }
      case _ => InvalidIdentifierTypeError(f)
    }

    override def getChildrenBase(env: Env): List[(Term, Env)] = List(
      (f, env),
      (v, env),
      (inType, env),
      (outType, env),
      (e, env ++ Map(f.toString -> PlaceholderValue(Func(inType, outType)), v.toString -> PlaceholderValue(inType)))
    )

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = List(
      (e, tEnv ++ Map(f.toString -> Func(inType, outType), v.toString -> inType))
    )

    override def getChildrenEval(env: Env): List[(Term, Env)] = Nil

    override def prettyPrint: String = prettyPrintRec(f, v, inType, outType, e)
  }

  object Rec {
    def apply(f: String, v: String, in_typ: Type, out_typ: Type, e: Expr): Rec =
      Rec(Literal.fromString(f), Literal.fromString(v), in_typ, out_typ, e)
  }

  // values
  case class RecV(f: Literal, v: Literal, in_typ: Type, out_typ: Type, e: Expr, env: Env) extends FunctionValue {
    override val typ: Type = Func(in_typ, out_typ)

    override def getFunctionEvaluation(applyValue: Value): (Expr, Env) =
      (e, env ++ Map(f.toString -> this, v.toString -> applyValue))

    override def evalApply(value: Value): Value = e.eval(env ++ Map(f.toString -> this, v.toString -> value))

    override lazy val valueText: Text.TypedTag[String] = div(
      raw(
        RecV(
          f,
          v,
          TypePlaceholder(in_typ.toHtml.toString),
          TypePlaceholder(out_typ.toHtml.toString),
          ExprPlaceholder(e.toHtml.toString),
          env
        ).prettyPrint
      )
    )

    override def prettyPrint: String = prettyPrintRec(f, v, in_typ, out_typ, e)
  }

  private def prettyPrintRec(f: Literal, v: Literal, in_typ: Type, out_typ: Type, e: Expr) =
    s"rec $f($v: ${in_typ.prettyPrint}): ${out_typ.prettyPrint}. ${e.prettyPrint}"

  override def calculateExprClassList: List[Class[Expr]] = {
    super.calculateExprClassList ++ List(classOf[Rec]).map(_.asInstanceOf[Class[Expr]])
  }

  class RecursiveFunctionExpressionOutTypeMismatch(declared: Type, actual: Type) extends TypeError {
    override val message: String =
      s"Recursive function expression declared return type $declared does not match actual return type $actual"
  }
}

object LRec extends LRec {}
