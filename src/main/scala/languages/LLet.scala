package languages

class LLet extends LIf {
  // expressions

  case class Var(v: Literal) extends Expr {
    override def evalInner(env: Env): Value = v match {
      case LiteralIdentifier(identifier) => env.getOrElse(identifier, UnknownVariableEvalError(v))
      case _                             => InvalidIdentifierEvalError(v)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = v match {
      case LiteralIdentifier(identifier) => tEnv.getOrElse(identifier, UnknownVariableTypeError(v))
      case _                             => InvalidIdentifierTypeError(v)
    }

    override def prettyPrint: String = v.toString

    override val needsBrackets: Boolean = false
  }

  object Var {
    def apply(v: Variable): Var = new Var(Literal.fromString(v))
  }

  case class Let(v: Literal, assign: Expr, bound: Expr) extends Expr {
    override def evalInner(env: Env): Value = v match {
      case LiteralIdentifier(identifier) =>
        val assign_val: Value = assign.eval(env)
        if (assign_val.isError) assign_val else bound.eval(env + (identifier -> assign_val))
      case _ => InvalidIdentifierEvalError(v)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = v match {
      case LiteralIdentifier(identifier) =>
        val assign_type: Type = assign.typeCheck(tEnv)
        if (assign_type.isError) assign_type else bound.typeCheck(tEnv + (identifier -> assign_type))
      case _ => InvalidIdentifierTypeError(v)
    }

    override def getChildrenBase(env: Env): List[(Term, Env)] =
      List((v, env), (assign, env), (bound, env + (v.toString -> assign.eval(env))))

    override def getChildrenEval(env: Env): List[(Term, Env)] =
      List((assign, env), (bound, env + (v.toString -> assign.eval(env))))

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] =
      List((assign, tEnv), (bound, tEnv + (v.toString -> assign.typeCheck(tEnv))))

    override def prettyPrint: String = s"let $v = ${assign.prettyPrintBracketed} in ${bound.prettyPrintBracketed}"
  }

  object Let {
    def apply(v: Variable, assign: Expr, bound: Expr): Let = new Let(Literal.fromString(v), assign, bound)
  }

  // errors

  case class UnknownVariableEvalError(v: Literal) extends EvalError {
    override val message: String = s"Unknown variable identifier '$v'"

    override val typ: Type = UnknownVariableTypeError(v)
  }

  case class UnknownVariableTypeError(v: Literal) extends TypeError {
    override val message: String = s"Unknown variable identifier '$v'"

    override def prettyPrint: String = s"UnknownVariable($v)"
  }

  case class InvalidIdentifierEvalError(v: Literal) extends EvalError {
    override val message: String = s"Invalid identifier '$v'"

    override val typ: Type = InvalidIdentifierTypeError(v)
  }

  case class InvalidIdentifierTypeError(v: Literal) extends TypeError {
    override val message: String = s"Invalid identifier '$v'"
  }

  override def calculateExprClassList: List[Class[Expr]] = {
    super.calculateExprClassList ++ List(classOf[Var], classOf[Let]).map(_.asInstanceOf[Class[Expr]])
  }
}

object LLet extends LLet {}
