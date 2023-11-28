package languages

class LLet extends LIf {
  // expressions

  case class Var(v: Literal) extends Expr

  object Var {
    def apply(v: Variable): Var = new Var(LiteralString(v))
  }

  case class Let(v: Literal, assign_expr: Expr, bound_expr: Expr) extends Expr

  object Let {
    def apply(v: Variable, assign_expr: Expr, bound_expr: Expr): Let = new Let(LiteralString(v), assign_expr, bound_expr)
  }

  // errors

  case class UnknownVariableEvalError(v: Literal) extends EvalError {
    override val message: String = s"Unknown variable identifier '$v'"

    override val typ: Type = UnknownVariableTypeError(v)
  }

  case class UnknownVariableTypeError(v: Literal) extends TypeError {
    override val message: String = s"Unknown variable identifier '$v'"
  }

  override def eval(e: Expr, env: Env): Value = e match {
    case Var(LiteralString(v)) => env.getOrElse(v, UnknownVariableEvalError(LiteralString(v)))
    case Let(LiteralString(v), assign_expr, bound_expr) => {
      val assign_val: Value = eval(assign_expr, env)
      if (assign_val.isError) {
        assign_val
      } else {
        eval(bound_expr, env + (v -> assign_val))
      }
    }
    case _ => super.eval(e, env)
  }

  override def typeOf(e: Expr, tenv: TypeEnv): Type = e match {
    case Var(LiteralString(v)) => tenv.getOrElse(v, UnknownVariableTypeError(LiteralString(v)))
    case Let(LiteralString(v), assign_expr, bound_expr) => {
      val assign_type: Type = typeOf(assign_expr, tenv)
      if (assign_type.isError) {
        assign_type
      } else {
        typeOf(bound_expr, tenv + (v -> assign_type))
      }
    }
    case _ => super.typeOf(e, tenv)
  }
}

object LLet extends LLet {}
