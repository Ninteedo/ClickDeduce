package languages

import convertors.*

class LArith extends ClickDeduceLanguage {
  // expressions

  /** A numeric expression. Can be any integer.
    *
    * @param x
    *   The integer value of the number.
    */
  case class Num(x: Literal) extends Expr {
    override def evalInner(env: ValueEnv): Value = x match {
      case LiteralInt(x) => NumV(x)
      case _             => UnexpectedArgValue(s"Num can only accept LiteralInt, not $x")
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = x match {
      case LiteralInt(_) => IntType()
      case _             => UnexpectedArgType(s"Num can only accept LiteralInt, not $x")
    }

    override def prettyPrint: String = x.toString

    override def toText: ConvertableText = MathElement(x.toString)

    override val needsBrackets: Boolean = false
  }

  object Num {
    def apply(x: BigInt): Num = new Num(LiteralInt(x))

    def apply(x: Int): Num = new Num(LiteralInt(BigInt(x)))
  }

  addExprBuilder(
    "Num",
    {
      case List(l: Literal) => Some(Num(l))
      case Nil              => Some(Num(defaultLiteral))
      case _                => None
    }
  )

  /** A plus expression. Both subexpressions must evaluate to `NumV`.
    *
    * @param e1
    *   The first expression to add.
    * @param e2
    *   The second expression to add.
    */
  case class Plus(e1: Expr, e2: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = (e1.eval(env), e2.eval(env)) match {
      case (NumV(x), NumV(y))    => NumV(x + y)
      case (v1, _) if v1.isError => v1
      case (_, v2) if v2.isError => v2
      case (v1, v2)              => UnexpectedArgValue(s"Plus cannot accept ($v1, $v2)")
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = (e1.typeCheck(tEnv), e2.typeCheck(tEnv)) match {
      case (IntType(), IntType()) => IntType()
      case (t1, _) if t1.isError  => t1
      case (_, t2) if t2.isError  => t2
      case (t1, t2)               => UnexpectedArgType(s"Plus cannot accept ($t1, $t2)")
    }

    override def prettyPrint: String = s"${e1.prettyPrintBracketed} + ${e2.prettyPrintBracketed}"

    override def toText: ConvertableText =
      MultiElement(e1.toTextBracketed, SurroundSpaces(MathElement.plus), e2.toTextBracketed)
  }

  addExprBuilder(
    "Plus",
    {
      case List(e1: Expr, e2: Expr) => Some(Plus(e1, e2))
      case Nil                      => Some(Plus(defaultExpr, defaultExpr))
      case _                        => None
    }
  )

  /** A times expression. Both subexpressions must evaluate to `NumV`.
    *
    * @param e1
    *   The first expression to multiply.
    * @param e2
    *   The second expression to multiply.
    */
  case class Times(e1: Expr, e2: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = (e1.eval(env), e2.eval(env)) match {
      case (NumV(x), NumV(y))    => NumV(x * y)
      case (v1, _) if v1.isError => v1
      case (_, v2) if v2.isError => v2
      case (v1, v2)              => UnexpectedArgValue(s"Times cannot accept ($v1, $v2)")
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = (e1.typeCheck(tEnv), e2.typeCheck(tEnv)) match {
      case (IntType(), IntType()) => IntType()
      case (t1, _) if t1.isError  => t1
      case (_, t2) if t2.isError  => t2
      case (t1, t2)               => UnexpectedArgType(s"Times cannot accept ($t1, $t2)")
    }

    override def prettyPrint: String = s"${e1.prettyPrintBracketed} × ${e2.prettyPrintBracketed}"

    override def toText: ConvertableText =
      MultiElement(e1.toTextBracketed, SurroundSpaces(TimesSymbol()), e2.toTextBracketed)
  }

  addExprBuilder(
    "Times",
    {
      case List(e1: Expr, e2: Expr) => Some(Times(e1, e2))
      case Nil                      => Some(Times(defaultExpr, defaultExpr))
      case _                        => None
    }
  )

  // values

  trait OrdinalValue extends Value {
    def compare(that: OrdinalValue): Int
  }

  /** A numeric value. Can be any integer.
    *
    * @param x
    *   The integer value of the number.
    */
  case class NumV(x: BigInt) extends Value, OrdinalValue {
    override val typ: Type = IntType()

    override def prettyPrint: String = x.toString

    override val needsBrackets: Boolean = false

    override def compare(that: OrdinalValue): Int = that match {
      case NumV(y) => x.compare(y)
      case other   => throw new IllegalArgumentException(s"Cannot compare NumV with non-NumV ($other)")
    }

    override def toText: ConvertableText = MathElement(x.toString)
  }

  addValueBuilder(
    "NumV",
    {
      case List(x: BigInt) => Some(NumV(x))
      case _               => None
    }
  )

  /** An error that occurs due to an incorrect argument type.
    *
    * @param message
    *   The error message.
    */
  case class UnexpectedArgValue(override val message: String) extends EvalError {
    override val typ: Type = UnexpectedArgType(message)
  }

  // types

  trait OrdinalType extends Type

  /** Type for integers.
    */
  case class IntType() extends Type, OrdinalType {
    override def prettyPrint: String = "Int"

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = TextElement("Int")
  }

  addTypeBuilder(
    "IntType",
    {
      case Nil => Some(IntType())
      case _   => None
    }
  )

  /** An error that occurs due to an incorrect argument type.
    *
    * @param message
    *   The error message.
    */
  case class UnexpectedArgType(override val message: String) extends TypeError
}

object LArith extends LArith {}
