package languages

import languages.LArith.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

class ReadExprTest extends AnyFunSuite {
  test("can load a simple addition of 2 numbers using LArith correctly") {
    val originalExpr = Plus(Num(1), Num(2))
    val expr = LArith.readExpr(originalExpr.toString)
    expr.get should be (originalExpr)
  }

  test("can load an LArith expression with multiple nested operations correctly") {
    val originalExpression = Times(Plus(Num(1), Num(2)), Times(Num(3), Plus(Num(4), Num(5))))
    val expr = LArith.readExpr(originalExpression.toString)
    expr.get should be (originalExpression)
  }

  test("can load an LArith expression with a very large integer literal") {
    val originalExpression = Num(BigInt("12345678901234567890"))
    val expr = LArith.readExpr(originalExpression.toString)
    expr.get should be (originalExpression)
  }
}
