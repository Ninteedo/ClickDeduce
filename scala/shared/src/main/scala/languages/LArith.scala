package languages

import convertors.*

import scala.annotation.targetName

class LArith extends ClickDeduceLanguage {
  Num.register()
  Plus.register()
  Times.register()
  IntType.register()
  NumV.register()

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

    override def toText: ConvertableText = MathElement(x.toString)

    override val needsBrackets: Boolean = false
  }

  object Num extends ExprCompanion {
    def apply(x: BigInt): Num = new Num(LiteralInt(x))

    def apply(x: Int): Num = new Num(LiteralInt(BigInt(x)))

    override def createExpr(args: List[Any]): Option[Expr] = args match {
      case List(l: Literal) => Some(Num(l))
      case defaultArgs      => Some(Num(defaultLiteral))
      case _                => None
    }

    override val aliases: List[String] = List("Number", "Integer")
  }

  /** A plus expression. Both subexpressions must evaluate to `NumV`.
    *
    * @param e1
    *   The first expression to add.
    * @param e2
    *   The second expression to add.
    */
  case class Plus(e1: Expr, e2: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = (e1.eval(env), e2.eval(env)) match {
      case (x: NumericValue, y: NumericValue) => x + y
      case (v1, _) if v1.isError              => v1
      case (_, v2) if v2.isError              => v2
      case (v1, v2)                           => UnexpectedArgValue(s"Plus cannot accept ($v1, $v2)")
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = (e1.typeCheck(tEnv), e2.typeCheck(tEnv)) match {
      case (IntType(), IntType()) => IntType()
      case (t1, _) if t1.isError  => t1
      case (_, t2) if t2.isError  => t2
      case (t1, t2)               => UnexpectedArgType(s"Plus cannot accept ($t1, $t2)")
    }

    override def toText: ConvertableText =
      MultiElement(e1.toTextBracketed, SurroundSpaces(MathElement.plus), e2.toTextBracketed)
  }

  object Plus extends ExprCompanion {
    override def createExpr(args: List[Any]): Option[Expr] = args match {
      case List(e1: Expr, e2: Expr) => Some(Plus(e1, e2))
      case defaultArgs              => Some(Plus(defaultExpr, defaultExpr))
      case _                        => None
    }

    override val aliases: List[String] = List("Addition", "+")
  }

  /** A times expression. Both subexpressions must evaluate to `NumV`.
    *
    * @param e1
    *   The first expression to multiply.
    * @param e2
    *   The second expression to multiply.
    */
  case class Times(e1: Expr, e2: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = (e1.eval(env), e2.eval(env)) match {
      case (x: NumericValue, y: NumericValue) => x * y
      case (v1, _) if v1.isError              => v1
      case (_, v2) if v2.isError              => v2
      case (v1, v2)                           => UnexpectedArgValue(s"Times cannot accept ($v1, $v2)")
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = (e1.typeCheck(tEnv), e2.typeCheck(tEnv)) match {
      case (IntType(), IntType()) => IntType()
      case (t1, _) if t1.isError  => t1
      case (_, t2) if t2.isError  => t2
      case (t1, t2)               => UnexpectedArgType(s"Times cannot accept ($t1, $t2)")
    }

    override def toText: ConvertableText =
      MultiElement(e1.toTextBracketed, SurroundSpaces(TimesSymbol()), e2.toTextBracketed)
  }

  object Times extends ExprCompanion {
    override def createExpr(args: List[Any]): Option[Expr] = args match {
      case List(e1: Expr, e2: Expr) => Some(Times(e1, e2))
      case defaultArgs              => Some(Times(defaultExpr, defaultExpr))
      case _                        => None
    }

    override val aliases: List[String] = List("Multiplication", "Multiply", "*")
  }

  // values

  trait OrdinalValue extends Value {
    def compare(that: OrdinalValue): Int
  }

  trait NumericValue extends OrdinalValue {
    @targetName("plus")
    def +(that: NumericValue): NumericValue
    @targetName("times")
    def *(that: NumericValue): NumericValue
  }

  /** A numeric value. Can be any integer.
    *
    * @param x
    *   The integer value of the number.
    */
  case class NumV(x: BigInt) extends NumericValue {
    override val typ: Type = IntType()

    override val needsBrackets: Boolean = false

    override def compare(that: OrdinalValue): Int = that match {
      case NumV(y) => x.compare(y)
      case other   => throw new IllegalArgumentException(s"Cannot compare NumV with non-NumV ($other)")
    }

    @targetName("plus")
    override def +(that: NumericValue): NumericValue = that match {
      case NumV(y) => NumV(x + y)
      case other   => throw new IllegalArgumentException(s"Cannot add NumV to non-NumV ($other)")
    }

    @targetName("times")
    override def *(that: NumericValue): NumericValue = that match {
      case NumV(y) => NumV(x * y)
      case other   => throw new IllegalArgumentException(s"Cannot multiply NumV by non-NumV ($other)")
    }

    override def toText: ConvertableText = MathElement(x.toString)
  }

  object NumV extends ValueCompanion {
    override def createValue(args: List[Any]): Option[Value] = args match {
      case List(x: BigInt) => Some(NumV(x))
      case _               => None
    }
  }

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
  case class IntType() extends OrdinalType {
    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = TextElement("Int")
  }

  object IntType extends TypeCompanion {
    override def createType(args: List[Any]): Option[Type] = args match {
      case defaultArgs => Some(IntType())
      case _           => None
    }

    override val aliases: List[Variable] = List("Number", "Integer")
  }

  /** An error that occurs due to an incorrect argument type.
    *
    * @param message
    *   The error message.
    */
  case class UnexpectedArgType(override val message: String) extends TypeError

  // tasks
  SelectAnyExprTask.register()
  EnterANumberTask.register()
  BasicArithmeticTask.register()

  private object SelectAnyExprTask extends Task {
    override val name: String = "Select an expression"
    override val description: String = "The tree currently only contains the root node. " +
      "An expression can be selected by clicking on the dropdown and clicking an option, " +
      "or by typing the expression's name into the text box and pressing Enter."
    override val difficulty: Int = 1

    override def checkFulfilled(expr: Expr): Boolean = expr match {
      case BlankExprDropDown() => false
      case _                   => true
    }
  }

  private object EnterANumberTask extends Task {
    override val name: String = "Enter a number"
    override val description: String = "Select a Num expression and enter a number into its text box."
    override val difficulty: Int = 1

    override def checkFulfilled(expr: Expr): Boolean = {
      def checkNum(expr: Expr): Boolean = checkCondition(
        expr,
        {
          case Num(LiteralInt(_)) => true
          case _                  => false
        }
      )

      checkNum(expr)
    }
  }

  private object BasicArithmeticTask extends Task {
    override val name: String = "Basic arithmetic"
    override val description: String = "Create an expression that results in 42, involving both addition and " +
      "multiplication, but no zeroes."
    override val difficulty: Int = 2

    override def checkFulfilled(expr: Expr): Boolean = {
      def checkNoZeroes(expr: Expr): Boolean = !checkCondition(
        expr,
        {
          case Num(LiteralInt(0)) => true
          case _                  => false
        }
      )

      expr.eval() match {
        case NumV(42) => checkNoZeroes(expr) && checkHasOp(expr, classOf[Plus]) && checkHasOp(expr, classOf[Times])
        case _        => false
      }
    }
  }
}

object LArith extends LArith {}
