package languages

class LLet extends LIf {
  // expressions

  case class Var(v: Literal) extends Expr {
    override def eval(env: Env): Value = v match {
      case LiteralIdentifier(identifier) => env.getOrElse(identifier, UnknownVariableEvalError(v))
      case _ => InvalidIdentifierEvalError(v)
    }

    override def typeCheck(tEnv: TypeEnv): Type = v match {
      case LiteralIdentifier(identifier) => tEnv.getOrElse(identifier, UnknownVariableTypeError(v))
      case _ => InvalidIdentifierTypeError(v)
    }
  }

  object Var {
    def apply(v: Variable): Var = new Var(Literal.fromString(v))
  }

  case class Let(v: Literal, assign_expr: Expr, bound_expr: Expr) extends Expr {
    override def eval(env: Env): Value = v match {
      case LiteralIdentifier(identifier) => {
        val assign_val: Value = assign_expr.eval(env)
        if (assign_val.isError) {
          assign_val
        } else {
          bound_expr.eval(env + (identifier -> assign_val))
        }
      }
      case _ => InvalidIdentifierEvalError(v)
    }

    override def typeCheck(tEnv: TypeEnv): Type = v match {
      case LiteralIdentifier(identifier) => {
        val assign_type: Type = assign_expr.typeCheck(tEnv)
        if (assign_type.isError) {
          assign_type
        } else {
          bound_expr.typeCheck(tEnv + (identifier -> assign_type) )
        }
      }
      case _ => InvalidIdentifierTypeError(v)
    }

    override def getChildrenBase(env: Env): List[(Term, Env)] =
      List((v, env), (assign_expr, env), (bound_expr, env + (v.toString -> assign_expr.eval(env))))

    override def getChildrenEval(env: Env): List[(Term, Env)] = List(
      (assign_expr, env), (bound_expr, env + (v.toString -> assign_expr.eval(env)))
    )

    override def getChildrenTypeCheck(tenv: TypeEnv): List[(Term, TypeEnv)] = List(
      (assign_expr, tenv), (bound_expr, tenv + (v.toString -> assign_expr.typeCheck(tenv)))
    )
  }

  object Let {
    def apply(v: Variable, assign_expr: Expr, bound_expr: Expr): Let = new Let(
      Literal.fromString(v), assign_expr, bound_expr
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

  case class InvalidIdentifierEvalError(v: Literal) extends EvalError {
    override val message: String = s"Invalid identifier '$v'"

    override val typ: Type = InvalidIdentifierTypeError(v)
  }

  case class InvalidIdentifierTypeError(v: Literal) extends TypeError {
    override val message: String = s"Invalid identifier '$v'"
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
