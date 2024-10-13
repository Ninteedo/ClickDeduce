package languages

import languages.LPoly.*
import org.scalatest.matchers.should.Matchers.*

class LPolyTest extends TestTemplate[Expr, Value, Type] {
  property("Example identity function expressions") {
    val literalT = Literal.fromString("T")
    val identityFunction = Poly(literalT, Lambda("x", TypeVar(literalT), Var("x")))

    identityFunction
      .typeCheck() shouldEqual PolyType(TypeVar(literalT), Func(TypeVar(literalT), TypeVar(literalT)))

    ApplyType(identityFunction, IntType())
      .eval() shouldEqual LambdaV("x", IntType(), Var("x"), Env("T" -> TypeValueContainer(IntType())))
    ApplyType(identityFunction, Func(IntType(), BoolType())).eval() shouldEqual LambdaV(
      "x",
      Func(IntType(), BoolType()),
      Var("x"),
      Env("T" -> TypeValueContainer(Func(IntType(), BoolType())))
    )

    Apply(ApplyType(identityFunction, IntType()), Num(1)).typeCheck() shouldEqual IntType()
    Apply(ApplyType(identityFunction, IntType()), Num(1)).eval() shouldEqual NumV(1)

    val recIdentityFunction = Poly(literalT, Rec("f", "x", TypeVar(literalT), TypeVar(literalT), Var("x")))
    recIdentityFunction.typeCheck() shouldEqual PolyType(TypeVar(literalT), Func(TypeVar(literalT), TypeVar(literalT)))
    ApplyType(recIdentityFunction, IntType()).typeCheck() shouldEqual Func(IntType(), IntType())
    ApplyType(recIdentityFunction, Func(IntType(), BoolType())).typeCheck() shouldEqual Func(
      Func(IntType(), BoolType()),
      Func(IntType(), BoolType())
    )

    Apply(ApplyType(recIdentityFunction, IntType()), Num(1)).typeCheck() shouldEqual IntType()
    Apply(ApplyType(recIdentityFunction, IntType()), Num(1)).eval() shouldEqual NumV(1)
  }

  testExpression(
    "Poly",
    Table(
      testExpressionTableHeading,
      (Poly("A", Num(1)), PolyV(TypeVar("A"), Num(1), Env()), PolyType(TypeVar("A"), IntType())),
      (
        Poly("T", Lambda("x", TypeVar("T"), Var("x"))),
        PolyV(TypeVar("T"), Lambda("x", TypeVar("T"), Var("x")), Env()),
        PolyType(TypeVar("T"), Func(TypeVar("T"), TypeVar("T")))
      )
    )
  )

  testExpression(
    "ApplyType",
    Table(
      testExpressionTableHeading,
      (ApplyType(Poly("B", Num(1)), IntType()), NumV(1), IntType()),
      (ApplyType(Poly("foo", Num(1)), BoolType()), NumV(1), IntType()),
      (
        ApplyType(Poly("T", Lambda("x", TypeVar("T"), Var("x"))), IntType()),
        LambdaV("x", IntType(), Var("x"), Env("T" -> TypeValueContainer(IntType()))),
        Func(IntType(), IntType())
      ),
      (
        ApplyType(Poly("T", Lambda("x", TypeVar("T"), Var("x"))), BoolType()),
        LambdaV("x", BoolType(), Var("x"), Env("T" -> TypeValueContainer(BoolType()))),
        Func(BoolType(), BoolType())
      ),
      (
        ApplyType(
          IfThenElse(
            Bool(true),
            Poly("T", Lambda("x", Func(BoolType(), TypeVar("T")), Apply(Var("x"), Bool(false)))),
            Poly("T", Lambda("x", Func(BoolType(), TypeVar("T")), Apply(Var("x"), Bool(true))))
          ),
          IntType()
        ),
        LambdaV("x", Func(BoolType(), IntType()), Apply(Var("x"), Bool(false)), Env("T" -> TypeValueContainer(IntType()))),
        Func(Func(BoolType(), IntType()), IntType())
      )
    )
  )

  testExpression(
    "Nested Poly",
    Table(
      testExpressionTableHeading,
      (
        Poly(
          "X",
          Poly("Y", Lambda("f", Func(TypeVar("X"), TypeVar("Y")), Lambda("x", TypeVar("X"), Apply(Var("f"), Var("x")))))
        ),
        PolyV(
          TypeVar("X"),
          Poly(
            "Y",
            Lambda("f", Func(TypeVar("X"), TypeVar("Y")), Lambda("x", TypeVar("X"), Apply(Var("f"), Var("x"))))
          ),
          Env()
        ),
        PolyType(
          TypeVar("X"),
          PolyType(TypeVar("Y"), Func(Func(TypeVar("X"), TypeVar("Y")), Func(TypeVar("X"), TypeVar("Y"))))
        )
      )
    )
  )

  testExpression(
    "Polymorphic function application",
    Table(
      testExpressionTableHeading,
      (
        Apply(
          Apply(
            ApplyType(
              Poly(
                "X",
                Lambda("x", TypeVar("X"), Lambda("f", Func(TypeVar("X"), IntType()), Apply(Var("f"), Var("x"))))
              ),
              BoolType()
            ),
            Bool(true)
          ),
          Lambda("x", BoolType(), IfThenElse(Var("x"), Num(1), Num(0)))
        ),
        NumV(1),
        IntType()
      )
    )
  )

  testExpression(
    "Var using a TypeVar results in an error",
    Table(
      testExpressionTableHeading,
      (
        ApplyType(Poly("T", Var("T")), IntType()),
        VariableOnlyEvalError(Literal.fromString("T")),
        VariableOnlyTypeError(Literal.fromString("T"))
      )
    )
  )

  property("Lambda using TypeVar is handled correctly") {
    Lambda("x", TypeVar("X"), Var("x")).typeCheck(Env("X" -> TypeContainer(TypeVar("X")))) shouldEqual
      Func(TypeVar("X"), TypeVar("X"))
    Lambda("x", TypeVar("X"), Var("x")).eval(Env("X" -> TypeValueContainer(TypeVar("X")))) shouldEqual
      LambdaV("x", TypeVar("X"), Var("x"), Env("X" -> TypeValueContainer(TypeVar("X"))))

    val env: ValueEnv = Env("X" -> TypeValueContainer(TypeVar("Y")), "Y" -> TypeValueContainer(TypeVar("X")))
    Lambda("x", TypeVar("X"), Var("x")).eval(env) shouldEqual
      LambdaV("x", TypeVar("Y"), Var("x"), env)

    Lambda("x", TypeVar("X"), Var("x")).typeCheck(Env("X" -> TypeContainer(IntType()))) shouldEqual
      Func(IntType(), IntType())
    Lambda("x", TypeVar("X"), Var("x")).eval(Env("X" -> TypeValueContainer(IntType()))) shouldEqual
      LambdaV("x", IntType(), Var("x"), Env("X" -> TypeValueContainer(IntType())))
  }

  property("CreatePolyFunctionTask is checked correctly") {
    CreatePolyFunctionTask.checkFulfilled(Poly("T", Lambda("x", TypeVar("T"), Var("x")))) shouldBe true
    CreatePolyFunctionTask.checkFulfilled(Poly("T", Lambda("x", TypeVar("T"), IfThenElse(Var("x"), BlankExprDropDown(), BlankExprDropDown())))) shouldBe false
  }
}
