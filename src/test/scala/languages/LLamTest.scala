package languages

import languages.LLam.*
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers.{an, shouldBe, shouldEqual}
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatest.propspec.AnyPropSpec

import scala.collection.immutable.Map
import scala.util.Random

class LLamTest extends AnyPropSpec with TableDrivenPropertyChecks with GivenWhenThen {
  Random.setSeed(2026)

  val incrementFunction: Lambda = Lambda("x", IntType(), Plus(Var("x"), Num(1)))
  val twiceFunction: Lambda = Lambda(
    "f", Func(IntType(), IntType()), Lambda("x", IntType(), Apply(Var("f"), Apply(Var("f"), Var("x"))))
  )

  property("Lambda correctly type-checks") {
    typeOf(incrementFunction) shouldEqual Func(IntType(), IntType())

    val exampleEnv: TypeEnv = Map("y" -> BoolType())
    typeOf(incrementFunction, exampleEnv) shouldEqual Func(IntType(), IntType())
  }

  property("Lambda correctly evaluates") {
    eval(incrementFunction) shouldEqual LambdaV("x", IntType(), Plus(Var("x"), Num(1)), Map())

    val exampleEnv: Env = Map("y" -> NumV(76))
    eval(incrementFunction, exampleEnv) shouldEqual LambdaV("x", IntType(), Plus(Var("x"), Num(1)), exampleEnv)
  }

  property("Apply correctly type-checks") {
    typeOf(Apply(incrementFunction, Num(24))) shouldEqual IntType()
    typeOf(Apply(incrementFunction, Num(24)), Map("x" -> BoolType(), "y" -> IntType())) shouldEqual IntType()

    typeOf(Apply(Lambda("a", BoolType(), IfThenElse(Var("a"), Num(5), Num(10))), Bool(true))) shouldEqual IntType()
    typeOf(Apply(IfThenElse(Bool(false), incrementFunction, Lambda("b", IntType(), Times(Var("b"), Num(-1)))), Num(-3))
    ) shouldEqual IntType()
  }

  property("Apply correctly evaluates") {
    eval(Apply(incrementFunction, Num(24))) shouldEqual NumV(25)
    eval(Apply(incrementFunction, Num(78)), Map("x" -> NumV(2))) shouldEqual NumV(79)

    eval(Apply(IfThenElse(Bool(false), incrementFunction, Lambda("b", IntType(), Times(Var("b"), Num(-1)))), Num(-3))
    ) shouldEqual NumV(3)

    eval(Apply(Apply(twiceFunction, incrementFunction), Num(4))) shouldEqual NumV(6)
  }
}
