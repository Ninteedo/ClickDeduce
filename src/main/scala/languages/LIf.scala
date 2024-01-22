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

    override def prettyPrint: String = b.toString
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

    override def prettyPrint: String = s"(${e1.prettyPrint} == ${e2.prettyPrint})"
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

    override def prettyPrint: String =
      s"(if ${cond.prettyPrint} then ${then_expr.prettyPrint} else ${else_expr.prettyPrint})"
  }

  // values
  case class BoolV(b: Boolean) extends Value {
    override val typ: Type = BoolType()

    override def prettyPrint: String = b.toString
  }

  case class TypeMismatchError(exprName: String, type1: Type, type2: Type) extends EvalError {
    override val message: String = s"$type1 not compatible with $type2 in $exprName"

    override val typ: Type = TypeMismatchType(type1, type2)
  }

  // types
  case class BoolType() extends Type {
    override def prettyPrint: String = "Bool"
  }

  case class TypeMismatchType(type1: Type, type2: Type) extends TypeError {
    override val message: String = s"$type1 not compatible with $type2"

    override def prettyPrint: String = s"TypeMismatch($type1, $type2)"
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
