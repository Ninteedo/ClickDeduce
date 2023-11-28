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

  val intValues: List[BigInt] = List(0, 1, -1, 2, -2, 5, -5, 10, 100, -100, 198765, -157396, 5168765, -4376418)
  val nums: TableFor1[BigInt] = Table("num", intValues: _*)

  property("Num type-checks to IntType") {
    forAll(nums) { n =>
      typeOf(Num(n), Map()) should be(IntType())
    }
  }

  property("Num correctly evaluates to NumV") {
    forAll(nums) { n =>
      eval(Num(n), Map()) should be(NumV(n))
    }
  }

  def testExpression(table: TableFor3[Expr, Value, Type], expressionName: String): Unit = {
    property(s"$expressionName type-checks correctly") {
      forAll(table) { (expr, _, typ) => {
        typeOf(expr, Map()) should be(typ)
      }
      }
    }

    property(s"$expressionName evaluates correctly") {
      forAll(table) { (expr, res, _) =>
        eval(expr, Map()) should be(res)
      }
    }
  }

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

  private def arithmeticOperationTests(op: (Num, Num) => Expr, opEval: (BigInt, BigInt) => BigInt): Unit = {
    val table = {
      val tuples = for {
        a <- intValues
        b <- intValues
      } yield (a, b)
      val expressions = tuples.map { case (n, m) => op(Num(n), Num(m)) }
      val results = tuples.map { case (n, m) => NumV(opEval(n, m)) }
      val types = expressions.map(_ => IntType())
      Table(("expressions", "results", "types"), createExprTable(expressions, results, types): _*)
    }

    testExpression(table, op.toString)
  }

  arithmeticOperationTests(Plus.apply, _ + _)
  arithmeticOperationTests(Times.apply, _ * _)

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
        case 2 =>
          val (other_expr, other_total) = generateExpression(0)
          if (Random.nextBoolean()) {
            left_expr = other_expr
            left_total = other_total
          } else {
            right_expr = other_expr
            right_total = other_total
          }
          if (Random.nextBoolean()) {
            (Plus(left_expr, right_expr), left_total + right_total)
          } else {
            (Times(left_expr, right_expr), left_total * right_total)
          }
      }
    }
  }

  private def nestedArithmeticOperationsTests(depth: Int): Unit = {
    val generated = for {
      _ <- 0 until 10
    } yield generateExpression(depth)
    val expressions = generated.map(_._1)
    val results = generated.map { case (_, n) => NumV(n) }
    val types = expressions.map(_ => IntType())
    testExpression(createExprTable(expressions, results, types), s"$depth-depth nested arithmetic operations")
  }

  for (depth <- 1 to 5) {
    nestedArithmeticOperationsTests(depth)
  }

  private val nested3DepthExpressions = for {
    _ <- 0 until 10
  } yield generateExpression(3)
  private val nested3DepthExpressionsTable = createExprTable(
    nested3DepthExpressions.map(_._1), nested3DepthExpressions.map { case (_, n) => NumV(n) },
    nested3DepthExpressions.map(_ => IntType())
  )

  property("Commutativity of expressions") {
    forAll(nested3DepthExpressionsTable) {
      case (Plus(e1, e2), _, _) => eval(Plus(e1, e2), Map()) should be(eval(Plus(e2, e1), Map()))
      case (Times(e1, e2), _, _) => eval(Times(e1, e2), Map()) should be(eval(Times(e2, e1), Map()))
    }
  }

  property("Identity expressions have no effect") {
    forAll(nested3DepthExpressionsTable) {
      if (Random.nextBoolean()) {
        case (e, res, _) => eval(Plus(e, Num(0)), Map()) should be(res)
      } else {
        case (e, res, _) => eval(Times(e, Num(1)), Map()) should be(res)
      }
    }
  }

  property("Arithmetic expressions should print appropriately") {
    prettyPrint(Num(15)) should be("15")
    prettyPrint(Plus(Num(15), Num(20))) should be("(15 + 20)")
    prettyPrint(Times(Num(15), Num(20))) should be("(15 × 20)")
    prettyPrint(Plus(Num(15), Times(Num(20), Num(25)))) should be("(15 + (20 × 25))")
    prettyPrint(Times(Plus(Num(15), Num(20)), Num(25))) should be("((15 + 20) × 25)")
    prettyPrint(Times(Plus(Num(15), Num(20)), Plus(Num(25), Num(30)))) should be("((15 + 20) × (25 + 30))")
  }

  property("NumV should print appropriately") {
    prettyPrint(NumV(15)) should be("15")
    prettyPrint(NumV(-15)) should be("-15")
  }

  property("Children of arithmetic expressions is accurate") {
    Num(15).children should be(Nil)
    Plus(Num(15), Num(20)).children should be(List(Num(15), Num(20)))
    Times(Num(15), Num(20)).children should be(List(Num(15), Num(20)))
    Plus(Num(15), Times(Num(20), Num(25))).children should be(List(Num(15), Times(Num(20), Num(25))))
    Times(Plus(Num(15), Num(20)), Num(25)).children should be(List(Plus(Num(15), Num(20)), Num(25)))
    Times(Plus(Num(15), Num(20)), Plus(Num(25), Num(30))).children should be(
      List(Plus(Num(15), Num(20)), Plus(Num(25), Num(30)))
    )
  }

  property("Num with non-integer literal inputs results in errors") {
    val invalidNumLiterals = List(
      LiteralString("a"), LiteralBool(true), LiteralBool(false), LiteralString("5"), LiteralString("45j"),
      LiteralAny("--1"), LiteralAny("5O"), LiteralAny("34.1"), LiteralAny("\"0\"")
    )
    val invalidNumLiteralsTable = Table("InvalidNumLiterals", invalidNumLiterals: _*)
    forAll(invalidNumLiteralsTable) {
      literal => {
        eval(Num(literal)) shouldBe an[EvalError]
        typeOf(Num(literal)) shouldBe an[TypeError]
      }
    }
  }

  property("Num with integer literal inputs is correctly interpreted") {
    forAll(nums) {
      num => {
        eval(Num(Literal.fromString(num.toString))) shouldBe NumV(num)
        eval(Num(LiteralInt(num))) shouldBe NumV(num)
      }
    }
  }
}
