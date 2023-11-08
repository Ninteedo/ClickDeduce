package languages

import languages.LIf.*
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1, TableFor3}
import org.scalatest.propspec.AnyPropSpec

import scala.util.Random

class LIfTest extends AnyPropSpec with TableDrivenPropertyChecks with GivenWhenThen {
  Random.setSeed(2024)

  def genRandBool(): Boolean = Random.nextBoolean()

  def genRandInt(): BigInt = Random.nextInt(200) - 100

  val bools: TableFor1[Boolean] = Table("bool", true, false)

  property("Bool type-checks to BoolType") {
    forAll(bools) { b =>
      typeOf(Bool(b)) shouldEqual BoolType()
    }
  }

  property("Bool correctly evaluates to BoolV") {
    forAll(bools) { b =>
      eval(Bool(b)) shouldEqual BoolV(b)
    }
  }

  property("BoolV's type is BoolType") {
    forAll(bools) { b =>
      BoolV(b).typ shouldEqual BoolType()
    }
  }

  property("IfThenElse returns then_expr when cond is true") {
    eval(IfThenElse(Bool(true), Num(1), Num(2))) shouldEqual NumV(1)
    eval(IfThenElse(Bool(true), Bool(true), Bool(false))) shouldEqual BoolV(true)
  }

  property("IfThenElse returns else_expr when cond is false") {
    eval(IfThenElse(Bool(false), Num(1), Num(2))) shouldEqual NumV(2)
    eval(IfThenElse(Bool(false), Bool(true), Bool(false))) shouldEqual BoolV(false)
  }

  property("IfThenElse correctly type-checks when both branches have the same type") {
    typeOf(IfThenElse(Bool(true), Num(1), Num(2))) shouldEqual IntType()
    typeOf(IfThenElse(Bool(true), Bool(true), Bool(false))) shouldEqual BoolType()
  }

  property("IfThenElse type-checks to an error when the branches have different types") {
    typeOf(IfThenElse(Bool(true), Num(1), Bool(false))) shouldBe a[TypeError]
    typeOf(IfThenElse(Bool(true), Bool(true), Num(2))) shouldBe a[TypeError]
  }

  property("Eq type-checks to BoolType when both sides have the same type") {
    typeOf(Eq(Num(1), Num(2))) shouldEqual BoolType()
    typeOf(Eq(Bool(true), Bool(false))) shouldEqual BoolType()
    typeOf(Eq(Bool(true), Bool(true))) shouldEqual BoolType()
    typeOf(Eq(Num(1), Num(1))) shouldEqual BoolType()
  }

  property("Eq type-checks to an error when the sides have different types") {
    typeOf(Eq(Num(1), Bool(false))) shouldBe a[TypeError]
    typeOf(Eq(Bool(true), Num(2))) shouldBe a[TypeError]
  }

  property("Eq correctly evaluates to BoolV") {
    eval(Eq(Num(1), Num(2))) shouldEqual BoolV(false)
    eval(Eq(Bool(true), Bool(false))) shouldEqual BoolV(false)
    eval(Eq(Bool(true), Bool(true))) shouldEqual BoolV(true)
    eval(Eq(Num(1), Num(1))) shouldEqual BoolV(true)
  }
}
