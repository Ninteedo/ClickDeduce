package languages

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1, TableFor3}
import org.scalatest.propspec.AnyPropSpec

trait TestTemplate[E <: ClickDeduceLanguage#Expr, V <: ClickDeduceLanguage#Value, T <: ClickDeduceLanguage#Type]
  extends AnyPropSpec with TableDrivenPropertyChecks with GivenWhenThen {
  /**
   * Run tests for the type-checking and evaluation of a table of expressions and their correct results and types.
   *
   * @param table          the table of expressions, results, and types
   * @param expressionName the name of the expression to display in the test name
   */
  def testExpression(table: TableFor3[E, V, T], expressionName: String): Unit = {
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
    expressions: Iterable[E],
    results: Iterable[V],
    types: Iterable[T]
  ): TableFor3[E, V, T] = {
    val zipped = List(expressions, results, types).transpose.map {
      case List(a: E, b: V, c: T) => (a, b, c)
      case v => throw new Exception(s"Unexpected value in createExprTable: $v")
    }
    Table(("expressions", "results", "types"), zipped: _*)
  }
}
