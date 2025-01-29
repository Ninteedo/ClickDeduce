package languages

import convertors.*
import convertors.text.*
import languages.env.*
import languages.env.Env.Variable
import languages.previews.*
import languages.terms.*
import languages.terms.builders.*
import languages.terms.errors.*
import languages.terms.exprs.Expr
import languages.terms.literals.*
import languages.terms.types.*
import languages.terms.values.*

class LLet extends LIf {
  registerTerms("LLet", List(Var, Let))

  // expressions

  case class Var(v: LiteralIdentifierLookup) extends Expr {
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
    def apply(v: Variable): Var = new Var(LiteralIdentifierLookup(v))

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(v: LiteralIdentifierLookup) => Some(Var(v))
      case Nil              => Some(Var(LiteralIdentifierLookup("")))
      case _                => None
    }

    override val aliases: List[String] = List("Variable", "X")

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .setConclusion(TermCommons.x, TermCommons.t)
          .addAssumption(TypeCheckRulePart(MultiElement(Symbols.gamma, BracketedElement(TermCommons.x), MathElement.equals.spacesAround, TermCommons.t)))
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(TermCommons.x, MultiElement(Symbols.sigma, BracketedElement(TermCommons.x)))
      )
      .buildOption
  }

  private def formatLet(v: ConvertableText, assign: ConvertableText, bound: ConvertableText): ConvertableText =
    MultiElement(TextElement("let "), v, SurroundSpaces(MathElement.equals), assign, TextElement(" in "), bound)

  case class Let(v: LiteralIdentifierBind, assign: Expr, bound: Expr) extends Expr {
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

    override def toText: ConvertableText = formatLet(v.toText, assign.toTextBracketed, bound.toTextBracketed)
  }

  object Let extends ExprCompanion {
    def apply(v: Variable, assign: Expr, bound: Expr): Let = new Let(LiteralIdentifierBind(v), assign, bound)

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(v: LiteralIdentifierBind, assign: Expr, bound: Expr) => Some(Let(v, assign, bound))
      case Nil => Some(Let(LiteralIdentifierBind.default, defaultExpr, defaultExpr))
      case _   => None
    }

    override val aliases: List[String] = List("=")

    override lazy val rulePreview: Option[RulePreview] = {
      val exprText = formatLet(TermCommons.x, TermCommons.e(1), TermCommons.e(2))
      RulePreviewBuilder()
        .addTypeCheckRule(
          TypeCheckRuleBuilder()
            .setConclusion(exprText, TermCommons.t(2))
            .addAssumption(TermCommons.e(1), TermCommons.t(1))
            .addAssumption(TermCommons.e(2), TermCommons.t(2), List(TypeCheckRuleBind(TermCommons.x, TermCommons.t(1))))
        )
        .addEvaluationRule(
          EvalRuleBuilder()
            .setConclusion(exprText, TermCommons.v(2))
            .addAssumption(TermCommons.e(1), TermCommons.v(1))
            .addAssumption(TermCommons.e(2), TermCommons.v(2), List(EvalRuleBind(TermCommons.x, TermCommons.v(1))))
        )
        .buildOption
    }
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
      case Var(v2) => v2.identEquals(v)
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
        case Var(v2) => v2.identEquals(v2)
        case Let(v2, assign, bound) =>
          checkVarUsedNoOverwrite(assign, v1) || (!v2.identEquals(v1) && checkVarUsedNoOverwrite(bound, v1))
        case e => e.getExprFields.exists(checkVarUsedNoOverwrite(_, v1))
      }

      !expr.typeCheck().isError && checkCondition(
        expr,
        cond = {
          case Let(v1, _, bound1) =>
            checkVarUsedNoOverwrite(bound1, v1) && checkCondition(
              bound1,
              {
                case Let(v2, _, bound2) => v1.identEquals(v2) && checkHasVar(bound2, v1)
                case _                  => false
              }
            )
          case _ => false
        }
      )
    }
  }

  protected class LLetParser extends LIfParser {
    protected def keywords: Set[String] = Set(
      "let", "in", "if", "then", "else", "true", "false"
    )

    private lazy val keywordsCache: Set[String] = keywords

    private def varParse: Parser[Expr] = ident.filter(!keywordsCache.contains(_)) ^^ {Var(_)}

    protected def let: Parser[Expr] = "let" ~> ident ~ ("=" ~> expr) ~ ("in" ~> expr) ^^ {
      case v ~ assign ~ bound => Let(v, assign, bound)
    }

    override protected def primitive: Parser[Expr] = super.primitive | varParse

    override protected def exprOperators: List[ExprOperator] = super.exprOperators ++ List(
      SpecialParser(let, 1)
    )
  }

  override protected val exprParser: ExprParser = new LLetParser
}

object LLet extends LLet {}
