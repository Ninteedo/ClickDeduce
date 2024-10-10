package languages

import languages.LData.*
import org.scalatest.matchers.should.Matchers.{a, be, should, shouldBe, shouldEqual}

class LDataTest extends TestTemplate[Expr, Value, Type] {
  testExpression(
    "Pair",
    Table(
      testExpressionTableHeading,
      (Pair(Num(1), Num(2)), PairV(NumV(1), NumV(2)), PairType(IntType(), IntType())),
      (Pair(Num(1), Bool(true)), PairV(NumV(1), BoolV(true)), PairType(IntType(), BoolType())),
      (
        Pair(Lambda("x", IntType(), Num(1)), Num(-1)),
        PairV(LambdaV("x", IntType(), Num(1), Env()), NumV(-1)),
        PairType(Func(IntType(), IntType()), IntType())
      ),
      (
        Pair(Pair(Bool(true), Plus(Num(1), Num(2))), Bool(false)),
        PairV(PairV(BoolV(true), NumV(3)), BoolV(false)),
        PairType(PairType(BoolType(), IntType()), BoolType())
      )
    )
  )

  testExpression(
    "Fst",
    Table(
      testExpressionTableHeading,
      (Fst(Pair(Num(1), Num(2))), NumV(1), IntType()),
      (Fst(Pair(Num(1), Bool(true))), NumV(1), IntType()),
      (
        Fst(Pair(Lambda("x", IntType(), Num(1)), Num(-1))),
        LambdaV("x", IntType(), Num(1), Env()),
        Func(IntType(), IntType())
      ),
      (
        Fst(Pair(Pair(Bool(true), Plus(Num(1), Num(2))), Bool(false))),
        PairV(BoolV(true), NumV(3)),
        PairType(BoolType(), IntType())
      )
    )
  )

  testExpression(
    "Snd",
    Table(
      testExpressionTableHeading,
      (Snd(Pair(Num(1), Num(2))), NumV(2), IntType()),
      (Snd(Pair(Num(1), Bool(true))), BoolV(true), BoolType()),
      (Snd(Pair(Lambda("x", IntType(), Num(1)), Num(-1))), NumV(-1), IntType()),
      (Snd(Pair(Pair(Bool(true), Plus(Num(1), Num(2))), Bool(false))), BoolV(false), BoolType())
    )
  )

  testExpression(
    "LetPair",
    Table(
      testExpressionTableHeading,
      (LetPair("x", "y", Pair(Num(1), Num(2)), Plus(Var("x"), Var("y"))), NumV(3), IntType()),
      (LetPair("x", "y", Pair(Num(1), Bool(true)), IfThenElse(Var("y"), Var("x"), Num(-100))), NumV(1), IntType()),
      (
        LetPair("x", "y", Pair(Lambda("x", IntType(), Plus(Var("x"), Num(1))), Num(-1)), Apply(Var("x"), Var("y"))),
        NumV(0),
        IntType()
      ),
      (LetPair("x", "y", Pair(Pair(Bool(true), Plus(Num(1), Num(2))), Bool(false)), Snd(Var("x"))), NumV(3), IntType())
    )
  )

  testExpression("UnitExpr", Table(testExpressionTableHeading, (UnitExpr(), UnitV(), EmptyType())))

  testExpression(
    "Left",
    Table(
      testExpressionTableHeading,
      (Left(Num(1), BoolType()), LeftV(NumV(1), BoolType()), UnionType(IntType(), BoolType())),
      (Left(Bool(true), IntType()), LeftV(BoolV(true), IntType()), UnionType(BoolType(), IntType())),
      (
        Left(Lambda("x", IntType(), Num(1)), IntType()),
        LeftV(LambdaV("x", IntType(), Num(1), Env()), IntType()),
        UnionType(Func(IntType(), IntType()), IntType())
      ),
      (
        Left(Pair(Bool(true), Plus(Num(1), Num(2))), PairType(Func(IntType(), BoolType()), IntType())),
        LeftV(PairV(BoolV(true), NumV(3)), PairType(Func(IntType(), BoolType()), IntType())),
        UnionType(PairType(BoolType(), IntType()), PairType(Func(IntType(), BoolType()), IntType()))
      )
    )
  )

  testExpression(
    "Right",
    Table(
      testExpressionTableHeading,
      (Right(BoolType(), Num(1)), RightV(BoolType(), NumV(1)), UnionType(BoolType(), IntType())),
      (Right(IntType(), Bool(true)), RightV(IntType(), BoolV(true)), UnionType(IntType(), BoolType())),
      (
        Right(BoolType(), Lambda("x", IntType(), Num(1))),
        RightV(BoolType(), LambdaV("x", IntType(), Num(1), Env())),
        UnionType(BoolType(), Func(IntType(), IntType()))
      ),
      (
        Right(PairType(IntType(), Func(BoolType(), BoolType())), Pair(Bool(true), Plus(Num(1), Num(2)))),
        RightV(PairType(IntType(), Func(BoolType(), BoolType())), PairV(BoolV(true), NumV(3))),
        UnionType(PairType(IntType(), Func(BoolType(), BoolType())), PairType(BoolType(), IntType()))
      )
    )
  )

