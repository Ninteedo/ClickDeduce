package languages

import languages.terms.blanks.BlankTypeDropDown
import languages.terms.exprs.Expr
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.prop.TableDrivenPropertyChecks

class ExprParseTest extends AnyFunSuite, TableDrivenPropertyChecks {
  private def testParses(l: AbstractLanguage, cases: Seq[(String, Expr)]): Unit = {
    val table = Table(("text", "expr"), cases*)
    forAll(table) { (text, expr) =>
      l.parseExpr(text) should be(Some(expr))
    }
  }

  test("can parse a single number") {
    val l = LArith
    val numbers = Table(
      "number",
      1,
      45,
      398517195685675876L,
      -3489
    )

    forAll(numbers) { number =>
      val expr = l.parseExpr(number.toString)
      expr should be(Some(l.Num(number)))
    }
  }

  test("can parse a simple plus or times") {
    val l = LArith
    testParses(l, List(
      ("1 + 2", l.Plus(l.Num(1), l.Num(2))),
      ("-45 + 481", l.Plus(l.Num(-45), l.Num(481))),
      ("1 * 2", l.Times(l.Num(1), l.Num(2))),
      ("-45 * 481", l.Times(l.Num(-45), l.Num(481)))
    ))
  }

  test("can parse multiple plus and times with brackets") {
    val l = LArith
    testParses(l, List(
      ("(1 + 2) * 3", l.Times(l.Plus(l.Num(1), l.Num(2)), l.Num(3))),
      ("1 * (2 + 3)", l.Times(l.Num(1), l.Plus(l.Num(2), l.Num(3)))),
      ("(1 * 2) + 3", l.Plus(l.Times(l.Num(1), l.Num(2)), l.Num(3))),
      ("1 + (2 * 3)", l.Plus(l.Num(1), l.Times(l.Num(2), l.Num(3))))
    ))
  }

  test("can parse multiple plus and times without brackets") {
    val l = LArith
    testParses(l, List(
      ("1 + 2 + 3", l.Plus(l.Plus(l.Num(1), l.Num(2)), l.Num(3))),
      ("1 * 2 * 3", l.Times(l.Times(l.Num(1), l.Num(2)), l.Num(3))),
      ("1 * 2 + 3", l.Plus(l.Times(l.Num(1), l.Num(2)), l.Num(3))),
      ("1 + 2 * 3", l.Plus(l.Num(1), l.Times(l.Num(2), l.Num(3)))),
    ))
  }

  test("can parse LIf expressions") {
    val l = LIf
    testParses(l, List(
      ("true", l.Bool(true)),
      ("false", l.Bool(false)),
      ("True", l.Bool(true)),
      ("False", l.Bool(false)),
      ("if true then 1 else 2", l.IfThenElse(l.Bool(true), l.Num(1), l.Num(2))),
      ("if false then 1 else 2", l.IfThenElse(l.Bool(false), l.Num(1), l.Num(2))),
      ("if true then 1 else if false then 2 else 3", l.IfThenElse(l.Bool(true), l.Num(1), l.IfThenElse(l.Bool(false), l.Num(2), l.Num(3)))),
      ("if true then if false then 1 else 2 else 3", l.IfThenElse(l.Bool(true), l.IfThenElse(l.Bool(false), l.Num(1), l.Num(2)), l.Num(3))),
      ("34 == -12", l.Equal(l.Num(34), l.Num(-12))),
      ("746 < true", l.LessThan(l.Num(746), l.Bool(true))),
      ("67 + 12 == 79", l.Equal(l.Plus(l.Num(67), l.Num(12)), l.Num(79))),
      ("67 + (12 == 79)", l.Plus(l.Num(67), l.Equal(l.Num(12), l.Num(79)))),
      ("if 67 + 12 == 79 then 1 else 2", l.IfThenElse(l.Equal(l.Plus(l.Num(67), l.Num(12)), l.Num(79)), l.Num(1), l.Num(2)))
    ))
  }

  test("can parse LLam expressions") {
    val l = LLam
    testParses(l, List(
      ("\\x: int. x + 1", l.Lambda("x", l.IntType(), l.Plus(l.Var("x"), l.Num(1)))),
      ("\\x: InT. x + 1", l.Lambda("x", l.IntType(), l.Plus(l.Var("x"), l.Num(1)))),
      ("lambda myBool: bool. if myBool then 1 else -1", l.Lambda("myBool", l.BoolType(), l.IfThenElse(l.Var("myBool"), l.Num(1), l.Num(-1)))),
      ("\\x. x * 2", l.Lambda("x", BlankTypeDropDown(l), l.Times(l.Var("x"), l.Num(2)))),
      ("(\\x: int. x + 1) 5", l.Apply(l.Lambda("x", l.IntType(), l.Plus(l.Var("x"), l.Num(1))), l.Num(5))),
      ("f x y", l.Apply(l.Apply(l.Var("f"), l.Var("x")), l.Var("y"))),
      ("f (x y)", l.Apply(l.Var("f"), l.Apply(l.Var("x"), l.Var("y")))),
      ("f (x y) z", l.Apply(l.Apply(l.Var("f"), l.Apply(l.Var("x"), l.Var("y"))), l.Var("z")))
    ))
  }
}
