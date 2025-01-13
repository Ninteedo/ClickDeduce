package languages

import convertors.*
import languages.env.*
import languages.terms.*
import languages.terms.blanks.BlankExprDropDown
import languages.terms.builders.*
import languages.terms.errors.*
import languages.terms.exprs.Expr
import languages.terms.literals.*
import languages.terms.types.Type
import languages.terms.values.Value

import scala.annotation.targetName

class LArith extends ClickDeduceLanguage {
  registerTerms("LArith", List(Num, Plus, Times, IntType, NumV))

  // expressions

  /** A numeric expression. Can be any integer.
    *
    * @param x
    *   The integer value of the number.
    */
  case class Num(x: LiteralInt) extends Expr {
    override val needsBrackets: Boolean = false

    override def evalInner(env: ValueEnv): Value = NumV(x.value)

    override def typeCheckInner(tEnv: TypeEnv): Type = IntType()

    override def toText: ConvertableText = x.toText

    override def getRulePreview: Option[RulePreview] = Some(
      RulePreview(
        TypeCheckRulePreview(TypeCheckRulePart(MathElement("n"), IntType().toText)),
        EvalRulePreview(EvalRulePart(MathElement("v"), MathElement("v")))
      )
    )
  }

  object Num extends ExprCompanion {
    override val aliases: List[String] = List("Number", "Integer")

    def apply(x: BigInt): Num = new Num(LiteralInt(x))

    def apply(x: Int): Num = new Num(LiteralInt(BigInt(x)))

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(l: LiteralInt) => Some(Num(l))
      case _ => Some(Num(LiteralInt(0)))
    }
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

    override def getRulePreview: Option[RulePreview] = Some(
      RulePreview(
        TypeCheckRulePreview(
          TypeCheckRulePart(
            MultiElement(TermCommons.e(1), MathElement.plus.spacesAround, TermCommons.e(2)),
            IntType().toText
          ),
          TypeCheckRulePart.eTo(1, IntType()), TypeCheckRulePart.eTo(2, IntType())
        ),
        EvalRulePreview(
          EvalRulePart(
            MultiElement(TermCommons.e(1), MathElement.plus.spacesAround, TermCommons.e(2)),
            MultiElement(
              TermCommons.v(1), SubscriptElement.labelled(MathElement.plus, Symbols.doubleStrokeN).spacesAround,
              TermCommons.v(2)
            )
          ),
          EvalRulePart.eToV(1), EvalRulePart.eToV(2)
        )
      )
    )
  }

  object Plus extends ExprCompanion {
    override val aliases: List[String] = List("Addition", "+")

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e1: Expr, e2: Expr) => Some(Plus(e1, e2))
      case _ => Some(Plus(defaultExpr, defaultExpr))
    }
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
      MultiElement(e1.toTextBracketed, SurroundSpaces(Symbols.times), e2.toTextBracketed)

    override def getRulePreview: Option[RulePreview] = Some(
      RulePreview(
        TypeCheckRulePreview(
          TypeCheckRulePart(
            MultiElement(TermCommons.e(1), Symbols.times.spacesAround, TermCommons.e(2)),
            IntType().toText
          ),
          TypeCheckRulePart.eTo(1, IntType()), TypeCheckRulePart.eTo(2, IntType())
        ),
        EvalRulePreview(
          EvalRulePart(
            MultiElement(TermCommons.e(1), Symbols.times.spacesAround, TermCommons.e(2)),
            MultiElement(
              TermCommons.v(1), SubscriptElement.labelled(Symbols.times, Symbols.doubleStrokeN).spacesAround,
              TermCommons.v(2)
            )
          ),
          EvalRulePart.eToV(1), EvalRulePart.eToV(2)
        )
      )
    )

  }

  object Times extends ExprCompanion {
    override val aliases: List[String] = List("Multiplication", "Multiply", "*")

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e1: Expr, e2: Expr) => Some(Times(e1, e2))
      case _ => Some(Times(defaultExpr, defaultExpr))
    }
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

  object NumV extends ValueCompanion

  // types

  trait OrdinalType extends Type

  /** Type for integers.
   */
  case class IntType() extends OrdinalType {
    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = TextElement("Int")
  }

  object IntType extends TypeCompanion {
    override val aliases: List[String] = List("Number", "Integer")

    override def create(args: BuilderArgs): Option[Type] = Some(IntType())
  }

  /** An error that occurs due to an incorrect argument type.
   *
   * @param message
   * The error message.
   */
  case class UnexpectedArgValue(override val message: String) extends EvalError {
    override val typ: Type = UnexpectedArgType(message)
  }

  /** An error that occurs due to an incorrect argument type.
   *
   * @param message
   * The error message.
   */
  case class UnexpectedArgType(override val message: String) extends TypeError

  // tasks
  setTasks(SelectAnyExprTask, EnterANumberTask, BasicArithmeticTask)

  private object SelectAnyExprTask extends Task {
    override val name: String = "Select an expression"
    override val description: String = "The tree currently only contains the root node. " +
      "An expression can be selected by clicking on the dropdown and clicking an option, " +
      "or by typing the expression's name into the text box and pressing Enter."
    override val difficulty: Int = 1

    override def checkFulfilled(expr: Expr): Boolean = !expr.isInstanceOf[BlankExprDropDown]
  }

  private object EnterANumberTask extends Task {
    override val name: String = "Enter a positive integer"
    override val description: String = "Select a Num expression and enter a positive integer into its text box."
    override val difficulty: Int = 1

    override def checkFulfilled(expr: Expr): Boolean = {
      def checkNum(expr: Expr): Boolean = checkCondition(
        expr,
        cond = {
          case Num(LiteralInt(n)) => n > 0
          case _                  => false
        }
      )

      checkNum(expr)
    }
  }

  private object BasicArithmeticTask extends Task {
    override val name: String = "Plus and Times to 42"
    override val description: String = "Create an expression that results in 42, involving both addition and " +
      "multiplication, but no zeroes. You can right-click on a tree element to open the option to delete it."
    override val difficulty: Int = 2

    override def checkFulfilled(expr: Expr): Boolean = {
      def checkNoZeroes(expr: Expr): Boolean = !checkCondition(
        expr,
        cond = {
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
