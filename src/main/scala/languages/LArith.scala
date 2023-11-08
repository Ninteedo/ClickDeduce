package languages

class LArith extends ClickDeduceLanguage {
  // expressions

  /**
   * A numeric expression.
   * Can be any integer.
   *
   * @param x The integer value of the number.
   */
  case class Num(x: Literal) extends Expr {
//    def this(x: BigInt) = this(LiteralInt(x))
//
//    def this(x: Int) = this(BigInt(x))
  }

  object Num {
    def apply(x: BigInt): Num = new Num(LiteralInt(x))
    def apply(x: Int): Num = new Num(LiteralInt(BigInt(x)))
  }

  /**
   * A plus expression.
   * Both subexpressions must evaluate to `NumV`.
   *
   * @param e1 The first expression to add.
   * @param e2 The second expression to add.
   */
  case class Plus(e1: Expr, e2: Expr) extends Expr

  /**
   * A times expression.
   * Both subexpressions must evaluate to `NumV`.
   *
   * @param e1 The first expression to multiply.
   * @param e2 The second expression to multiply.
   */
  case class Times(e1: Expr, e2: Expr) extends Expr

  // values

  /**
   * A numeric value.
   * Can be any integer.
   *
   * @param x The integer value of the number.
   */
  case class NumV(x: BigInt) extends Value {
    override val typ: Type = IntType()
  }

  /**
   * An error that occurs due to an incorrect argument type.
   *
   * @param message The error message.
   */
  case class UnexpectedArgValue(override val message: String) extends EvalError {
    override val typ: Type = UnexpectedArgType(message)
  }

  /**
   * An error that occurs due to attempting to process an unknown `Expr`.
   *
   * @param message The error message.
   */
  case class UnexpectedExpr(override val message: String) extends EvalError {
    override val typ: Type = UnexpectedExprType(message)
  }

  // types

  /**
   * Type for integers.
   */
  case class IntType() extends Type

  /**
   * An error that occurs due to an incorrect argument type.
   *
   * @param message The error message.
   */
  case class UnexpectedArgType(message: String) extends TypeError

  /**
   * An error that occurs due to attempting to process an unknown `Expr`.
   *
   * @param message The error message.
   */
  case class UnexpectedExprType(message: String) extends TypeError


  override def eval(e: Expr, env: Env): Value = e match {
    case Num(LiteralInt(x)) => NumV(x)
    case Plus(e1, e2) => (eval(e1, env), eval(e2, env)) match {
      case (NumV(x), NumV(y)) => NumV(x + y)
      case (v1, v2) => UnexpectedArgValue(s"Plus can only accept (NumV, NumV), not ($v1, $v2)")
    }
    case Times(e1, e2) => (eval(e1, env), eval(e2, env)) match {
      case (NumV(x), NumV(y)) => NumV(x * y)
      case (v1, v2) => UnexpectedArgValue(s"Times can only accept (NumV, NumV), not ($v1, $v2)")
    }
    case _ => UnexpectedExpr(s"Unexpected expression: $e")
  }

  override def typeOf(e: Expr, tenv: TypeEnv): Type = e match {
    case Num(x) => IntType()
    case Plus(e1, e2) => (typeOf(e1, tenv), typeOf(e2, tenv)) match {
      case (IntType(), IntType()) => IntType()
      case (t1, t2) => UnexpectedArgType(s"Plus can only accept (IntType, IntType), not ($t1, $t2)")
    }
    case Times(e1, e2) => (typeOf(e1, tenv), typeOf(e2, tenv)) match {
      case (IntType(), IntType()) => IntType()
      case (t1, t2) => UnexpectedArgType(s"Times can only accept (IntType, IntType), not ($t1, $t2)")
    }
    case _ => UnexpectedExprType(s"Unexpected expression: $e")
  }

  override def prettyPrint(e: Expr): String = e match {
    case Num(x) => x.toString
    case Plus(e1, e2) => s"(${prettyPrint(e1)} + ${prettyPrint(e2)})"
    case Times(e1, e2) => s"(${prettyPrint(e1)} × ${prettyPrint(e2)})"
    case e => super.prettyPrint(e)
  }

  override def prettyPrint(v: Value): String = v match {
    case NumV(x) => x.toString
    case v => super.prettyPrint(v)
  }

  override def prettyPrint(t: Type): String = t match {
    case IntType() => "Int"
    case t => super.prettyPrint(t)
  }

  override def calculateExprClassList: List[Class[Expr]] = List(classOf[Num], classOf[Plus], classOf[Times]).map(_.asInstanceOf[Class[Expr]])
}

object LArith extends LArith {

}
