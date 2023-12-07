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
    override def eval(env: Env): Value = x match {
      case LiteralInt(x) => NumV(x)
      case _ => UnexpectedArgValue(s"Num can only accept LiteralInt, not $x")
    }

    override def typeCheck(tEnv: TypeEnv): Type = x match {
      case LiteralInt(_) => IntType()
      case _ => UnexpectedArgType(s"Num can only accept LiteralInt, not $x")
    }
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
  case class Plus(e1: Expr, e2: Expr) extends Expr {
    override def eval(env: Env): Value = (e1.eval(env), e2.eval(env)) match {
      case (NumV(x), NumV(y)) => NumV(x + y)
      case (v1, v2) => UnexpectedArgValue(s"Plus can only accept (NumV, NumV), not ($v1, $v2)")
    }

    override def typeCheck(tEnv: TypeEnv): Type = (e1.typeCheck(tEnv), e2.typeCheck(tEnv)) match {
      case (IntType(), IntType()) => IntType()
      case (t1, t2) => UnexpectedArgType(s"Plus can only accept (IntType, IntType), not ($t1, $t2)")
    }
  }

  /**
   * A times expression.
   * Both subexpressions must evaluate to `NumV`.
   *
   * @param e1 The first expression to multiply.
   * @param e2 The second expression to multiply.
   */
  case class Times(e1: Expr, e2: Expr) extends Expr {
    override def eval(env: Env): Value = (e1.eval(env), e2.eval(env)) match {
      case (NumV(x), NumV(y)) => NumV(x * y)
      case (v1, v2) => UnexpectedArgValue(s"Times can only accept (NumV, NumV), not ($v1, $v2)")
    }

    override def typeCheck(tEnv: TypeEnv): Type = (e1.typeCheck(tEnv), e2.typeCheck(tEnv)) match {
      case (IntType(), IntType()) => IntType()
      case (t1, t2) => UnexpectedArgType(s"Times can only accept (IntType, IntType), not ($t1, $t2)")
    }
  }

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
  case class UnexpectedArgType(override val message: String) extends TypeError

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

  override def calculateExprClassList: List[Class[Expr]] = List(classOf[Num], classOf[Plus], classOf[Times])
    .map(_.asInstanceOf[Class[Expr]])

  override def calculateTypeClassList: List[Class[Type]] = super.calculateTypeClassList ++ List(classOf[IntType])
    .map(_.asInstanceOf[Class[Type]])
}

object LArith extends LArith {

}
