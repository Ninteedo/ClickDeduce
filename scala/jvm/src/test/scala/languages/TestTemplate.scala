package languages

import languages.env.*
import languages.terms.*
import languages.terms.builders.*
import languages.terms.errors.*
import languages.terms.exprs.Expr
import languages.terms.literals.*
import languages.terms.types.Type
import languages.terms.values.Value
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1, TableFor3}
import org.scalatest.propspec.AnyPropSpec

import scala.util.Random

trait TestTemplate
    extends AnyPropSpec
    with TableDrivenPropertyChecks
    with GivenWhenThen {
  Random.setSeed(2024)

  /** Run tests for the type-checking and evaluation of a table of expressions and their correct results and types.
    *
    * @param table
    *   the table of expressions, results, and types
    * @param expressionName
    *   the name of the expression to display in the test name
    */
  def testExpression(expressionName: String, table: TableFor3[Expr, Value, Type]): Unit = {
    property(s"$expressionName type-checks correctly") {
      forAll(table)((expr, _, typ) => {
        expr.typeCheck() shouldBe typ
      })
    }

    property(s"$expressionName evaluates correctly") {
      forAll(table)((expr, res, _) => {
        expr.eval() shouldBe res
      })
    }
  }

  val testExpressionTableHeading: (String, String, String) = ("expr", "value", "type")

  /** Create a table of expressions, their correct evaluation results, and their correct types.
    */
  def createExprTable(expressions: Iterable[Expr], results: Iterable[Value], types: Iterable[Type]): TableFor3[Expr, Value, Type] = {
    val zipped = List(expressions, results, types).transpose.map {
      case List(a: Expr, b: Value, c: Type) => (a, b, c)
      case v                      => throw new Exception(s"Unexpected value in createExprTable: $v")
    }
    Table(("expressions", "results", "types"), zipped: _*)
  }

  def createExprTable(tuples: (Expr, Value, Type)*): TableFor3[Expr, Value, Type] = {
    Table(("expressions", "results", "types"), tuples.toSeq: _*)
  }

  val bools: TableFor1[Boolean] = Table("bool", true, false)
  val intValues: List[BigInt] =
    List(0, 1, -1, 2, -2, 5, -5, 10, 100, -100, 198765, -157396, 5168765, -4376418, 159871598156996L)
  val nums: TableFor1[BigInt] = Table("num", intValues: _*)
  val variableNames: List[String] = List("a", "b", "x", "y", "foo", "bar", "gha867", "p1f")
  val invalidVariableNames: List[String] = List("", ".", "1", "1foo", "foo bar", "845", " x")
}
