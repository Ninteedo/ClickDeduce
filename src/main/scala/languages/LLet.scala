package languages

class LLet extends LIf {
  // expressions

  case class Var(v: Literal) extends Expr

  object Var {
    def apply(v: Variable): Var = new Var(LiteralString(v))
  }

  case class Let(v: Literal, assign_expr: Expr, bound_expr: Expr) extends Expr

  object Let {
    def apply(v: Variable, assign_expr: Expr, bound_expr: Expr): Let = new Let(
      LiteralString(v), assign_expr, bound_expr
    )
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
    case Var(LiteralAny(v)) => env.getOrElse(v, UnknownVariableEvalError(LiteralAny(v)))
    case Let(LiteralAny(v), assign_expr, bound_expr) => {
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
    case Var(LiteralAny(v)) => tenv.getOrElse(v, UnknownVariableTypeError(LiteralAny(v)))
    case Let(LiteralAny(v), assign_expr, bound_expr) => {
      val assign_type: Type = typeOf(assign_expr, tenv)
      if (assign_type.isError) {
        assign_type
      } else {
        typeOf(bound_expr, tenv + (v -> assign_type))
      }
    }
    case _ => super.typeOf(e, tenv)
  }

  override def prettyPrint(e: Expr): String = e match {
    case Var(v) => v.toString
    case Let(v, assign_expr, bound_expr) => s"let $v = ${prettyPrint(assign_expr)} in ${prettyPrint(bound_expr)}"
    case _ => super.prettyPrint(e)
  }

  override def prettyPrint(t: Type): String = t match {
    case UnknownVariableTypeError(v) => s"UnknownVariable($v)"
    case _ => super.prettyPrint(t)
  }

  override def calculateExprClassList: List[Class[Expr]] = {
    super.calculateExprClassList ++ List(classOf[Var], classOf[Let]).map(_.asInstanceOf[Class[Expr]])
  }
}

object LLet extends LLet {}
