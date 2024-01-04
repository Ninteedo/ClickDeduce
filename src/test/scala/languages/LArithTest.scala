package languages

import languages.LArith.*
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1, TableFor3}
import org.scalatest.propspec.AnyPropSpec

import scala.util.Random

class LArithTest extends AnyPropSpec with TableDrivenPropertyChecks with GivenWhenThen {
  Random.setSeed(2023)

  def genRandInt(): BigInt = {
    Random.nextInt(200) - 100
  }

  val intValues: List[BigInt] = List(0, 1, -1, 2, -2, 5, -5, 10, 100, -100, 198765, -157396, 5168765, -4376418, 159871598156996L)
  val nums: TableFor1[BigInt] = Table("num", intValues: _*)

  property("Num type-checks to IntType") {
    forAll(nums)(n =>
      Num(n).typeCheck(Map()) shouldBe IntType()
    )
  }

  property("Num correctly evaluates to NumV") {
    forAll(nums)(n =>
      Num(n).eval(Map()) shouldBe NumV(n)
    )
  }

  /**
   * Run tests for the type-checking and evaluation of a table of expressions and their correct results and types.
   * @param table the table of expressions, results, and types
   * @param expressionName the name of the expression to display in the test name
   */
  def testExpression(table: TableFor3[Expr, Value, Type], expressionName: String): Unit = {
    property(s"$expressionName type-checks correctly") {
      forAll(table)((expr, _, typ) => {
        println(s"$expr -> $typ")
        expr.typeCheck(Map()) shouldBe typ
      })
    }

    property(s"$expressionName evaluates correctly") {
      forAll(table)((expr, res, _) => {
        println(s"$expr -> $res")
        expr.eval(Map()) shouldBe res
      })
    }
  }

  /**
   * Create a table of expressions, their correct evaluation results, and their correct types.
   */
  def createExprTable(
    expressions: Iterable[Expr],
    results: Iterable[Value],
    types: Iterable[Type]
  ): TableFor3[Expr, Value, Type] = {
    val zipped = List(expressions, results, types).transpose.map {
      case List(a: Expr, b: Value, c: Type) => (a, b, c)
      case _ => throw new Exception("Should not happen")
    }
    Table(("expressions", "results", "types"), zipped: _*)
  }

  /**
   * Run tests for binary arithmetic operations.
   * @param op the operation to test
   * @param opEval the correct evaluation result of the operation
   * @param opName the name of the operation
   */
  private def arithmeticOperationTests(op: (Num, Num) => Expr, opEval: (BigInt, BigInt) => BigInt, opName: String): Unit = {
    val table: TableFor3[Expr, Value, Type] = {
      val valuePairs = for {
        a <- intValues
        b <- intValues
      } yield (a, b)
      val expressions = valuePairs.map { case (n, m) => op(Num(n), Num(m)) }
      val results = valuePairs.map { case (n, m) => NumV(opEval(n, m)) }
      val types = expressions.map(_ => IntType())
      createExprTable(expressions, results, types)
    }

    testExpression(table, opName)
  }

  arithmeticOperationTests(Plus.apply, _ + _, "Plus")
  arithmeticOperationTests(Times.apply, _ * _, "Times")

  /**
   * Generate a random expression of the given depth.
   * @param depth the maximum remaining depth of the expression
   * @return a tuple of the expression and its correct evaluation result
   */
  private def generateExpression(depth: Int): (Expr, BigInt) = {
    if (depth == 0) {
      val n = genRandInt()
      (Num(n), n)
    } else {
      var (left_expr, left_total) = generateExpression(depth - 1)
      var (right_expr, right_total) = generateExpression(depth - 1)
      Random.nextInt(3) match {
        case 0 => (Plus(left_expr, right_expr), left_total + right_total)
        case 1 => (Times(left_expr, right_expr), left_total * right_total)
        case 2 => { // terminal expression
          val (other_expr, other_total) = generateExpression(0)
          if (Random.nextBoolean()) {
            left_expr = other_expr
            left_total = other_total
          } else {
            right_expr = other_expr
            right_total = other_total
          }
          if (Random.nextBoolean()) (Plus(left_expr, right_expr), left_total + right_total)
          else (Times(left_expr, right_expr), left_total * right_total)
        }
      }
    }
  }

