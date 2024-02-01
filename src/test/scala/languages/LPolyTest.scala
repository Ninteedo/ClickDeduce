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
      .eval() shouldEqual LambdaV("x", IntType(), Var("x"), Map("T" -> TypeVarV(literalT, IntType())))
    ApplyType(identityFunction, Func(IntType(), BoolType())).eval() shouldEqual LambdaV(
      "x",
      Func(IntType(), BoolType()),
      Var("x"),
      Map("T" -> TypeVarV(literalT, Func(IntType(), BoolType())))
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
      (Poly("A", Num(1)), PolyV(TypeVar("A"), Num(1), Map()), PolyType(TypeVar("A"), IntType())),
      (
        Poly("T", Lambda("x", TypeVar("T"), Var("x"))),
        PolyV(TypeVar("T"), Lambda("x", TypeVar("T"), Var("x")), Map()),
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
        LambdaV("x", IntType(), Var("x"), Map("T" -> TypeVarV("T", IntType()))),
        Func(IntType(), IntType())
      ),
      (
        ApplyType(Poly("T", Lambda("x", TypeVar("T"), Var("x"))), BoolType()),
        LambdaV("x", BoolType(), Var("x"), Map("T" -> TypeVarV("T", BoolType()))),
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
        LambdaV("x", Func(BoolType(), IntType()), Apply(Var("x"), Bool(false)), Map("T" -> TypeVarV("T", IntType()))),
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
          Map()
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
}