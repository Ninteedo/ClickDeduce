package languages

class LIf extends LArith {
  // expressions
  case class Bool(b: Literal) extends Expr

  object Bool {
    def apply(b: Boolean): Bool = new Bool(LiteralBool(b))
  }

  case class Eq(e1: Expr, e2: Expr) extends Expr

  case class IfThenElse(cond: Expr, then_expr: Expr, else_expr: Expr) extends Expr

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

  }

}

object LIf extends LIf {}
