package languages

import convertors.*
import convertors.text.*
import languages.env.*
import languages.terms.*
import languages.terms.builders.*
import languages.terms.errors.*
import languages.terms.exprs.Expr
import languages.terms.literals.*
import languages.terms.types.Type
import languages.terms.values.Value

class LIf extends LArith {
  registerTerms("LIf", List(Bool, Equal, LessThan, IfThenElse, BoolType, BoolV))

  // expressions
  case class Bool(b: LiteralBool) extends Expr {
    override def evalInner(env: ValueEnv): Value = BoolV(b.value)

    override def typeCheckInner(tEnv: TypeEnv): Type = BoolType()

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = b.toText

    override def getRulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .setConclusion(MathElement("b"), BoolType().toText)
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(EvalRulePart(TermCommons.v, TermCommons.v))
      )
      .buildOption
  }

  object Bool extends ExprCompanion {
    def apply(b: Boolean): Bool = new Bool(LiteralBool(b))

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(b: LiteralBool) => Some(Bool(b))
      case Nil              => Some(Bool(LiteralBool(false)))
      case _                => None
    }

    override val aliases: List[String] = List("Boolean", "True", "False")
  }

  private def formatEquals(e1: ConvertableText, e2: ConvertableText): ConvertableText =
    MultiElement(e1, MathElement.doubleEquals.spacesAround, e2)

  case class Equal(e1: Expr, e2: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = {
      val v1 = e1.eval(env)
      val v2 = e2.eval(env)
      if (v1.isError) {
        v1
      } else if (v2.isError) {
        v2
      } else if (v1.typ == v2.typ) {
        BoolV(v1 == v2)
      } else {
        TypeMismatchError("Equal", v1.typ, v2.typ)
      }
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = {
      val t1 = e1.typeCheck(tEnv)
      val t2 = e2.typeCheck(tEnv)
      if (t1.isError) {
        t1
      } else if (t2.isError) {
        t2
      } else if (t1 == t2) {
        BoolType()
      } else {
        TypeMismatchType(t1, t2)
      }
    }

    override def toText: ConvertableText = formatEquals(e1.toTextBracketed, e2.toTextBracketed)

    override def getRulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .setConclusion(formatEquals(TermCommons.e(1), TermCommons.e(2)), BoolType().toText)
          .addAssumption(TermCommons.e(1), Symbols.tau)
          .addAssumption(TermCommons.e(2), Symbols.tau)
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(formatEquals(TermCommons.e(1), TermCommons.e(2)), TextElement("true"))
          .addAssumption(TermCommons.e(1), TermCommons.v)
          .addAssumption(TermCommons.e(2), TermCommons.v)
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(formatEquals(TermCommons.e(1), TermCommons.e(2)), TextElement("false"))
          .addAssumption(TermCommons.e(1), TermCommons.v(1))
          .addAssumption(TermCommons.e(2), TermCommons.v(2))
          .addAssumption(EvalRulePart(MultiElement(TermCommons.v(1), MathElement.notEquals.spacesAround, TermCommons.v(2))))
      )
      .buildOption
  }

  object Equal extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e1: Expr, e2: Expr) => Some(Equal(e1, e2))
      case Nil                      => Some(Equal(defaultExpr, defaultExpr))
      case _                        => None
    }

    override val aliases: List[String] = List("==")
  }

  case class LessThan(e1: Expr, e2: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = (e1.eval(env), e2.eval(env)) match {
      case (v1: OrdinalValue, v2: OrdinalValue) => BoolV(v1.compare(v2) < 0)
      case (v1, v2)                             => ComparisonWithNonOrdinalError(v1.typ, v2.typ)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = (e1.typeCheck(tEnv), e2.typeCheck(tEnv)) match {
      case (t1: OrdinalType, t2: OrdinalType) => BoolType()
      case (t1, t2)                           => ComparisonWithNonOrdinalType(t1, t2)
    }

    override def toText: ConvertableText =
      MultiElement(e1.toTextBracketed, SurroundSpaces(MathElement.lessThan), e2.toTextBracketed)
  }

  object LessThan extends ExprCompanion {
    override val aliases: List[String] = List("<", "LT")

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e1: Expr, e2: Expr) => Some(LessThan(e1, e2))
      case Nil                      => Some(LessThan(defaultExpr, defaultExpr))
      case _                        => None
    }
  }

  private def formatIfThenElse(cond: ConvertableText, thenExpr: ConvertableText, elseExpr: ConvertableText): ConvertableText =
    MultiElement(
      TextElement("if "),
      cond,
      TextElement(" then "),
      thenExpr,
      TextElement(" else "),
      elseExpr
    )

  case class IfThenElse(cond: Expr, then_expr: Expr, else_expr: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = cond.eval(env) match {
      case BoolV(true)    => then_expr.eval(env)
      case BoolV(false)   => else_expr.eval(env)
      case v if v.isError => v
      case v              => TypeMismatchError("IfThenElse", v.typ, BoolType())
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = cond.typeCheck(tEnv) match {
      case BoolType() =>
        val t1 = then_expr.typeCheck(tEnv)
        val t2 = else_expr.typeCheck(tEnv)
        if (t1 == t2) t1
        else TypeMismatchType(t1, t2)
      case t => TypeMismatchType(t, BoolType())
    }

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] = cond.eval(env) match {
      case BoolV(true)  => List((cond, env), (then_expr, env))
      case BoolV(false) => List((cond, env), (else_expr, env))
      case _            => List((cond, env), (then_expr, env), (else_expr, env))
    }

    override def toText: ConvertableText =
      formatIfThenElse(cond.toTextBracketed, then_expr.toTextBracketed, else_expr.toTextBracketed)

    override def getRulePreview: Option[RulePreview] = {
      val exprText = formatIfThenElse(TermCommons.e, TermCommons.e(1), TermCommons.e(2))
      RulePreviewBuilder()
        .addTypeCheckRule(
          TypeCheckRuleBuilder()
            .setConclusion(exprText, Symbols.tau)
            .addAssumption(TermCommons.e, BoolType().toText)
            .addAssumption(TermCommons.e(1), Symbols.tau)
            .addAssumption(TermCommons.e(2), Symbols.tau)
        )
        .addEvaluationRule(
          EvalRuleBuilder()
            .setConclusion(exprText, TermCommons.v(1))
            .addAssumption(TermCommons.e, TextElement("true"))
            .addAssumption(TermCommons.e(1), TermCommons.v(1))
        )
        .addEvaluationRule(
          EvalRuleBuilder()
            .setConclusion(exprText, TermCommons.v(2))
            .addAssumption(TermCommons.e, TextElement("false"))
            .addAssumption(TermCommons.e(2), TermCommons.v(2))
        )
        .buildOption
    }
  }

  object IfThenElse extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(cond: Expr, then_expr: Expr, else_expr: Expr) => Some(IfThenElse(cond, then_expr, else_expr))
      case Nil                                                => Some(IfThenElse(defaultExpr, defaultExpr, defaultExpr))
      case _                                                  => None
    }
  }

  // values
  case class BoolV(b: Boolean) extends Value {
    override val typ: Type = BoolType()

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = TextElement(b.toString)
  }

  object BoolV extends ValueCompanion {
  }

  // types
  case class BoolType() extends Type {
    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = TextElement("Bool")
  }

  object BoolType extends TypeCompanion {
    override def create(args: BuilderArgs): Option[Type] = args match {
      case Nil => Some(BoolType())
      case _   => None
    }

    override val aliases: List[String] = List("Boolean", "True", "False")
  }

  // errors

  case class TypeMismatchType(type1: Type, type2: Type) extends TypeError {
    override val message: String = s"$type1 not compatible with $type2"
  }

  case class TypeMismatchError(exprName: String, type1: Type, type2: Type) extends EvalError {
    override val message: String = s"$type1 not compatible with $type2 in $exprName"

    override val typ: Type = TypeMismatchType(type1, type2)
  }

  case class ComparisonWithNonOrdinalError(type1: Type, type2: Type) extends EvalError {
    override val message: String = s"$type1 or $type2 is not an ordinal type"

    override val typ: Type = ComparisonWithNonOrdinalType(type1, type2)
  }

  case class ComparisonWithNonOrdinalType(type1: Type, type2: Type) extends TypeError {
    override val message: String = s"$type1 or $type2 is not an ordinal type"
  }

  // tasks
  setTasks(SimpleBoolTask, IfStatementTask, IfAndComparisonTask)

  private object SimpleBoolTask extends Task {
    override val name: String = "Enter a Boolean"
    override val description: String = "Boolean values must be typed in as either \"True\" or \"False\", case insensitive."
    override val difficulty: Int = 1

    override def checkFulfilled(expr: Expr): Boolean = expr match {
      case Bool(LiteralBool(_)) => true
      case e => e.getExprFields.exists(checkFulfilled)
    }
  }

  private object IfStatementTask extends Task {
    override val name: String = "Create an If Statement"
    override val description: String = "Create an if statement, filling out its condition and both possible subexpressions. " +
      "During evaluation, only the matching one is evaluated. " +
      "The condition must be a boolean expression, and the subexpressions must have the same type."
    override val difficulty: Int = 2

    override def checkFulfilled(expr: Expr): Boolean = {
      def checkIfStatementComplete(e: IfThenElse) = e.cond.eval() match {
        case BoolV(_) => e.then_expr.typeCheck() == e.else_expr.typeCheck() && !e.then_expr.typeCheck().isError
        case _ => false
      }

      def checkForValidIfStatement(e: Expr): Boolean = e match {
        case e: IfThenElse => checkIfStatementComplete(e) || e.getExprFields.exists(checkForValidIfStatement)
        case _ => e.getExprFields.exists(checkForValidIfStatement)
      }

      checkForValidIfStatement(expr)
    }
  }

  private object IfAndComparisonTask extends Task {
    override val name: String = "Use an If and a comparison inside a condition"
    override val description: String = "Create an if statement with a comparison and another if statement inside the condition. " +
      "The expression must successfully type check."
    override val difficulty: Int = 3

    override def checkFulfilled(expr: Expr): Boolean = {
      def checkForValidIfStatement(e: Expr): Boolean = e match {
        case e: IfThenElse => (checkHasOp(e.cond, classOf[LessThan]) || checkHasOp(e.cond, classOf[Equal])) &&
          e.getExprFields.exists(checkHasOp(_, classOf[IfThenElse]))
        case _ => e.getExprFields.exists(checkForValidIfStatement)
      }

      checkForValidIfStatement(expr) && !expr.typeCheck().isError
    }
  }
}

object LIf extends LIf {}
