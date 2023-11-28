package languages

import languages.LLet.*
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers.{an, shouldBe, shouldEqual}
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatest.propspec.AnyPropSpec

import scala.util.Random

class LLetTest extends AnyPropSpec with TableDrivenPropertyChecks with GivenWhenThen {
  Random.setSeed(2025)

  val intValues: List[BigInt] = List(0, 1, -1, 2, -2, 5, -5, 10, 100, -100, 198765, -157396, 5168765, -4376418)
  val nums: TableFor1[BigInt] = Table("num", intValues: _*)

  val bools: TableFor1[Boolean] = Table("bool", true, false)

  val assortedValues: TableFor1[Value] = Table("value", intValues.map(NumV.apply) ++ bools.map(BoolV.apply): _*)

  val variableNames: List[Variable] = List("a", "b", "x", "y", "foo", "bar", "gha867", "p1f")

  def randomElement[A](l: List[A]): A = {
    l(Random.nextInt(l.length))
  }

  property("Var correctly returns a value from the environment during evaluation") {
    // environment with only correct variable
    forAll(assortedValues) {
      value => {
        val v = randomElement(variableNames)
        eval(Var(v), Map(v -> value)) shouldEqual value
      }
    }

    // big environment with lots of other variables
    forAll(assortedValues) {
      value => {
        var env: Env = Map()
        for (i <- 0 until Math.min(assortedValues.length, variableNames.length)) {
          val v = randomElement(variableNames)
          val value = randomElement(assortedValues.toList)
          env += v -> value
        }
        val v: Variable = randomElement(env.keys.toList)
        eval(Var(v), env) shouldEqual env(v)
      }
    }
  }

  property("Var correctly type-checks") {
    // environment with only correct variable
    forAll(assortedValues) {
      value => {
        val v = randomElement(variableNames)
        typeOf(Var(v), Map(v -> value.typ)) shouldEqual value.typ
      }
    }

    // big environment with lots of other variables
    forAll(assortedValues) {
      value => {
        var env: TypeEnv = Map()
        for (i <- 0 until Math.min(assortedValues.length, variableNames.length)) {
          val v = randomElement(variableNames)
          val value = randomElement(assortedValues.toList)
          env += v -> value.typ
        }
        val v: Variable = randomElement(env.keys.toList)
        typeOf(Var(v), env) shouldEqual env(v)
      }
    }
  }

  property("Let correctly type-checks") {
    typeOf(Let("y", Num(51), Var("y"))) shouldEqual IntType()
    typeOf(Let("gri3hga3", Bool(true), IfThenElse(Var("gri3hga3"), Num(1), Num(2)))) shouldEqual IntType()
    typeOf(Eq(Num(0), Let("abc", Num(3), Plus(Num(-3), Var("abc"))))) shouldEqual BoolType()
  }

  property("Let correctly evaluates with single Let in expression") {
    eval(Let("x", Num(2), Var("x"))) shouldEqual NumV(2)
    eval(Let("gh2", Bool(false), Eq(Var("gh2"), Bool(false)))) shouldEqual BoolV(true)
    eval(Let("iou", Plus(Num(1), Num(5)), Times(Var("iou"), Var("iou")))) shouldEqual NumV(36)
  }

  property("Let correctly evaluates with multiple Let expressions in single expression") {
    eval(
      Let(
        "x",
        Num(43),
        Let(
          "y",
          Num(12),
          Plus(Var("x"), Var("y"))
        )
      )
    ) shouldEqual NumV(55)

    eval(
      Let(
        "hello",
        Plus(
          Let(
            "world",
            Num(2),
            Times(Var("world"), Num(-1))
          ),
          Num(6)
        ),
        Eq(Num(4), Var("hello"))
      )
    ) shouldEqual BoolV(true)

    eval(
      Let(
        "x",
        Num(1),
        Let(
          "y",
          Num(2),
          Let(
            "z",
            Num(3),
            Plus(Plus(Var("z"), Var("y")), Var("x"))
          )
        )
      )
    ) shouldEqual NumV(6)

    eval(
      Plus(
        Let("x", Num(20), Plus(Var("x"), Num(1))),
        Let("x", Num(34), Times(Var("x"), Num(-1)))
      )
    ) shouldEqual NumV(-13)
  }

  property("Var results an error when variable not found") {
    eval(Var("x"), Map("y" -> NumV(4), "xx" -> NumV(1), "w" -> BoolV(true))) shouldBe an[EvalError]
    typeOf(Var("x"), Map("y" -> IntType(), "xx" -> IntType(), "w" -> BoolType())) shouldBe an[TypeError]

    eval(
      Plus(
        Var("foo"),
        Let("foo", Num(1), Var("foo"))
      )
    ) shouldBe an[EvalError]
  }
}
