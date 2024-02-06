package languages

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.prop.TableDrivenPropertyChecks.forAll

class ReadExprTest extends AnyFunSuite, TableDrivenPropertyChecks {
  test("can load a simple addition of 2 numbers using LArith correctly") {
    val l = LArith
    val originalExpr = l.Plus(l.Num(1), l.Num(2))
    val expr = l.readExpr(originalExpr.toString)
    expr.get should be(originalExpr)
  }

  test("can load an LArith expression with multiple nested operations correctly") {
    val l = LArith
    val originalExpression = l.Times(l.Plus(l.Num(1), l.Num(2)), l.Times(l.Num(3), l.Plus(l.Num(4), l.Num(5))))
    val expr = l.readExpr(originalExpression.toString)
    expr.get should be(originalExpression)
  }

  test("can load an LArith expression with a very large integer literal") {
    val l = LArith
    val originalExpression = l.Num(BigInt("12345678901234567890"))
    val expr = l.readExpr(originalExpression.toString)
    expr.get should be(originalExpression)
  }

  test("LIf expression reading") {
    val l = LIf
    val expressions = Table(
      "expr",
      l.Bool(true),
      l.Bool(false),
      l.Equal(l.Num(1), l.Num(2)),
      l.LessThan(l.Equal(l.Bool(true), l.Bool(false)), l.Bool(true)),
      l.IfThenElse(l.Bool(true), l.Num(1), l.Num(2)),
      l.IfThenElse(l.Equal(l.Num(1), l.Num(2)), l.Num(1), l.IfThenElse(l.Bool(true), l.Num(1), l.Num(2)))
    )

    forAll(expressions) { expr =>
      l.readExpr(expr.toString) should be(Some(expr))
    }
  }

  test("LLet expression reading") {
    val l = LLet
    val expressions = Table(
      "expr",
      l.Let("x", l.Num(1), l.Bool(false)),
      l.Var("x"),
      l.Let("x", l.Num(1), l.Var("x")),
      l.Let("x", l.Num(1), l.Let("y", l.Num(2), l.Plus(l.Var("x"), l.Var("y")))),
    )

    forAll(expressions) { expr =>
      l.readExpr(expr.toString) should be(Some(expr))
    }
  }

  test("LLam expression reading") {
    val l = LLam
    val expressions = Table(
      "expr",
      l.Lambda("x", l.BoolType(), l.Var("x")),
      l.Lambda("x", l.Func(l.IntType(), l.BoolType()), l.Lambda("y", l.IntType(), l.Plus(l.Var("x"), l.Var("y")))),
      l.Apply(l.Lambda("x", l.IntType(), l.Times(l.Var("x"), l.Num(2))), l.Num(3))
    )

    forAll(expressions) { expr =>
      l.readExpr(expr.toString) should be(Some(expr))
    }
  }

  test("LRec expression reading") {
    val l = LRec
    val expressions = Table(
      "expr",
      l.Rec("f", "x", l.IntType(), l.IntType(), l.Var("x")),
    )

    forAll(expressions) { expr =>
      l.readExpr(expr.toString) should be(Some(expr))
    }
  }

  test("LData expression reading") {
    val l = LData
    val expressions = Table(
      "expr",
      l.Pair(l.Num(1), l.Num(2)),
      l.LetPair("x", "y", l.Pair(l.Num(1), l.Num(2)), l.Plus(l.Var("x"), l.Var("y"))),
      l.Fst(l.Pair(l.Num(1), l.Num(2))),
      l.Snd(l.Pair(l.Num(1), l.Num(2))),
      l.Left(l.Num(1), l.IntType()),
      l.Right(l.Func(l.IntType(), l.BoolType()), l.Num(1)),
      l.CaseSwitch(l.Left(l.Num(1), l.PairType(l.UnionType(l.IntType(), l.BoolType()), l.IntType())), "x", "y", l.Var("x"), l.Var("y")),
    )

    forAll(expressions) { expr =>
      l.readExpr(expr.toString) should be(Some(expr))
    }
  }

  test("LPoly expression reading") {
    val l = LPoly
    val expressions = Table(
      "expr",
      l.Poly("X", l.Num(5)),
      l.ApplyType(l.Poly("X", l.Num(5)), l.IntType()),
      l.Lambda("x", l.Func(l.TypeVar("X"), l.TypeVar("X")), l.Var("x")),
    )

    forAll(expressions) { expr =>
      l.readExpr(expr.toString) should be(Some(expr))
    }
  }
}
