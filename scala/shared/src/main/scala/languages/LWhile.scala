package languages

import convertors.*
import convertors.text.*
import languages.env.*
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
    EnvValue,
  ))

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
  }

  case class SkipStmt() extends Stmt {
    override def newEnv(env: ValueEnv): ValueEnv = env

    override def newTEnv(tEnv: TypeEnv): TypeEnv | TypeError = tEnv

    override def toText: ConvertableText = TextElement("skip")
  }

  object SkipStmt extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = Some(SkipStmt())
  }

  case class SeqStmt(stmtA: Expr, stmtB: Expr) extends Stmt {
    override def newEnv(env: ValueEnv): ValueEnv | EvalError = (stmtA, stmtB) match {
      case (stmtA: Stmt, stmtB: Stmt) =>
        stmtA.newEnv(env) match {
          case env: ValueEnv => stmtB.newEnv(env)
          case error: EvalError => error
        }
      case (stmtA: Stmt, e) => NotAStmtError(e)
      case (e, _) => NotAStmtError(e)
    }

    override def newTEnv(tEnv: TypeEnv): TypeEnv | TypeError = (stmtA, stmtB) match {
      case (stmtA: Stmt, stmtB: Stmt) =>
        stmtA.newTEnv(tEnv) match {
          case tEnv: TypeEnv => stmtB.newTEnv(tEnv)
          case error: TypeError => error
        }
      case (stmtA: Stmt, e) => NotAStmtTypeError(e)
      case (e, _) => NotAStmtTypeError(e)
    }

    private def defaultChildren(env: ValueEnv): List[(Term, ValueEnv)] = {
      List((stmtA, env), stmtA match {
        case stmtA: Stmt => (stmtB, stmtA.newEnv(env) match {
          case env: ValueEnv => env
          case _ => env
        })
        case _ => (stmtB, env)
      })
    }

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] = defaultChildren(env)

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = {
      List((stmtA, tEnv), stmtA match {
        case stmtA: Stmt => (stmtB, stmtA.newTEnv(tEnv) match {
          case tEnv: TypeEnv => tEnv
          case _ => tEnv
        })
        case _ => (stmtB, tEnv)
      })
    }

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] = defaultChildren(env)

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
      case BoolV(true) => stmtT match {
        case stmtT: Stmt => stmtT.newEnv(env)
        case e => NotAStmtError(e)
      }
      case BoolV(false) => stmtF match {
        case stmtF: Stmt => stmtF.newEnv(env)
        case e => NotAStmtError(e)
      }
      case v => TypeMismatchError("IfStmt", v.typ, BoolType())
    }

    override def newTEnv(tEnv: TypeEnv): TypeEnv | TypeError = cond.typeCheck(tEnv) match {
      case BoolType() => (stmtT, stmtF) match {
        case (stmtT: Stmt, stmtF: Stmt) =>
          (stmtT.newTEnv(tEnv), stmtF.newTEnv(tEnv)) match {
            case (tEnvT: TypeEnv, tEnvF: TypeEnv) =>
              EnvType(tEnvT).matches(EnvType(tEnvF)) match {
                case true => tEnvT
                case false => TypeMismatchType(EnvType(tEnvT), EnvType(tEnvF))
              }
            case (error: TypeError, _) => error
            case (_, error: TypeError) => error
          }
        case (stmtT: Stmt, e) => NotAStmtTypeError(e)
        case (e, _) => NotAStmtTypeError(e)
      }
    }

    override def toText: ConvertableText = MultiElement(
      TextElement("if "),
      cond.toText,
      TextElement(" then "),
      stmtT.toText,
      TextElement(" else "),
      stmtF.toText
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
      case BoolV(true) => stmt match {
        case stmt: Stmt => stmt.newEnv(env) match {
          case env: ValueEnv => WhileStmt(cond, stmt).newEnv(env)
          case error: EvalError => error
        }
        case e => NotAStmtError(e)
      }
      case BoolV(false) => env
      case v => TypeMismatchError("WhileStmt", v.typ, BoolType())
    }

    override def newTEnv(tEnv: TypeEnv): TypeEnv | TypeError = cond.typeCheck(tEnv) match {
      case BoolType() => stmt match {
        case stmt: Stmt => stmt.newTEnv(tEnv)
        case _ => NoStmtTypeError()
      }
      case typ => TypeMismatchType(typ, BoolType())
    }

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] = {
      val default = super.getChildrenEval(env)
      cond.eval(env) match {
        case BoolV(true) => stmt match {
          case stmt: Stmt => stmt.newEnv(env) match {
            case childEnv: ValueEnv => List((cond, env), (stmt, env), (this, childEnv))
            case _ => default
          }
          case _ => default
        }
        case BoolV(false) => List((cond, env))
        case _ => default
      }
    }

    override def toText: ConvertableText = MultiElement(
      TextElement("while "),
      cond.toText,
      TextElement(" do "),
      stmt.toText
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

    override def toText: ConvertableText = MultiElement(
      v.toText,
      TextElement(" := "),
      e.toText
    )
  }

  object AssignStmt extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(v: LiteralIdentifierBind, e: Expr) => Some(AssignStmt(v, e))
      case Nil => Some(AssignStmt(LiteralIdentifierBind.default, defaultExpr))
      case _ => None
    }
  }

  case class EnvValue(env: ValueEnv) extends Value {
    override val typ: Type = EnvType(TypeEnv.fromValueEnv(env))

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = ListElement(env.toMap.toList.map(p =>
      MultiElement(TextElement(p._1), TextElement(" = "), p._2.toText)))

    override def valueTextShowType: Boolean = false
  }

  object EnvValue extends ValueCompanion

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
}

object LWhile extends LWhile
