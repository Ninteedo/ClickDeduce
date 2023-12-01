package languages

class LIf extends LArith {
  // expressions
  case class Bool(b: Literal) extends Expr

  object Bool {
    def apply(b: Boolean): Bool = new Bool(LiteralBool(b))
  }

  case class Eq(e1: Expr, e2: Expr) extends Expr

  case class IfThenElse(cond: Expr, then_expr: Expr, else_expr: Expr) extends Expr {
    override def getChildrenEval(env: Env): List[(Expr, Env)] = eval(cond, env) match {
      case BoolV(true) => List((cond, env), (then_expr, env))
      case BoolV(false) => List((cond, env), (else_expr, env))
      case _ => List((cond, env))
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

  override def eval(e: Expr, env: Env): Value = e match {
    case Bool(LiteralBool(b)) => BoolV(b)
    case Eq(e1, e2) => {
      val v1 = eval(e1, env)
      val v2 = eval(e2, env)
      if (v1.typ == v2.typ) {
        BoolV(v1 == v2)
      } else {
        TypeMismatchError("Eq", v1.typ, v2.typ)
      }
    }
    case IfThenElse(cond, then_expr, else_expr) => eval(cond, env) match {
      case BoolV(true) => eval(then_expr, env)
      case BoolV(false) => eval(else_expr, env)
      case v => if (v.isError) {v} else {TypeMismatchError("IfThenElse", v.typ, BoolType())}
    }
    case _ => super.eval(e, env)
  }

  override def typeOf(e: Expr, tenv: TypeEnv): Type = e match {
    case Bool(LiteralBool(b)) => BoolType()
    case Eq(e1, e2) => {
      val t1 = typeOf(e1, tenv)
      val t2 = typeOf(e2, tenv)
      if (t1 == t2) {
        BoolType()
      } else {
        TypeMismatchType(t1, t2)
      }
    }
    case IfThenElse(cond, then_expr, else_expr) => typeOf(cond, tenv) match {
      case BoolType() => {
        val t1 = typeOf(then_expr, tenv)
        val t2 = typeOf(else_expr, tenv)
        if (t1 == t2) {
          t1
        } else {
          TypeMismatchType(t1, t2)
        }
      }
      case t => TypeMismatchType(t, BoolType())
    }
    case _ => super.typeOf(e, tenv)
  }

  override def prettyPrint(e: Expr): String = e match {
    case Bool(b) => b.toString
    case Eq(e1, e2) => s"(${prettyPrint(e1)} == ${prettyPrint(e2)})"
    case IfThenElse(cond, then_expr, else_expr) => s"(if ${prettyPrint(cond)} then ${prettyPrint(then_expr)} else ${prettyPrint(else_expr)})"
    case _ => super.prettyPrint(e)
  }

  override def prettyPrint(v: Value): String = v match {
    case BoolV(b) => b.toString
    case _ => super.prettyPrint(v)
  }

  override def prettyPrint(t: Type): String = t match {
    case BoolType() => "Bool"
    case TypeMismatchType(t1, t2) => s"TypeMismatch($t1, $t2)"
    case _ => super.prettyPrint(t)
  }

  override def calculateExprClassList: List[Class[Expr]] = {
    super.calculateExprClassList ++ List(classOf[Bool], classOf[Eq], classOf[IfThenElse]).map(_.asInstanceOf[Class[Expr]])
  }

  override def calculateTypeClassList: List[Class[Type]] = {
    super.calculateTypeClassList ++ List(classOf[BoolType]).map(_.asInstanceOf[Class[Type]])
  }
}

object LIf extends LIf {}
