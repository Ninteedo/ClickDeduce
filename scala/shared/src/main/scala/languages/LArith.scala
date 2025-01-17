package languages

import convertors.*
import convertors.text.*
import languages.env.*
import languages.previews.*
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
  }

  object Num extends ExprCompanion {
    override val aliases: List[String] = List("Number", "Integer")

    def apply(x: BigInt): Num = new Num(LiteralInt(x))

    def apply(x: Int): Num = new Num(LiteralInt(BigInt(x)))

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(l: LiteralInt) => Some(Num(l))
      case _ => Some(Num(LiteralInt(0)))
    }

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(TypeCheckRuleBuilder().setConclusion(MathElement("n"), IntType().toText))
      .addEvaluationRule(EvalRuleBuilder().setConclusion(TermCommons.v, TermCommons.v))
      .buildOption
  }

  private def formatArithOperator(op: ConvertableText, toggle: Boolean): ConvertableText =
    MultiElement(op, if toggle then SubscriptElement(Symbols.doubleStrokeN) else NullElement()).spacesAround

  private def formatPlus(e1: ConvertableText, e2: ConvertableText, arith: Boolean = false): ConvertableText =
    MultiElement(e1, formatArithOperator(MathElement.plus, arith), e2)

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

    override def toText: ConvertableText = formatPlus(e1.toTextBracketed, e2.toTextBracketed)
  }

  object Plus extends ExprCompanion {
    override val aliases: List[String] = List("Addition", "+")

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e1: Expr, e2: Expr) => Some(Plus(e1, e2))
      case _ => Some(Plus(defaultExpr, defaultExpr))
    }

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .setConclusion(formatPlus(TermCommons.e(1), TermCommons.e(2)), IntType().toText)
          .addAssumption(TypeCheckRulePart.eTo(1, IntType()))
          .addAssumption(TypeCheckRulePart.eTo(2, IntType()))
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(
            formatPlus(TermCommons.e(1), TermCommons.e(2)), formatPlus(TermCommons.v(1), TermCommons.v(2), arith = true)
          )
          .addAssumption(EvalRulePart.eToV(1))
          .addAssumption(EvalRulePart.eToV(2))
      )
      .buildOption
  }

  private def formatTimes(e1: ConvertableText, e2: ConvertableText, arith: Boolean = false): ConvertableText =
    MultiElement(e1, formatArithOperator(Symbols.times, arith), e2)

  /** A multiplication expression. Both subexpressions must evaluate to `NumV`.
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
      formatTimes(e1.toTextBracketed, e2.toTextBracketed)
  }

  object Times extends ExprCompanion {
    override val aliases: List[String] = List("Multiplication", "Multiply", "*")

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e1: Expr, e2: Expr) => Some(Times(e1, e2))
      case _ => Some(Times(defaultExpr, defaultExpr))
    }

    override lazy val rulePreview: Option[RulePreview] = RulePreviewBuilder()
      .addTypeCheckRule(
        TypeCheckRuleBuilder()
          .setConclusion(formatTimes(TermCommons.e(1), TermCommons.e(2)), IntType().toText)
          .addAssumption(TypeCheckRulePart.eTo(1, IntType()))
          .addAssumption(TypeCheckRulePart.eTo(2, IntType()))
      )
      .addEvaluationRule(
        EvalRuleBuilder()
          .setConclusion(
            formatTimes(TermCommons.e(1), TermCommons.e(2)),
            formatTimes(TermCommons.v(1), TermCommons.v(2), arith = true)
          )
          .addAssumption(EvalRulePart.eToV(1))
          .addAssumption(EvalRulePart.eToV(2))
      )
      .buildOption
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

    override def toText: ConvertableText = NumberElement(x)
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

  protected class LArithParser extends ExprParser {
    protected def num: Parser[Num] = wholeNumber ^^ (n => Num(LiteralInt(BigInt(n))))

    protected def chainl1(p: Parser[Expr], op: Parser[(Expr, Expr) => Expr]): Parser[Expr] = {
      def rest(acc: Expr): Parser[Expr] =
        ((op ~ p) ^^ { case f ~ x => f(acc, x) } flatMap rest) | success(acc)

      p flatMap rest
    }

    protected def primitive: Parser[Expr] = num | "(" ~> expr <~ ")"

    private def level6: Parser[Expr] = chainl1(primitive, level6Parse)

    private def level5: Parser[Expr] = chainl1(level6, level5Parse)

    private def level4: Parser[Expr] = chainl1(level5, level4Parse)

    protected def level3: Parser[Expr] = chainl1(level4, level3Parse)

    protected def level2: Parser[Expr] = chainl1(level3, level2Parse)

    private def level1: Parser[Expr] = chainl1(level2, level1Parse)

    protected def level6Parse: Parser[(Expr, Expr) => Expr] = "*" ^^^ {Times(_, _)}

    protected def level5Parse: Parser[(Expr, Expr) => Expr] = "+" ^^^ {Plus(_, _)}

    protected def level4Parse: Parser[(Expr, Expr) => Expr] = failure("No level 4 operators")

    protected def level3Parse: Parser[(Expr, Expr) => Expr] = failure("No level 3 operators")

    protected def level2Parse: Parser[(Expr, Expr) => Expr] = failure("No level 2 operators")

    protected def level1Parse: Parser[(Expr, Expr) => Expr] = failure("No level 1 operators")

    override def expr: Parser[Expr] = level1
  }

  override protected val exprParser: ExprParser = new LArithParser
}

object LArith extends LArith {}
