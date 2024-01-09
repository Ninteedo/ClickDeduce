package languages

import languages.LRec.*
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.TimeLimits.failAfter
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.{a, an, shouldBe, shouldEqual}
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatest.propspec.AnyPropSpec
import org.scalatest.time.{Millis, Span}

import scala.collection.immutable.Map
import scala.util.Random

class LRecTest extends AnyPropSpec with TableDrivenPropertyChecks with GivenWhenThen {
  val factorialFunction: Expr = Rec("factorial", "n", IntType(), IntType(),
    IfThenElse(Eq(Var("n"), Num(0)), Num(1), Times(Var("n"), Apply(Var("factorial"), Plus(Var("n"), Num(-1))))))

  val nestedOverridingRecFunction1: Expr = Rec("f", "x", IntType(), IntType(),
    Apply(Rec("f", "x", IntType(), IntType(), Num(1)), Var("x")))

  property("Rec type-checks correctly") {
    Rec("f", "x", IntType(), IntType(), Num(1)).typeCheck(Map()) shouldEqual Func(IntType(), IntType())
    Rec("f", "x", IntType(), IntType(), Num(1)).typeCheck(Map("f" -> IntType(), "x" -> IntType())) shouldEqual Func(IntType(), IntType())
    Rec("f", "x", IntType(), Func(IntType(), BoolType()), Lambda("y", IntType(), Eq(Var("y"), Num(0)))).typeCheck(Map("f" -> IntType(), "x" -> BoolType())) shouldEqual Func(IntType(), Func(IntType(), BoolType()))
    factorialFunction.typeCheck(Map()) shouldEqual Func(IntType(), IntType())
    nestedOverridingRecFunction1.typeCheck(Map()) shouldEqual Func(IntType(), IntType())
  }

  property("Applying with Rec type-checks correctly") {
    Apply(factorialFunction, Num(5)).typeCheck(Map()) shouldEqual IntType()
    Apply(factorialFunction, Num(5)).typeCheck(Map("factorial" -> Func(IntType(), IntType()), "n" -> IntType())) shouldEqual IntType()
    Apply(Rec("f", "x", IntType(), Func(IntType(), BoolType()), Lambda("y", IntType(), Eq(Var("y"), Num(0)))), Num(5)).typeCheck(Map()) shouldEqual Func(IntType(), BoolType())
    Apply(nestedOverridingRecFunction1, Num(5)).typeCheck(Map()) shouldEqual IntType()
  }

  property("Applying with Rec evaluates correctly") {
    Apply(factorialFunction, Num(5)).eval(Map()) shouldEqual NumV(120)
    Apply(factorialFunction, Num(5)).eval(Map("factorial" -> factorialFunction.eval(Map()), "n" -> NumV(6))) shouldEqual NumV(120)

    val recWithLambda = Rec("f", "x", IntType(), Func(IntType(), BoolType()), Lambda("y", IntType(), Eq(Var("y"), Num(0))))
    Apply(recWithLambda, Num(5)).eval(Map()) shouldEqual
      LambdaV("y", IntType(), Eq(Var("y"), Num(0)), Map("f" -> recWithLambda.eval(Map()), "x" -> NumV(5)))
  }

  property("Applying with Rec correctly shows a phantom tree") {
    val factorialExpr = Apply(factorialFunction, Num(3))
    val factorialTree = VariableNode.fromExpr(factorialExpr)

    val children = factorialTree.getVisibleChildren(DisplayMode.Evaluation)
    children.length shouldEqual 3

    children.head shouldBe a[VariableNode]
    children.head.asInstanceOf[VariableNode].exprName shouldEqual "Rec"

    children(1) shouldEqual VariableNode("Num", List(LiteralNode("3")))

    children(2) shouldBe a[VariableNode]
    val phantomNode = children(2).asInstanceOf[VariableNode]
    phantomNode.isPhantom shouldEqual true
    phantomNode.getEnv shouldEqual Map("factorial" -> factorialFunction.eval(Map()), "n" -> NumV(3))
    phantomNode.getValue shouldEqual NumV(6)
  }

  property("Infinite recursion results in a stack overflow error") {
    val infiniteRec = Rec("f", "x", IntType(), IntType(), Apply(Var("f"), Var("x")))
    failAfter(Span(1000, Millis)) {
      Apply(infiniteRec, Num(1)).eval(Map()) shouldBe a[EvalException]
      Apply(infiniteRec, Num(1)).eval(Map()) match {
        case EvalException(exception) => exception shouldBe a[StackOverflowError]
      }
    }
  }
}
