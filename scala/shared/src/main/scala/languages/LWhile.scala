package languages

import convertors.*
import convertors.text.*
import languages.env.*
import languages.env.Env.Variable
import languages.terms.*
import languages.terms.builders.*
import languages.terms.errors.*
import languages.terms.exprs.Expr
import languages.terms.literals.*
import languages.terms.types.*
import languages.terms.values.*

class LWhile extends LList {
  registerTerms("LWhile", List(
    SkipStmt,
    SeqStmt,
    IfStmt,
    WhileStmt,
    AssignStmt,
  ))

  /**
   * A statement from the LWhile language.
   * Instead of evaluating to a regular value, statements modify the environment.
   * In practice, they are treated as expressions that evaluate to an [[EnvValue]].
   */
  trait Stmt extends Expr {
    def newEnv(env: ValueEnv): ValueEnv | EvalError

    def newTEnv(tEnv: TypeEnv): TypeEnv | TypeError

    override protected def evalInner(env: ValueEnv): Value = newEnv(env) match {
      case env: ValueEnv => EnvValue(env)
      case error: EvalError => error
    }

    override protected def typeCheckInner(tEnv: TypeEnv): Type = newTEnv(tEnv) match {
      case env: TypeEnv => EnvType(env)
      case error: TypeError => error
    }

    def assignments: Set[Variable]
  }

  private def safeAssignments(stmt: Expr): Set[Variable] = stmt match {
    case stmt: Stmt => stmt.assignments
    case _ => Set.empty
  }

  /**
   * Helper function to ensure that an expression is a statement, returning an error if it is not.
   * @param e The expression that should be a statement
   * @param f The function to apply to the statement
   * @param els The function to apply to the expression if it is not a statement (default: return an error)
   * @tparam A The return type of the function
   * @tparam B The return type of the error function
   * @return The result of applying the function to the statement, or an error if the expression is not a statement
   */
  protected def stmtOnly[A, B](e: Expr, f: Stmt => A, els: Expr => B = NotAStmtError(_)): A | B = e match {
    case stmt: Stmt => f(stmt)
    case _ => els(e)
  }

  protected def stmtOnlyT[A, B](e: Expr, f: Stmt => A, els: Expr => B = NotAStmtTypeError(_)): A | B = e match {
    case stmt: Stmt => f(stmt)
    case _ => els(e)
  }

  /**
   * Helper function to safely get the new environment from a statement.
   * @param stmt The statement
   * @param env The starting environment
   * @param f The function to apply to the new environment
   * @tparam A The return type of the function
   * @return The result of applying the function to the new environment, or an error if the statement is not valid
   */
  protected def stmtNewEnv[A, B](stmt: Stmt, env: ValueEnv, f: ValueEnv => A, els: EvalError => B = identity): A | B = {
    stmt.newEnv(env) match {
      case env: ValueEnv => f(env)
      case error: EvalError => els(error)
    }
  }

  protected def stmtNewTEnv[A, B](stmt: Stmt, tEnv: TypeEnv, f: TypeEnv => A, els: TypeError => B = identity): A | B = {
    stmt.newTEnv(tEnv) match {
      case tEnv: TypeEnv => f(tEnv)
      case error: TypeError => els(error)
    }
  }

  case class SkipStmt() extends Stmt {
    override def newEnv(env: ValueEnv): ValueEnv = env

    override def newTEnv(tEnv: TypeEnv): TypeEnv | TypeError = tEnv

    override def assignments: Set[Variable] = Set.empty

    override def toText: ConvertableText = TextElement("skip")

    override val needsBrackets: Boolean = false
  }

