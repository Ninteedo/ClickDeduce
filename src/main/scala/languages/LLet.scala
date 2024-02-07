package languages

class LLet extends LIf {
  // expressions

  case class Var(v: Literal) extends Expr {
    override def evalInner(env: ValueEnv): Value = v match {
      case LiteralIdentifier(identifier) =>
        env.get(identifier) match {
          case None                          => UnknownVariableEvalError(v)
          case Some(TypeValueContainer(typ)) => VariableOnlyEvalError(v)
          case Some(value)                   => value
        }
      case _ => InvalidIdentifierEvalError(v)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = v match {
      case LiteralIdentifier(identifier) =>
        tEnv.get(identifier) match {
          case None                     => UnknownVariableTypeError(v)
          case Some(TypeContainer(typ)) => VariableOnlyTypeError(v)
          case Some(typ)                => typ
        }
      case _ => InvalidIdentifierTypeError(v)
    }

    override def prettyPrint: String = v.toString

    override val needsBrackets: Boolean = false
  }

  object Var {
    def apply(v: Variable): Var = new Var(Literal.fromString(v))
  }

  case class Let(v: Literal, assign: Expr, bound: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = v match {
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

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] =
      List((v, env), (assign, env), (bound, env + (v.toString -> assign.eval(env))))

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] =
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
  }

  case class VariableOnlyTypeError(v: Literal) extends TypeError {
    override val message: String = s"Variable '$v' can only be used as a type"
  }

  case class VariableOnlyEvalError(v: Literal) extends EvalError {
    override val message: String = s"Variable '$v' can only be used as a type"

    override val typ: Type = VariableOnlyTypeError(v)
  }

  case class InvalidIdentifierEvalError(v: Literal) extends EvalError {
    override val message: String = s"Invalid identifier '$v'"

    override val typ: Type = InvalidIdentifierTypeError(v)
  }

  case class InvalidIdentifierTypeError(v: Literal) extends TypeError {
    override val message: String = s"Invalid identifier '$v'"
  }
  
  addExprBuilder(
    "Let",
    {
      case List(v: Literal, assign: Expr, bound: Expr) => Some(Let(v, assign, bound))
      case Nil                                         => Some(Let(defaultLiteral, defaultExpr, defaultExpr))
      case _                                           => None
    }
  )

  addExprBuilder(
    "Var",
    {
      case List(v: Literal) => Some(Var(v))
      case Nil              => Some(Var(defaultLiteral))
      case _                => None
    }
  )
}

object LLet extends LLet {}
