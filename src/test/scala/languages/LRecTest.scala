package languages

import languages.LRec.*
import org.scalatest.concurrent.TimeLimits.failAfter
import org.scalatest.matchers.must.Matchers.noException
import org.scalatest.matchers.should.Matchers.{a, an, be, shouldBe, shouldEqual}
import org.scalatest.prop.TableFor1
import org.scalatest.time.{Millis, Span}

import scala.collection.immutable.Map

class LRecTest extends TestTemplate[Expr, Value, Type] {
  val factorialFunction: Expr = Rec(
    "factorial",
    "n",
    IntType(),
    IntType(),
    IfThenElse(Eq(Var("n"), Num(0)), Num(1), Times(Var("n"), Apply(Var("factorial"), Plus(Var("n"), Num(-1)))))
  )

  val nestedOverridingRecFunction1: Expr =
    Rec("f", "x", IntType(), IntType(), Apply(Rec("f", "x", IntType(), IntType(), Num(1)), Var("x")))

  property("Rec type-checks correctly") {
    Rec("f", "x", IntType(), IntType(), Num(1)).typeCheck(Map()) shouldEqual Func(IntType(), IntType())
    Rec("f", "x", IntType(), IntType(), Num(1)).typeCheck(Map("f" -> IntType(), "x" -> IntType())) shouldEqual Func(
      IntType(),
      IntType()
    )
    Rec("f", "x", IntType(), Func(IntType(), BoolType()), Lambda("y", IntType(), Eq(Var("y"), Num(0))))
      .typeCheck(Map("f" -> IntType(), "x" -> BoolType())) shouldEqual Func(IntType(), Func(IntType(), BoolType()))
    factorialFunction.typeCheck(Map()) shouldEqual Func(IntType(), IntType())
    nestedOverridingRecFunction1.typeCheck(Map()) shouldEqual Func(IntType(), IntType())
  }

  property("Applying with Rec type-checks correctly") {
    Apply(factorialFunction, Num(5)).typeCheck(Map()) shouldEqual IntType()
    Apply(factorialFunction, Num(5))
      .typeCheck(Map("factorial" -> Func(IntType(), IntType()), "n" -> IntType())) shouldEqual IntType()
    Apply(Rec("f", "x", IntType(), Func(IntType(), BoolType()), Lambda("y", IntType(), Eq(Var("y"), Num(0)))), Num(5))
      .typeCheck(Map()) shouldEqual Func(IntType(), BoolType())
    Apply(nestedOverridingRecFunction1, Num(5)).typeCheck(Map()) shouldEqual IntType()
  }

  property("Applying with Rec evaluates correctly") {
    Apply(factorialFunction, Num(5)).eval(Map()) shouldEqual NumV(120)
    Apply(factorialFunction, Num(5))
      .eval(Map("factorial" -> factorialFunction.eval(Map()), "n" -> NumV(6))) shouldEqual NumV(120)

    val recWithLambda =
      Rec("f", "x", IntType(), Func(IntType(), BoolType()), Lambda("y", IntType(), Eq(Var("y"), Num(0))))
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
    phantomNode.getEvalEnv shouldEqual Map("factorial" -> factorialFunction.eval(Map()), "n" -> NumV(3))
    phantomNode.getValue shouldEqual NumV(6)
  }

  property("Infinite recursion results in a stack overflow error") {
    val infiniteRec = Rec("f", "x", IntType(), IntType(), Apply(Var("f"), Var("x")))
    failAfter(Span(1000, Millis)) {
      Apply(infiniteRec, Num(1)).eval(Map()) shouldBe a[EvalException]
      Apply(infiniteRec, Num(1)).eval(Map()) match {
        case EvalException(message) => message shouldEqual "Stack overflow"
      }
    }
  }

  property("Infinite recursion in nodes results in a DepthLimitExceededException in evaluation mode") {
    val node = VariableNode(
      "Apply",
      List(
        SubExprNode(
          VariableNode(
            "Rec",
            List(
              LiteralNode("f"),
              LiteralNode("x"),
              SubTypeNode(TypeNode("IntType", Nil)),
              SubTypeNode(TypeNode("IntType", Nil)),
              SubExprNode(
                VariableNode(
                  "Apply",
                  List(
                    SubExprNode(VariableNode("Var", List(LiteralNode("f")))),
                    SubExprNode(VariableNode("Var", List(LiteralNode("x"))))
                  )
                )
              )
            )
          )
        ),
        SubExprNode(VariableNode("Num", List(LiteralNode("1"))))
      )
    )

    failAfter(Span(500, Millis)) {
      noException should be thrownBy node.toHtml(DisplayMode.Edit)
    }

    failAfter(Span(500, Millis)) {
      noException should be thrownBy node.toHtml(DisplayMode.TypeCheck)
    }

    failAfter(Span(500, Millis)) {
      a[DepthLimitExceededException] should be thrownBy node.toHtml(DisplayMode.Evaluation)
    }
  }

  property("Rec returns an error when the function or parameter names are not valid identifiers") {
    val expressions: TableFor1[Expr] = Table(
      "expr",
      Rec(LiteralInt(6), LiteralIdentifier("x"), IntType(), IntType(), Num(1)),
      Rec(LiteralIdentifier("f"), LiteralInt(-71), IntType(), IntType(), Num(1)),
      Rec(LiteralBool(true), LiteralIdentifier("y"), IntType(), IntType(), Num(1)),
      Rec(LiteralIdentifier("g"), LiteralBool(false), IntType(), IntType(), Num(1)),
      Rec(LiteralBool(true), LiteralBool(false), IntType(), IntType(), Num(1)),
      Rec(LiteralString("foo"), LiteralIdentifier("z"), IntType(), IntType(), Num(1)),
      Rec(LiteralIdentifier("h"), LiteralString("bar"), IntType(), IntType(), Num(1)),
      Rec(LiteralString("foo"), LiteralString("bar"), IntType(), IntType(), Num(1)),
      Rec(" x", "y", IntType(), IntType(), Num(1)),
      Rec("1foo", "bar", IntType(), IntType(), Num(1)),
    )

    forAll(expressions) { expr =>
      expr.typeCheck() shouldBe an[InvalidIdentifierTypeError]
      expr.eval() shouldBe an[InvalidIdentifierEvalError]
    }

    forAll(Table("identifier", invalidVariableNames: _*)) { v =>
      Rec(v, "x", IntType(), IntType(), Num(1)).typeCheck() shouldBe an[InvalidIdentifierTypeError]
      Rec("f", v, IntType(), IntType(), Num(1)).typeCheck() shouldBe an[InvalidIdentifierTypeError]
      Rec(v, v, IntType(), IntType(), Num(1)).typeCheck() shouldBe an[InvalidIdentifierTypeError]

      Rec(v, "x", IntType(), IntType(), Num(1)).eval() shouldBe an[InvalidIdentifierEvalError]
      Rec("f", v, IntType(), IntType(), Num(1)).eval() shouldBe an[InvalidIdentifierEvalError]
      Rec(v, v, IntType(), IntType(), Num(1)).eval() shouldBe an[InvalidIdentifierEvalError]
    }
  }

  property("Rec pretty prints correctly") {
    val factorialPretty = "rec factorial(n: Int): Int. (if (n == 0) then 1 else (n × ((factorial) (n + -1))))"

    prettyPrint(factorialFunction) shouldEqual factorialPretty

    prettyPrint(factorialFunction.eval()) shouldEqual factorialPretty
  }
}
