package languages

class LIf extends LArith {
  // expressions
  case class Bool(b: Literal) extends Expr {
    override def evalInner(env: Env): Value = b match {
      case LiteralBool(b) => BoolV(b)
      case _              => UnexpectedArgValue(s"Bool can only accept LiteralBool, not $b")
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = b match {
      case LiteralBool(_) => BoolType()
      case _              => UnexpectedArgType(s"Bool can only accept LiteralBool, not $b")
    }
  }

  object Bool {
    def apply(b: Boolean): Bool = new Bool(LiteralBool(b))
  }

  case class Eq(e1: Expr, e2: Expr) extends Expr {
    override def evalInner(env: Env): Value = {
      val v1 = e1.eval(env)
      val v2 = e2.eval(env)
      if (v1.typ == v2.typ) {
        BoolV(v1 == v2)
      } else {
        TypeMismatchError("Eq", v1.typ, v2.typ)
      }
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = {
      val t1 = e1.typeCheck(tEnv)
      val t2 = e2.typeCheck(tEnv)
      if (t1 == t2) {
        BoolType()
      } else {
        TypeMismatchType(t1, t2)
      }
    }
  }

  case class IfThenElse(cond: Expr, then_expr: Expr, else_expr: Expr) extends Expr {
    override def evalInner(env: Env): Value = cond.eval(env) match {
      case BoolV(true)    => then_expr.eval(env)
      case BoolV(false)   => else_expr.eval(env)
      case v if v.isError => v
      case v              => TypeMismatchError("IfThenElse", v.typ, BoolType())
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = cond.typeCheck(tEnv) match {
      case BoolType() => {
        val t1 = then_expr.typeCheck(tEnv)
        val t2 = else_expr.typeCheck(tEnv)
        if (t1 == t2) {
          t1
        } else {
          TypeMismatchType(t1, t2)
        }
      }
      case t => TypeMismatchType(t, BoolType())
    }

    override def getChildrenEval(env: Env): List[(Term, Env)] = cond.eval(env) match {
      case BoolV(true)  => List((cond, env), (then_expr, env))
      case BoolV(false) => List((cond, env), (else_expr, env))
      case _            => List((cond, env), (then_expr, env), (else_expr, env))
    }
  }

  // values
  case class BoolV(b: Boolean) extends Value {
    override val typ: Type = BoolType()
  }

  case class TypeMismatchError(exprName: String, type1: Type, type2: Type) extends EvalError {
    override val message: String = s"$type1 not compatible with $type2 in $exprName"

    override val typ: Type = TypeMismatchType(type1, type2)
  }

  // types
  case class BoolType() extends Type

  case class TypeMismatchType(type1: Type, type2: Type) extends TypeError {
    override val message: String = s"$type1 not compatible with $type2"
  }

  override def prettyPrint(e: Expr): String = e match {
    case Bool(b)    => b.toString
    case Eq(e1, e2) => s"(${prettyPrint(e1)} == ${prettyPrint(e2)})"
    case IfThenElse(cond, then_expr, else_expr) =>
      s"(if ${prettyPrint(cond)} then ${prettyPrint(then_expr)} else ${prettyPrint(else_expr)})"
    case _ => super.prettyPrint(e)
  }

  override def prettyPrint(v: Value): String = v match {
    case BoolV(b) => b.toString
    case _        => super.prettyPrint(v)
  }

  override def prettyPrint(t: Type): String = t match {
    case BoolType()               => "Bool"
    case TypeMismatchType(t1, t2) => s"TypeMismatch($t1, $t2)"
    case _                        => super.prettyPrint(t)
  }

  override def calculateExprClassList: List[Class[Expr]] = {
    super.calculateExprClassList ++ List(classOf[Bool], classOf[Eq], classOf[IfThenElse]).map(
      _.asInstanceOf[Class[Expr]]
    )
  }

  override def calculateTypeClassList: List[Class[Type]] = {
    super.calculateTypeClassList ++ List(classOf[BoolType]).map(_.asInstanceOf[Class[Type]])
  }
}

object LIf extends LIf {}
