package languages

import scala.collection.immutable.List

class LLam extends LLet {
  // expressions
  case class Apply(e1: Expr, e2: Expr) extends Expr

  case class Lambda(v: Literal, typ: Type, e: Expr) extends Expr {
    override def childExprEnvs(env: Env): List[Env] = List(env, env + (v.toString -> NumV(-999)))

    override def childExprTypeEnvs(tenv: TypeEnv): List[TypeEnv] = List(
      tenv, tenv + (v.toString -> typ)
    )
  }

  object Lambda {
    def apply(v: Variable, typ: Type, e: Expr): Lambda = new Lambda(LiteralAny(v), typ, e)
  }

  // types
  case class Func(in: Type, out: Type) extends Type

  case class ApplyToNonFunctionErrorType(wrongType: Type) extends TypeError {
    override val message: String = s"Cannot apply with left expression being ${prettyPrint(wrongType)}"
  }

  case class IncompatibleTypeErrorType(typ1: Type, typ2: Type) extends TypeError {
    override val message: String = s"mismatched types for applying function (expected $typ1 but got $typ2)"
  }

  // values
  case class LambdaV(v: Variable, inputType: Type, e: Expr, env: Env) extends Value {
    override val typ: Type = Func(inputType, typeOf(e, envToTypeEnv(env) + (v -> inputType)))
  }

  case class ApplyToNonFunctionError(value: Value) extends EvalError {
    override val message: String = s"Cannot apply with left expression being ${prettyPrint(value)}"

    override val typ: Type = ApplyToNonFunctionErrorType(value.typ)
  }


  override def eval(e: Expr, env: Env): Value = e match {
    case Lambda(LiteralAny(v), typ, e) => LambdaV(v, typ, e, env)
    case Apply(e1, e2) => evalApply(Apply(e1, e2), env)
    case _ => super.eval(e, env)
  }

  def evalApply(e: Apply, env: Env): Value = e match {
    //    case Apply((Apply(e1, e2)), e3) => (Apply(e1, e2), eval(e3, env)) {
    //      case (Apply(e1, e2), value) => (eval())
    //    }
    case _ => {
      (eval(e.e1, env), eval(e.e2, env)) match {
        case (LambdaV(v, inputType, e0, innerEnv), value) => eval(e0, innerEnv + (v -> value))
        case (v1, _) => ApplyToNonFunctionError(v1)
      }
    }
  }

  override def typeOf(e: Expr, env: TypeEnv): Type = e match {
    case Lambda(LiteralAny(v), typ, e) => Func(typ, typeOf(e, env + (v -> typ)))
    case Apply(e1, e2) => typeOfApply(Apply(e1, e2), env)
    case _ => super.typeOf(e, env)
  }

  def typeOfApply(e: Apply, env: TypeEnv): Type = (typeOf(e.e1, env), typeOf(e.e2, env)) match {
    case (Func(typ1, typ2), typ3) =>
      if (typ1 == typ3) {
        typ2
      } else {
        IncompatibleTypeErrorType(typ1, typ3)
      }
    case (typ1, _) => ApplyToNonFunctionErrorType(typ1)
  }

  override def prettyPrint(e: Expr): String = e match {
    case Lambda(v, typ, e) => s"λ$v:${prettyPrint(typ)}. ${prettyPrint(e)}"
    case Apply(e1, e2) => s"${prettyPrint(e1)} ${prettyPrint(e2)}"
    case _ => super.prettyPrint(e)
  }

  override def prettyPrint(v: Value): Variable = v match {
    case LambdaV(v, inputType, e, env) => s"λ$v. $e"
    case _ => super.prettyPrint(v)
  }

  override def prettyPrint(t: Type): Variable = t match {
    case Func(in, out) => s"${prettyPrint(in)} → ${prettyPrint(out)}"
    case ApplyToNonFunctionErrorType(typ) => s"CannotApplyError(${prettyPrint(typ)})"
    case IncompatibleTypeErrorType(typ1, typ2) => s"IncompatibleTypes(${prettyPrint(typ1)}, ${prettyPrint(typ2)})"
    case _ => super.prettyPrint(t)
  }

  override def calculateExprClassList: List[Class[Expr]] = {
    super.calculateExprClassList ++ List(classOf[Lambda], classOf[Apply]).map(_.asInstanceOf[Class[Expr]])
  }

  override def calculateTypeClassList: List[Class[Type]] = {
    super.calculateTypeClassList ++ List(classOf[Func]).map(_.asInstanceOf[Class[Type]])
  }
}

object LLam extends LLam {}
