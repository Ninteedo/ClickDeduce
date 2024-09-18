package languages

import convertors.*

class LLet extends LIf {
  Var.register()
  Let.register()

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

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = v.toText
  }

  object Var extends ExprCompanion {
    def apply(v: Variable): Var = new Var(Literal.fromString(v))

    override def createExpr(args: List[Any]): Option[Expr] = args match {
      case List(v: Literal) => Some(Var(v))
      case Nil              => Some(Var(defaultLiteral))
      case _                => None
    }

    override val aliases: List[String] = List("Variable", "X")
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

    override def toText: ConvertableText =
      MultiElement(
        TextElement("let "),
        v.toText,
        SurroundSpaces(MathElement.equals),
        assign.toTextBracketed,
        TextElement(" in "),
        bound.toTextBracketed
      )
  }

  object Let extends ExprCompanion {
    def apply(v: Variable, assign: Expr, bound: Expr): Let = new Let(Literal.fromString(v), assign, bound)

    override def createExpr(args: List[Any]): Option[Expr] = args match {
      case List(v: Literal, assign: Expr, bound: Expr) => Some(Let(v, assign, bound))
      case Nil                                         => Some(Let(defaultLiteral, defaultExpr, defaultExpr))
      case _                                           => None
    }

    override val aliases: List[String] = List("=")
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

  // tasks

  clearTasks()
  LetAndVarTask.register()
  UseVarInAssignmentTask.register()
  OverwriteVarTask.register()

  private def checkHasVar(e: Expr, v: Literal): Boolean = checkCondition(
    e, cond = {
      case Var(v2) => v == v2
      case _ => false
    }
  )

  private object LetAndVarTask extends Task {
    override val name: String = "Bind and use a variable"
    override val description: String =
      "Bind a value to a variable using \"Let\", then use that variable in an expression using \"Var\". " +
        "The variable name needs to be identical in both expressions. " +
        "The \"Var\" expression must be inside the right-hand side of the \"Let\" expression. " +
        "The expression must successfully type-check."
    override val difficulty: Int = 2

    override def checkFulfilled(expr: Expr): Boolean = {
      !expr.typeCheck().isError && checkCondition(
        expr, cond = {
          case Let(v, _, bound) => checkHasVar(bound, v)
          case _ => false
        }
      )
    }
  }

  private object UseVarInAssignmentTask extends Task {
    override val name: String = "Use a variable in an assignment"
    override val description: String =
      "Bind a value to a variable using \"Let\", then use that variable in the assignment of another variable, inside" +
        " another \"Let\". " +
        "The expression must successfully type-check."
    override val difficulty: Int = 3

    override def checkFulfilled(expr: Expr): Boolean = {
      !expr.typeCheck().isError && checkCondition(
        expr, cond = {
          case Let(v1, _, bound1) =>
            checkCondition(
              bound1,
              {
                case Let(_, assign2, _) => checkHasVar(assign2, v1)
                case _ => false
              }
            )
          case _ => false
        }
      )
    }
  }

  object OverwriteVarTask extends Task {
    override val name: String = "Overwrite a variable"
    override val description: String =
      "Bind a value to a variable using \"Let\", use it, then overwrite that variable with a new value using another " +
        "\"Let\" and use that new value. " +
        "The expression must successfully type-check."
    override val difficulty: Int = 3

    override def checkFulfilled(expr: Expr): Boolean = {
      def checkVarUsedNoOverwrite(e: Expr, v1: Literal): Boolean = e match {
        case Var(v2) => v1 == v2
        case Let(v2, assign, bound) => checkVarUsedNoOverwrite(assign, v1) || (v1 != v2 && checkVarUsedNoOverwrite(bound, v1))
        case e => e.getExprFields.exists(checkVarUsedNoOverwrite(_, v1))
      }

      !expr.typeCheck().isError && checkCondition(
        expr, cond = {
          case Let(v1, _, bound1) =>
            checkVarUsedNoOverwrite(bound1, v1) && checkCondition(
              bound1,
              {
                case Let(v2, _, bound2) => v1 == v2 && checkHasVar(bound2, v1)
                case _ => false
              }
            )
          case _ => false
        }
      )
    }
  }
}

object LLet extends LLet {}
