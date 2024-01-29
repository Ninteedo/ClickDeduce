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
        PairV(LambdaV("x", IntType(), Num(1), Map()), NumV(-1)),
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
        LambdaV("x", IntType(), Num(1), Map()),
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
      (Left(Num(1)), LeftV(NumV(1)), UnionType(IntType(), UnknownType())),
      (Left(Bool(true)), LeftV(BoolV(true)), UnionType(BoolType(), UnknownType())),
      (
        Left(Lambda("x", IntType(), Num(1))),
        LeftV(LambdaV("x", IntType(), Num(1), Map())),
        UnionType(Func(IntType(), IntType()), UnknownType())
      ),
      (
        Left(Pair(Bool(true), Plus(Num(1), Num(2)))),
        LeftV(PairV(BoolV(true), NumV(3))),
        UnionType(PairType(BoolType(), IntType()), UnknownType())
      )
    )
  )

  testExpression(
    "Right",
    Table(
      testExpressionTableHeading,
      (Right(Num(1)), RightV(NumV(1)), UnionType(UnknownType(), IntType())),
      (Right(Bool(true)), RightV(BoolV(true)), UnionType(UnknownType(), BoolType())),
      (
        Right(Lambda("x", IntType(), Num(1))),
        RightV(LambdaV("x", IntType(), Num(1), Map())),
        UnionType(UnknownType(), Func(IntType(), IntType()))
      ),
      (
        Right(Pair(Bool(true), Plus(Num(1), Num(2)))),
        RightV(PairV(BoolV(true), NumV(3))),
        UnionType(UnknownType(), PairType(BoolType(), IntType()))
      )
    )
  )

  testExpression(
    "CaseSwitch",
    Table(
      testExpressionTableHeading,
      (CaseSwitch(Left(Num(1)), "x", "y", Num(100), Num(-200)), NumV(100), IntType()),
      (CaseSwitch(Right(Num(1)), "x", "y", Num(100), Num(-200)), NumV(-200), IntType()),
      (CaseSwitch(Right(Num(1)), "x", "error", Plus(Num(2), Num(10)), Var("error")), NumV(1), IntType()),
      (
        CaseSwitch(Left(Plus(Num(1), Num(2))), "a", "b", Times(Num(3), Var("a")), Times(Num(4), Var("b"))),
        NumV(9),
        IntType()
      )
    )
  )

  property("Complex CaseSwitch type scenarios are type-checked correctly") {
    val cases = Table(
      ("expr", "type"),
      (CaseSwitch(Left(Num(1)), "x", "y", Num(100), Num(-200)), IntType()),
      (CaseSwitch(Right(Num(1)), "x", "y", Num(100), Num(-200)), IntType()),
      (CaseSwitch(Left(Num(1)), "x", "y", Num(100), Bool(true)), TypeMismatchType(IntType(), BoolType())),
      (
        CaseSwitch(Right(Num(1)), "x", "y", Lambda("x", IntType(), Num(1)), Num(-200)),
        TypeMismatchType(Func(IntType(), IntType()), IntType())
      ),
      (CaseSwitch(Left(Num(1)), "x", "y", Left(Num(100)), Right(Num(-200))), UnionType(IntType(), IntType())),
      (CaseSwitch(Right(Num(1)), "x", "y", Left(Num(100)), Right(Bool(true))), UnionType(IntType(), BoolType())),
      (CaseSwitch(Left(Num(1)), "x", "y", Left(Num(100)), Left(Num(-200))), UnionType(IntType(), UnknownType())),
      (
        CaseSwitch(Left(Bool(false)), "x", "y", Right(Bool(true)), Right(Bool(false))),
        UnionType(UnknownType(), BoolType())
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
      Fst(Left(Pair(Bool(true), Bool(false)))),
      Snd(Num(1)),
      Snd(Bool(true)),
      Snd(Lambda("x", IntType(), Num(1))),
      Snd(Left(Pair(Bool(true), Bool(false))))
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

    CaseSwitch(Left(Num(1)), "x", "y", Num(10), Bool(true)).typeCheck() shouldEqual
      TypeMismatchType(IntType(), BoolType())
    CaseSwitch(Right(Bool(false)), "x", "y", Left(Num(10)), Left(Bool(true))).typeCheck() shouldEqual
      TypeMismatchType(IntType(), BoolType())
  }

}