  private def nestedArithmeticOperationsTests(depth: Int): Unit = {
    val generated = (0 until 10).map(_ => generateExpression(depth))
    val expressions = generated.map(_._1)
    val results = generated.map { case (_, n) => NumV(n) }
    val types = expressions.map(_ => IntType())
    testExpression(createExprTable(expressions, results, types), s"$depth-depth nested arithmetic operations")
  }

  for (depth <- 0 to 6) {
    nestedArithmeticOperationsTests(depth)
  }

  private val nested3DepthExpressions = (0 until 10).map(_ => generateExpression(3))
  private val nested3DepthExpressionsTable = createExprTable(
    nested3DepthExpressions.map(_._1), nested3DepthExpressions.map { case (_, n) => NumV(n) },
    nested3DepthExpressions.map(_ => IntType())
  )

  property("Commutativity of expressions") {
    forAll(nested3DepthExpressionsTable) {
      case (Plus(e1, e2), _, _) => Plus(e1, e2).eval(Map()) shouldBe Plus(e2, e1).eval(Map())
      case (Times(e1, e2), _, _) => Times(e1, e2).eval(Map()) shouldBe Times(e2, e1).eval(Map())
    }
  }

  property("Identity expressions have no effect") {
    forAll(nested3DepthExpressionsTable) {
      if (Random.nextBoolean()) {
        case (e, res, _) => Plus(e, Num(0)).eval(Map()) shouldBe res
      } else {
        case (e, res, _) => Times(e, Num(1)).eval(Map()) shouldBe res
      }
    }
  }

  property("Arithmetic expressions should print appropriately") {
    prettyPrint(Num(15)) shouldBe "15"
    prettyPrint(Plus(Num(15), Num(20))) shouldBe "(15 + 20)"
    prettyPrint(Times(Num(15), Num(20))) shouldBe "(15 × 20)"
    prettyPrint(Plus(Num(15), Times(Num(20), Num(25)))) shouldBe "(15 + (20 × 25))"
    prettyPrint(Times(Plus(Num(15), Num(20)), Num(25))) shouldBe "((15 + 20) × 25)"
    prettyPrint(Times(Plus(Num(15), Num(20)), Plus(Num(25), Num(30)))) shouldBe "((15 + 20) × (25 + 30))"
  }

  property("NumV should print appropriately") {
    prettyPrint(NumV(15)) shouldBe "15"
    prettyPrint(NumV(-15)) shouldBe "-15"
  }

  property("Children of arithmetic expressions is accurate") {
    def getChildrenExpressions(expr: Expr): List[Expr] = expr.getChildrenEval().map(_._1.asInstanceOf[Expr])

    getChildrenExpressions(Num(15)) shouldBe Nil
    getChildrenExpressions(Plus(Num(15), Num(20))) shouldBe List(Num(15), Num(20))
    getChildrenExpressions(Times(Num(15), Num(20))) shouldBe List(Num(15), Num(20))
    getChildrenExpressions(Plus(Num(15), Times(Num(20), Num(25)))) shouldBe List(Num(15), Times(Num(20), Num(25)))
    getChildrenExpressions(Times(Plus(Num(15), Num(20)), Num(25))) shouldBe List(Plus(Num(15), Num(20)), Num(25))
    getChildrenExpressions(Times(Plus(Num(15), Num(20)), Plus(Num(25), Num(30)))) shouldBe List(Plus(Num(15), Num(20)), Plus(Num(25), Num(30)))
  }

  property("Num with non-integer literal inputs results in errors") {
    val invalidNumLiterals = List(
      LiteralString("a"), LiteralBool(true), LiteralBool(false), LiteralString("5"), LiteralString("45j"),
      LiteralAny("--1"), LiteralAny("5O"), LiteralAny("34.1"), LiteralAny("\"0\"")
    )
    val invalidNumLiteralsTable = Table("InvalidNumLiterals", invalidNumLiterals: _*)
    forAll(invalidNumLiteralsTable) {
      literal => {
        Num(literal).eval(Map()) shouldBe an[EvalError]
        Num(literal).typeCheck(Map()) shouldBe an[TypeError]
      }
    }
  }

  property("Num with integer literal inputs is correctly interpreted") {
    forAll(nums) {
      num => {
        Num(Literal.fromString(num.toString)).eval(Map()) shouldBe NumV(num)
        Num(LiteralInt(num)).eval(Map()) shouldBe NumV(num)
      }
    }
  }
}
