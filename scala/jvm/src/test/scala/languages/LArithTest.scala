package languages

import convertors.{DisplayMode, HTMLConvertor}
import languages.LArith.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.prop.TableFor3
import org.scalatest.propspec.AnyPropSpec

import scala.util.Random

class LArithTest extends TestTemplate[Expr, Value, Type] {
  def genRandInt(): BigInt = {
    Random.nextInt(200) - 100
  }

  property("Num type-checks to IntType") {
    forAll(nums)(n => Num(n).typeCheck() shouldBe IntType())
  }

  property("Num correctly evaluates to NumV") {
    forAll(nums)(n => Num(n).eval() shouldBe NumV(n))
  }

  /** Run tests for binary arithmetic operations.
    * @param op
    *   the operation to test
    * @param opEval
    *   the correct evaluation result of the operation
    * @param opName
    *   the name of the operation
    */
  private def arithmeticOperationTests(
    op: (Num, Num) => Expr,
    opEval: (BigInt, BigInt) => BigInt,
    opName: String
  ): Unit = {
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

    testExpression(opName, table)
  }

  arithmeticOperationTests(Plus.apply, _ + _, "Plus")
  arithmeticOperationTests(Times.apply, _ * _, "Times")

  /** Generate a random expression of the given depth.
    * @param depth
    *   the maximum remaining depth of the expression
    * @return
    *   a tuple of the expression and its correct evaluation result
    */
  private def generateExpression(depth: Int): (Expr, BigInt) = {
    if (depth == 0) {
      val n = genRandInt()
      (Num(n), n)
    } else {
      var (left_expr, left_total): (Expr, BigInt) = generateExpression(depth - 1)
      var (right_expr, right_total): (Expr, BigInt) = generateExpression(depth - 1)
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
    testExpression(s"$depth-depth nested arithmetic operations", createExprTable(expressions, results, types))
  }

  for (depth <- 0 to 6) {
    nestedArithmeticOperationsTests(depth)
  }

  private val nested3DepthExpressions = (0 until 10).map(_ => generateExpression(3))
  private val nested3DepthExpressionsTable: TableFor3[Expr, Value, Type] = createExprTable(
    nested3DepthExpressions.map(_._1),
    nested3DepthExpressions.map { case (_, n) => NumV(n) },
    nested3DepthExpressions.map(_ => IntType())
  )

  property("Commutativity of expressions") {
    forAll(nested3DepthExpressionsTable) {
      case (Plus(e1, e2), _, _)  => Plus(e1, e2).eval() shouldBe Plus(e2, e1).eval()
      case (Times(e1, e2), _, _) => Times(e1, e2).eval() shouldBe Times(e2, e1).eval()
    }
  }

  property("Identity expressions have no effect") {
    forAll(nested3DepthExpressionsTable) {
      if (Random.nextBoolean()) { case (e, res, _) =>
        Plus(e, Num(0)).eval() shouldBe res
      }
      else { case (e, res, _) =>
        Times(e, Num(1)).eval() shouldBe res
      }
    }
  }

  property("Arithmetic expressions should print appropriately") {
    Num(15).prettyPrint shouldBe "15"
    Plus(Num(15), Num(20)).prettyPrint shouldBe "15 + 20"
    Times(Num(15), Num(20)).prettyPrint shouldBe "15 × 20"
    Plus(Num(15), Times(Num(20), Num(25))).prettyPrint shouldBe "15 + (20 × 25)"
    Times(Plus(Num(15), Num(20)), Num(25)).prettyPrint shouldBe "(15 + 20) × 25"
    Times(Plus(Num(15), Num(20)), Plus(Num(25), Num(30))).prettyPrint shouldBe "(15 + 20) × (25 + 30)"
  }

  property("Children of arithmetic expressions is accurate") {
    def getChildrenExpressions(expr: Expr): List[Expr] = expr.getChildrenEval().map(_._1.asInstanceOf[Expr])

    getChildrenExpressions(Num(15)) shouldBe Nil
    getChildrenExpressions(Plus(Num(15), Num(20))) shouldBe List(Num(15), Num(20))
    getChildrenExpressions(Times(Num(15), Num(20))) shouldBe List(Num(15), Num(20))
    getChildrenExpressions(Plus(Num(15), Times(Num(20), Num(25)))) shouldBe List(Num(15), Times(Num(20), Num(25)))
    getChildrenExpressions(Times(Plus(Num(15), Num(20)), Num(25))) shouldBe List(Plus(Num(15), Num(20)), Num(25))
    getChildrenExpressions(Times(Plus(Num(15), Num(20)), Plus(Num(25), Num(30)))) shouldBe List(
      Plus(Num(15), Num(20)),
      Plus(Num(25), Num(30))
    )
  }

  property("Num with non-integer literal inputs results in errors") {
    val invalidNumLiterals = List(
      LiteralString("a"),
      LiteralBool(true),
      LiteralBool(false),
      LiteralString("5"),
      LiteralString("45j"),
      LiteralAny("--1"),
      LiteralAny("5O"),
      LiteralAny("34.1"),
      LiteralAny("\"0\"")
    )
    val invalidNumLiteralsTable = Table("InvalidNumLiterals", invalidNumLiterals: _*)
    forAll(invalidNumLiteralsTable) { literal =>
      {
        Num(literal).eval() shouldBe an[EvalError]
        Num(literal).typeCheck() shouldBe an[TypeError]
      }
    }
  }

  property("Num with integer literal inputs is correctly interpreted") {
    forAll(nums) { num =>
      {
        Num(Literal.fromString(num.toString)).eval() shouldBe NumV(num)
        Num(LiteralInt(num)).eval() shouldBe NumV(num)
      }
    }
  }

  property("Num can accept integers at least 100 digits long") {
    val base = BigInt("10")
    val digits = 100
    val reallyBigInts = List(
      base.pow(digits) - 1,
      base.pow(digits),
      base.pow(digits) + 1,
      -base.pow(digits) + 1,
      -base.pow(digits),
      -base.pow(digits) - 1
    )

    forAll(Table("int", reallyBigInts: _*)) { num =>
      Num(LiteralInt(num)).eval() shouldBe NumV(num)
    }
  }

  property("Plus and Times pass errors on") {
    val invalidNumType = Num(LiteralString("invalid")).typeCheck()
    Plus(Num(LiteralString("invalid")), Num(5)).typeCheck() shouldEqual invalidNumType
    Plus(Num(5), Num(LiteralString("invalid"))).typeCheck() shouldEqual invalidNumType
    Times(Num(LiteralString("invalid")), Num(5)).typeCheck() shouldEqual invalidNumType
    Times(Num(5), Num(LiteralString("invalid"))).typeCheck() shouldEqual invalidNumType

    val invalidNumValue = Num(LiteralString("invalid")).eval()
    Plus(Num(LiteralString("invalid")), Num(5)).eval() shouldEqual invalidNumValue
    Plus(Num(5), Num(LiteralString("invalid"))).eval() shouldEqual invalidNumValue
    Times(Num(LiteralString("invalid")), Num(5)).eval() shouldEqual invalidNumValue
    Times(Num(5), Num(LiteralString("invalid"))).eval() shouldEqual invalidNumValue
  }

  property("Attempting to evaluate an expression not defined in LArith results in an error") {
    BlankExprDropDown().eval() shouldBe an[UnexpectedExpr]
    BlankExprDropDown().typeCheck() shouldBe an[UnexpectedExprType]

    Plus(BlankExprDropDown(), Num(5)).eval() shouldBe an[UnexpectedExpr]
    Plus(BlankExprDropDown(), Num(5)).typeCheck() shouldBe an[UnexpectedExprType]
    Plus(Num(5), BlankExprDropDown()).eval() shouldBe an[UnexpectedExpr]
    Plus(Num(5), BlankExprDropDown()).typeCheck() shouldBe an[UnexpectedExprType]

    Times(BlankExprDropDown(), Num(5)).eval() shouldBe an[UnexpectedExpr]
    Times(BlankExprDropDown(), Num(5)).typeCheck() shouldBe an[UnexpectedExprType]
    Times(Num(5), BlankExprDropDown()).eval() shouldBe an[UnexpectedExpr]
    Times(Num(5), BlankExprDropDown()).typeCheck() shouldBe an[UnexpectedExprType]
  }

  property("Num should pretty print correctly") {
    Num(15).prettyPrint shouldBe "15"
    Num(-15).prettyPrint shouldBe "-15"
    Num(0).prettyPrint shouldBe "0"
    val quiteBigInt = BigInt("965712796818796")
    Num(quiteBigInt).prettyPrint shouldBe quiteBigInt.toString
    val reallyBigInt = BigInt("965712796818796") * BigInt("8561575617798768176")
    Num(reallyBigInt).prettyPrint shouldBe reallyBigInt.toString
    val reallyNegativeBigInt = BigInt("965712796818796") * BigInt("-8561575617798768176")
    Num(reallyNegativeBigInt).prettyPrint shouldBe reallyNegativeBigInt.toString
  }

  property("Plus should pretty print correctly") {
    Plus(Num(15), Num(20)).prettyPrint shouldBe "15 + 20"
    Plus(Num(15), Num(-20)).prettyPrint shouldBe "15 + -20"
    Plus(Plus(Num(15), Num(20)), Num(25)).prettyPrint shouldBe "(15 + 20) + 25"
    Plus(Num(15), Plus(Num(20), Num(25))).prettyPrint shouldBe "15 + (20 + 25)"
    Plus(Plus(Num(15), Num(20)), Plus(Num(25), Num(30))).prettyPrint shouldBe "(15 + 20) + (25 + 30)"
  }

  property("Times should pretty print correctly") {
    Times(Num(15), Num(20)).prettyPrint shouldBe "15 × 20"
    Times(Num(15), Num(-20)).prettyPrint shouldBe "15 × -20"
    Times(Times(Num(15), Num(20)), Num(25)).prettyPrint shouldBe "(15 × 20) × 25"
    Times(Num(15), Times(Num(20), Num(25))).prettyPrint shouldBe "15 × (20 × 25)"
    Times(Times(Num(15), Num(20)), Times(Num(25), Num(30))).prettyPrint shouldBe "(15 × 20) × (25 × 30)"
  }

  property("Mixed Plus and Times should pretty print correctly") {
    Plus(Times(Num(15), Num(20)), Num(25)).prettyPrint shouldBe "(15 × 20) + 25"
    Plus(Num(15), Times(Num(20), Num(25))).prettyPrint shouldBe "15 + (20 × 25)"
    Plus(Times(Num(15), Num(20)), Times(Num(25), Num(30))).prettyPrint shouldBe "(15 × 20) + (25 × 30)"
  }

  property("NumV should pretty print correctly") {
    NumV(15).prettyPrint shouldBe "15"
    NumV(-15).prettyPrint shouldBe "-15"
  }

  property("IntType should pretty print correctly") {
    IntType().prettyPrint shouldBe "Int"
  }

  property("Num literal input is disabled in parent expressions") {
    val l = LArith
    val convertor = HTMLConvertor(l, DisplayMode.Edit)
    val tree = l.VariableNode(
      "Plus",
      List(l.SubExprNode(l.VariableNode("Num", List(l.LiteralNode("")))), l.SubExprNode(l.ExprChoiceNode()))
    )
    val html = convertor.convert(tree)
    val regex = """<input [\w\s-=":;]+/>""".r
    val matches = regex.findAllIn(html).toList
    println(matches.mkString("\n"))
    matches.length shouldBe 2
    matches.count(_.contains("readonly")) shouldBe 1
  }

  property("Expression and type builder names match expected") {
    LArith.exprBuilderNames shouldBe List(("Num", List("Number", "Integer")), ("Plus", List("Addition", "+")), ("Times", List("Multiplication", "Multiply", "*"))).map(entry => ("LArith", entry))
    LArith.typeBuilderNames shouldBe List(("IntType", List("Number", "Integer"))).map(entry => ("LArith", entry))
  }
}
