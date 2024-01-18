package languages

import scalatags.Text
import scalatags.Text.TypedTag
import scalatags.Text.all.*

class LRec extends LLam {
  // expressions
  case class Rec(f: Literal, v: Literal, in_typ: Type, out_typ: Type, e: Expr) extends Expr {
    override def evalInner(env: Env): Value = f match {
      case LiteralIdentifier(f_id) =>
        v match {
          case LiteralIdentifier(v_id) => RecV(f, v, in_typ, out_typ, e, env)
          case _                       => InvalidIdentifierEvalError(v)
        }
      case _ => InvalidIdentifierEvalError(f)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = f match {
      case LiteralIdentifier(f_id) =>
        v match {
          case LiteralIdentifier(v_id) => Func(in_typ, out_typ)
          case _                       => InvalidIdentifierTypeError(v)
        }
      case _ => InvalidIdentifierTypeError(f)
    }

    override def getChildrenBase(env: Env): List[(Term, Env)] = List(
      (f, env),
      (v, env),
      (in_typ, env),
      (out_typ, env),
      (e, env ++ Map(f.toString -> PlaceholderValue(Func(in_typ, out_typ)), v.toString -> PlaceholderValue(in_typ)))
    )

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = List(
      (e, tEnv ++ Map(f.toString -> Func(in_typ, out_typ), v.toString -> in_typ))
    )

    override def getChildrenEval(env: Env): List[(Term, Env)] = Nil
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

    override lazy val valueText: Text.TypedTag[String] =
      div(raw(s"rec $f($v: "), in_typ.toHtml, raw(s") : "), out_typ.toHtml, raw(". "), e.toHtml)
  }

  private def prettyPrintRec(f: Literal, v: Literal, in_typ: Type, out_typ: Type, e: Expr) = {
    s"rec $f($v: ${prettyPrint(in_typ)}): ${prettyPrint(out_typ)}. ${prettyPrint(e)}"
  }

  override def prettyPrint(e: Expr): String = e match {
    case Rec(f, v, in_typ, out_typ, e) => prettyPrintRec(f, v, in_typ, out_typ, e)
    case _                             => super.prettyPrint(e)
  }

  override def prettyPrint(v: Value): String = v match {
    case RecV(f, v, in_typ, out_typ, e, env) => prettyPrintRec(f, v, in_typ, out_typ, e)
    case _                                   => super.prettyPrint(v)
  }

  override def calculateExprClassList: List[Class[Expr]] = {
    super.calculateExprClassList ++ List(classOf[Rec]).map(_.asInstanceOf[Class[Expr]])
  }
}

object LRec extends LRec {}
