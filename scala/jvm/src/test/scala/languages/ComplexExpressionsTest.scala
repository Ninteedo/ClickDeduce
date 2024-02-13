package languages

import languages.LRec.*
import org.scalatest.matchers.should.Matchers.shouldBe

class ComplexExpressionsTest extends TestTemplate[LRec#Expr, LRec#Value, LRec#Type] {
  val ifNotEqualToZeroThenFunction: Expr = Lambda(
    "f",
    Func(IntType(), IntType()),
    Lambda("x", IntType(), IfThenElse(Equal(Var("x"), Num(0)), Num(1), Apply(Var("f"), Var("x"))))
  )

  val doubleFunction: Expr = Lambda("x", IntType(), Times(Var("x"), Num(2)))

  val factorialFunction: Expr = Rec(
    "f",
    "x",
    IntType(),
    IntType(),
    Apply(
      Apply(
        ifNotEqualToZeroThenFunction,
        Lambda("x", IntType(), Times(Var("x"), Apply(Var("f"), Plus(Var("x"), Num(-1)))))
      ),
      Var("x")
    )
  )

  property("functions with same argument name don't conflict due to scope") {
    val doubleIfNotZeroFunction = Apply(ifNotEqualToZeroThenFunction, doubleFunction)
    doubleIfNotZeroFunction.typeCheck() shouldBe Func(IntType(), IntType())
    Apply(doubleIfNotZeroFunction, Num(6)).typeCheck() shouldBe IntType()
    Apply(doubleIfNotZeroFunction, Num(6)).eval() shouldBe NumV(12)

    Apply(doubleIfNotZeroFunction, Num(-58)).typeCheck(Env("x" -> BoolType())) shouldBe IntType()
    Apply(doubleIfNotZeroFunction, Num(0)).eval(Env("x" -> BoolV(true))) shouldBe NumV(1)

    factorialFunction.typeCheck() shouldBe Func(IntType(), IntType())
    Apply(factorialFunction, Num(5878578)).typeCheck() shouldBe IntType()
    Apply(factorialFunction, Num(5)).eval() shouldBe NumV(120)
  }
}