  testExpression(
    "CaseSwitch",
    Table(
      testExpressionTableHeading,
      (
        CaseSwitch(Left(Lambda("x", BoolType(), Bool(false)), BoolType()), "x", "y", Num(100), Num(-200)),
        NumV(100),
        IntType()
      ),
      (CaseSwitch(Right(BoolType(), Num(1)), "x", "y", Num(100), Num(-200)), NumV(-200), IntType()),
      (CaseSwitch(Right(IntType(), Num(1)), "x", "error", Plus(Num(2), Num(10)), Var("error")), NumV(1), IntType()),
      (
        CaseSwitch(Left(Plus(Num(1), Num(2)), IntType()), "a", "b", Times(Num(3), Var("a")), Times(Num(4), Var("b"))),
        NumV(9),
        IntType()
      ),
      (
        CaseSwitch(
          Right(Func(IntType(), IntType()), Num(1)),
          "x",
          "y",
          Apply(Var("x"), Num(10)),
          Plus(Var("y"), Num(100))
        ),
        NumV(101),
        IntType()
      ),
      (
        CaseSwitch(
          Left(Lambda("x", IntType(), Plus(Var("x"), Num(1))), BoolType()),
            "x",
            "y",
          Left(Apply(Var("x"), Num(-1)), Func(IntType(), BoolType())),
          Right(IntType(), Lambda("x", IntType(), Equal(Var("x"), Num(0))))
        ),
        LeftV(NumV(0), Func(IntType(), BoolType())),
        UnionType(IntType(), Func(IntType(), BoolType()))
      )
    )
  )

  property("Complex CaseSwitch type scenarios are type-checked correctly") {
    val cases = Table(
      ("expr", "type"),
      (CaseSwitch(Left(Num(1), BoolType()), "x", "y", Num(100), Num(-200)), IntType()),
      (CaseSwitch(Right(IntType(), Num(1)), "x", "y", Num(100), Num(-200)), IntType()),
      (CaseSwitch(Left(Num(1), IntType()), "x", "y", Num(100), Bool(true)), TypeMismatchType(IntType(), BoolType())),
      (
        CaseSwitch(Right(Func(IntType(), IntType()), Num(1)), "x", "y", Lambda("x", IntType(), Num(1)), Num(-200)),
        TypeMismatchType(Func(IntType(), IntType()), IntType())
      ),
      (
        CaseSwitch(Left(Num(1), BoolType()), "x", "y", Left(Num(100), IntType()), Right(IntType(), Num(-200))),
        UnionType(IntType(), IntType())
      ),
      (
        CaseSwitch(Right(BoolType(), Num(1)), "x", "y", Left(Num(100), BoolType()), Right(IntType(), Bool(true))),
        UnionType(IntType(), BoolType())
      ),
      (
        CaseSwitch(Left(Num(1), IntType()), "x", "y", Left(Num(100), BoolType()), Left(Num(-200), BoolType())),
        UnionType(IntType(), BoolType())
      ),
      (
        CaseSwitch(
          Left(Bool(false), IntType()),
          "x",
          "y",
          Right(Func(IntType(), IntType()), Bool(true)),
          Right(Func(IntType(), IntType()), Bool(false))
        ),
        UnionType(Func(IntType(), IntType()), BoolType())
      )
    )

    forAll(cases) { (expr, expectedType) =>
      expr.typeCheck() shouldEqual expectedType
    }
  }

  property("Fst and Snd error when called with a value other than a pair") {
    val cases = Table(
      "expr",
      Fst(Num(1)),
      Fst(Bool(true)),
      Fst(Lambda("x", IntType(), Num(1))),
      Fst(Left(Pair(Bool(true), Bool(false)), IntType())),
      Snd(Num(1)),
      Snd(Bool(true)),
      Snd(Lambda("x", IntType(), Num(1))),
      Snd(Left(Pair(Bool(true), Bool(false)), IntType()))
    )

    forAll(cases) { expr =>
      expr.typeCheck() shouldBe a[TupleOperationOnNonTupleType]
      expr.eval() shouldBe a[TupleOperationOnNonTupleValue]
    }
  }

  property("CaseSwitch type-checks to errors in certain situations") {
    CaseSwitch(Num(1), "x", "y", Num(100), Num(-200)).typeCheck() shouldEqual CaseSwitchOnNonUnionType(IntType())
    CaseSwitch(Pair(Num(1), Num(2)), "x", "y", Num(100), Num(-200)).typeCheck() shouldEqual
      CaseSwitchOnNonUnionType(PairType(IntType(), IntType()))

    CaseSwitch(Left(Num(1), BoolType()), "x", "y", Num(10), Bool(true)).typeCheck() shouldEqual
      TypeMismatchType(IntType(), BoolType())
    CaseSwitch(Right(BoolType(), Bool(false)), "x", "y", Left(Num(10), BoolType()), Left(Bool(true), BoolType()))
      .typeCheck() shouldEqual
      TypeMismatchType(UnionType(IntType(), BoolType()), UnionType(BoolType(), BoolType()))
  }

  property("UnionFunctionWithNumber2Task is checked correctly") {
    UnionFunctionWithNumber2Task.checkFulfilled(
      Lambda("x", UnionType(IntType(), Func(IntType(), IntType())),
        CaseSwitch(Var("x"), "n", "f", Times(Var("n"), Num(2)), Apply(Var("f"), Num(2))))
    ) shouldBe true
  }
}