  object SkipStmt extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = Some(SkipStmt())
  }

  case class SeqStmt(stmtA: Expr, stmtB: Expr) extends Stmt {
    override def newEnv(env: ValueEnv): ValueEnv | EvalError = stmtOnly(stmtA, stmtA =>
      stmtOnly(stmtB, stmtB =>
        stmtNewEnv(stmtA, env, env =>
          stmtB.newEnv(env)
        )
      )
    )

    override def newTEnv(tEnv: TypeEnv): TypeEnv | TypeError = stmtOnlyT(stmtA, stmtA =>
      stmtOnlyT(stmtB, stmtB =>
        stmtNewTEnv(stmtA, tEnv, tEnv =>
          stmtB.newTEnv(tEnv)
        )
      )
    )

    override def assignments: Set[Variable] = safeAssignments(stmtA) ++ safeAssignments(stmtB)

    private def defaultChildren(env: ValueEnv): List[(Term, ValueEnv)] = {
      List((stmtA, env), (stmtB, stmtOnly(stmtA, stmtA =>
        stmtNewEnv(stmtA, env, identity, _ => env),
        _ => env)
      ))
    }

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] = defaultChildren(env)

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] = defaultChildren(env)

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = {
      List((stmtA, tEnv), (stmtB, stmtOnlyT(stmtA, stmtA =>
        stmtNewTEnv(stmtA, tEnv, identity, _ => tEnv),
        _ => tEnv
      )))
    }

    override def toText: ConvertableText = MultiElement(
      stmtA.toText,
      TextElement("; "),
      stmtB.toText
    )
  }

  object SeqStmt extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(stmtA: Expr, stmtB: Expr) => Some(SeqStmt(stmtA, stmtB))
      case Nil => Some(SeqStmt(defaultExpr, defaultExpr))
      case _ => None
    }
  }

  case class IfStmt(cond: Expr, stmtT: Expr, stmtF: Expr) extends Stmt {
    override def newEnv(env: ValueEnv): ValueEnv | EvalError = cond.eval(env) match {
      case BoolV(true) => stmtOnly(stmtT, _.newEnv(env))
      case BoolV(false) => stmtOnly(stmtF, _.newEnv(env))
      case v => TypeMismatchError("IfStmt", v.typ, BoolType())
    }

    override def newTEnv(tEnv: TypeEnv): TypeEnv | TypeError = cond.typeCheck(tEnv) match {
      case BoolType() => stmtOnlyT(stmtT, stmtT => stmtOnlyT(stmtF, stmtF =>
        stmtNewTEnv(stmtT, tEnv, tEnvT =>
          stmtNewTEnv(stmtF, tEnv, tEnvF =>
            Env(tEnvT.toSet.intersect(tEnvF.toSet).toMap)  // overlap
          )
        )
      ))
      case typ => TypeMismatchType(typ, BoolType())
    }

    override def assignments: Set[Variable] = safeAssignments(stmtT) ++ safeAssignments(stmtF)

    override def toText: ConvertableText = MultiElement(
      TextElement("if "),
      cond.toTextBracketed,
      TextElement(" then "),
      stmtT.toTextBracketed,
      TextElement(" else "),
      stmtF.toTextBracketed
    )
  }

  object IfStmt extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(cond: Expr, stmtT: Expr, stmtF: Expr) => Some(IfStmt(cond, stmtT, stmtF))
      case Nil => Some(IfStmt(defaultExpr, defaultExpr, defaultExpr))
      case _ => None
    }
  }

  case class WhileStmt(cond: Expr, stmt: Expr) extends Stmt {
    override def newEnv(env: ValueEnv): ValueEnv | EvalError = cond.eval(env) match {
      case BoolV(true) =>
        stmtOnly(stmt, stmt =>
          stmtNewEnv(stmt, env, WhileStmt(cond, stmt).newEnv(_))
        )
      case BoolV(false) => env
      case v => TypeMismatchError("WhileStmt", v.typ, BoolType())
    }

    override def newTEnv(tEnv: TypeEnv): TypeEnv | TypeError = cond.typeCheck(tEnv) match {
      case BoolType() => stmtOnlyT(stmt, _.newTEnv(tEnv))
      case typ => TypeMismatchType(typ, BoolType())
    }

    override def assignments: Set[Variable] = safeAssignments(stmt)

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] = {
      // variables assigned in the while loop body are hidden in the edit environment for the body
      val bodyAssignments = safeAssignments(stmt)
      val bodyEnv = env.mapToEnv((v, value) =>
        v -> (
          if bodyAssignments.contains(v)
          then HiddenValue(value.typ)
          else value
        ))
      List((cond, env), (stmt, bodyEnv))
    }

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] = {
      val default = super.getChildrenEval(env)
      cond.eval(env) match {
        case BoolV(true) => stmtOnly(stmt, stmt =>
          stmtNewEnv(stmt, env, childEnv =>
            List((cond, env), (stmt, env), (this, childEnv)),
            _ => default),
          _ => default
        )
        case BoolV(false) => List((cond, env))
        case _ => default
      }
    }

    override def toText: ConvertableText = MultiElement(
      TextElement("while "),
      cond.toTextBracketed,
      TextElement(" do "),
      stmt.toTextBracketed
    )
  }

  object WhileStmt extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(cond: Expr, stmt: Expr) => Some(WhileStmt(cond, stmt))
      case Nil => Some(WhileStmt(defaultExpr, defaultExpr))
      case _ => None
    }
  }

  case class AssignStmt(v: LiteralIdentifierBind, e: Expr) extends Stmt {
    override def newEnv(env: ValueEnv): ValueEnv | EvalError = env + (v -> e.eval(env))

    override def newTEnv(tEnv: TypeEnv): TypeEnv | TypeError = tEnv + (v -> e.typeCheck(tEnv))

    override def assignments: Set[Variable] = Set(v.getValue)

    override def toText: ConvertableText = MultiElement(
      v.toText,
      TextElement(" := "),
      e.toTextBracketed
    )
  }

  object AssignStmt extends ExprCompanion {
    def apply(v: String, e: Expr): AssignStmt = AssignStmt(LiteralIdentifierBind(v), e)

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(v: LiteralIdentifierBind, e: Expr) => Some(AssignStmt(v, e))
      case Nil => Some(AssignStmt(LiteralIdentifierBind.default, defaultExpr))
      case _ => None
    }
  }

  /**
   * Value wrapping an environment, used as evaluation result for statements.
   * @param env The value environment
   */
  case class EnvValue(env: ValueEnv) extends Value {
    override val typ: Type = EnvType(TypeEnv.fromValueEnv(env))

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = ListElement(env.toMap.toList.map(p =>
      MultiElement(TextElement(p._1), TextElement(" = "), p._2.toText)))

    override def valueTextShowType: Boolean = false

    override val isError: Boolean = env.toMap.exists(p => p._2.isError)
  }

  /**
   * Type wrapping an environment, used as type-checking result for statements.
   * @param env The type environment
   */
  case class EnvType(env: TypeEnv) extends Type {
    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = ListElement(env.toMap.toList.map(p =>
      MultiElement(TextElement(p._1), TextElement(" : "), p._2.toText)))

    def matches(other: EnvType): Boolean = env.toMap == other.env.toMap
  }

  case class NotAStmtError(e: Expr) extends EvalError {
    override val typ: Type = NotAStmtTypeError(e)

    override val message: String = "Expected a statement, got " + e
  }

  case class NotAStmtTypeError(e: Expr) extends TypeError {
    override val message: String = "Expected a statement, got " + e
  }

  case class NoStmtTypeError() extends TypeError {
    override val message: String = "Cannot type-check a statement"
  }

  setTasks()

  protected class LWhileParser extends LListParser {
    override def root: Parser[Expr] = stmt

    protected def stmt: Parser[Expr] = stmtSeqFactor

    protected def stmtSeqFactor: Parser[Expr] = stmtBase ~ ";" ~ stmtSeqFactor ^^ {
      case stmtL ~ _ ~ stmtR => SeqStmt(stmtL, stmtR)
    } | stmtBase

    protected def stmtBase: Parser[Expr] = skipStmt | whileStmt | ifStmt | assignStmt | super.expr

    protected def whileStmt: Parser[WhileStmt] = "while" ~> expr ~ ("do" ~> stmt) ^^ {
      case cond ~ stmt => WhileStmt(cond, stmt)
    }

    protected def assignStmt: Parser[AssignStmt] = ident ~ (":=" ~> expr) ^^ {
      case v ~ e => AssignStmt(LiteralIdentifierBind(v), e)
    }

    protected def skipStmt: Parser[SkipStmt] = "skip" ^^^ { SkipStmt() }

    protected def ifStmt: Parser[IfStmt] = "if" ~> expr ~ ("then" ~> stmt) ~ ("else" ~> stmt) ^^ {
      case cond ~ stmtT ~ stmtF => IfStmt(cond, stmtT, stmtF)
    }
  }

  override protected val exprParser: ExprParser = LWhileParser()
}

object LWhile extends LWhile
