package languages

import convertors.*

class LLet extends LIf {
  registerTerms("LLet", List(Var, Let))

  // expressions

  case class Var(v: LiteralIdentifier) extends Expr {
    override def evalInner(env: ValueEnv): Value = guardValidIdentifierEval(v,
      env.get(v.value) match {
        case None => UnknownVariableEvalError(v)
        case Some(TypeValueContainer(typ)) => VariableOnlyEvalError(v)
        case Some(value) => value
      }
    )

    override def typeCheckInner(tEnv: TypeEnv): Type = guardValidIdentifierType(v,
      tEnv.get(v.value) match {
        case None                     => UnknownVariableTypeError(v)
        case Some(TypeContainer(typ)) => VariableOnlyTypeError(v)
        case Some(typ)                => typ
      }
    )

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = v.toText
  }

  object Var extends ExprCompanion {
    def apply(v: Variable): Var = new Var(LiteralIdentifier(v))

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(v: LiteralIdentifier) => Some(Var(v))
      case Nil              => Some(Var(LiteralIdentifier("")))
      case _                => None
    }

    override val aliases: List[String] = List("Variable", "X")
  }

  case class Let(v: LiteralIdentifier, assign: Expr, bound: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = guardValidIdentifierEval(v, {
      val assign_val: Value = assign.eval(env)
      if (assign_val.isError) assign_val else bound.eval(env + (v -> assign_val))
    })

    override def typeCheckInner(tEnv: TypeEnv): Type = guardValidIdentifierType(v, {
      val assign_type: Type = assign.typeCheck(tEnv)
      if (assign_type.isError) assign_type else bound.typeCheck(tEnv + (v -> assign_type))
    })

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] =
      List((v, env), (assign, env), (bound, env + (v -> assign.eval(env))))

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] =
      List((assign, env), (bound, env + (v -> assign.eval(env))))

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] =
      List((assign, tEnv), (bound, tEnv + (v -> assign.typeCheck(tEnv))))

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
    def apply(v: Variable, assign: Expr, bound: Expr): Let = new Let(LiteralIdentifier(v), assign, bound)

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(v: LiteralIdentifier, assign: Expr, bound: Expr) => Some(Let(v, assign, bound))
      case Nil => Some(Let(LiteralIdentifier(""), defaultExpr, defaultExpr))
      case _   => None
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

  // helpers

  protected def guardValidIdentifierEval(v: LiteralIdentifier, f: => Value): Value =
    if (!v.validIdentifier) InvalidIdentifierEvalError(v) else f

  protected def guardValidIdentifierType(v: LiteralIdentifier, f: => Type): Type =
    if (!v.validIdentifier) InvalidIdentifierTypeError(v) else f

  // tasks
  setTasks(LetAndVarTask, UseVarInAssignmentTask, OverwriteVarTask)

  private def checkHasVar(e: Expr, v: Literal): Boolean = checkCondition(
    e,
    cond = {
      case Var(v2) => v == v2
      case _       => false
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
        expr,
        cond = {
          case Let(v, _, bound) => checkHasVar(bound, v)
          case _                => false
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
        expr,
        cond = {
          case Let(v1, _, bound1) =>
            checkCondition(
              bound1,
              {
                case Let(_, assign2, _) => checkHasVar(assign2, v1)
                case _                  => false
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
        case Let(v2, assign, bound) =>
          checkVarUsedNoOverwrite(assign, v1) || (v1 != v2 && checkVarUsedNoOverwrite(bound, v1))
        case e => e.getExprFields.exists(checkVarUsedNoOverwrite(_, v1))
      }

      !expr.typeCheck().isError && checkCondition(
        expr,
        cond = {
          case Let(v1, _, bound1) =>
            checkVarUsedNoOverwrite(bound1, v1) && checkCondition(
              bound1,
              {
                case Let(v2, _, bound2) => v1 == v2 && checkHasVar(bound2, v1)
                case _                  => false
              }
            )
          case _ => false
        }
      )
    }
  }
}

object LLet extends LLet {}
