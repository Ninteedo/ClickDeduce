package languages

import convertors.{DisplayMode, HTMLConvertor}
import languages.LRec.*
import org.scalatest.concurrent.TimeLimits.failAfter
import org.scalatest.matchers.must.Matchers.noException
import org.scalatest.matchers.should.Matchers.{a, an, be, shouldBe, shouldEqual}
import org.scalatest.prop.TableFor1
import org.scalatest.time.{Millis, Span}

class LRecTest extends TestTemplate[Expr, Value, Type] {
  val factorialFunction: Rec = Rec(
    "factorial",
    "n",
    IntType(),
    IntType(),
    IfThenElse(Equal(Var("n"), Num(0)), Num(1), Times(Var("n"), Apply(Var("factorial"), Plus(Var("n"), Num(-1)))))
  )

  val nestedOverridingRecFunction1: Rec =
    Rec("f", "x", IntType(), IntType(), Apply(Rec("f", "x", IntType(), IntType(), Num(1)), Var("x")))

  property("Rec type-checks correctly") {
    Rec("f", "x", IntType(), IntType(), Num(1)).typeCheck() shouldEqual Func(IntType(), IntType())
    Rec("f", "x", IntType(), IntType(), Num(1)).typeCheck(Env("f" -> IntType(), "x" -> IntType())) shouldEqual Func(
      IntType(),
      IntType()
    )
    Rec("f", "x", IntType(), Func(IntType(), BoolType()), Lambda("y", IntType(), Equal(Var("y"), Num(0))))
      .typeCheck(Env("f" -> IntType(), "x" -> BoolType())) shouldEqual Func(IntType(), Func(IntType(), BoolType()))
    factorialFunction.typeCheck() shouldEqual Func(IntType(), IntType())
    nestedOverridingRecFunction1.typeCheck() shouldEqual Func(IntType(), IntType())
  }

  property("Rec evaluates correctly") {
    Rec("f", "x", IntType(), IntType(), Num(1)).eval() shouldEqual RecV(
      LiteralIdentifierBind("f"),
      LiteralIdentifierBind("x"),
      IntType(),
      IntType(),
      Num(1),
      Env()
    )
    Rec("f", "x", IntType(), IntType(), Num(1))
      .eval(Env("f" -> NumV(6), "x" -> BoolV(false))) shouldEqual RecV(
      LiteralIdentifierBind("f"),
      LiteralIdentifierBind("x"),
      IntType(),
      IntType(),
      Num(1),
      Env("f" -> NumV(6), "x" -> BoolV(false))
    )
    Rec("f", "x", IntType(), Func(IntType(), BoolType()), Lambda("y", IntType(), Equal(Var("y"), Num(0))))
      .eval(Env("f" -> NumV(-657), "x" -> BoolV(true))) shouldEqual
      RecV(
        LiteralIdentifierBind("f"),
        LiteralIdentifierBind("x"),
        IntType(),
        Func(IntType(), BoolType()),
        Lambda("y", IntType(), Equal(Var("y"), Num(0))),
        Env("f" -> NumV(-657), "x" -> BoolV(true))
      )
    factorialFunction.eval() shouldEqual RecV(
      factorialFunction.f,
      factorialFunction.v,
      factorialFunction.inType,
      factorialFunction.outType,
      factorialFunction.e,
      Env()
    )
    nestedOverridingRecFunction1.eval() shouldEqual RecV(
      nestedOverridingRecFunction1.f,
      nestedOverridingRecFunction1.v,
      nestedOverridingRecFunction1.inType,
      nestedOverridingRecFunction1.outType,
      nestedOverridingRecFunction1.e,
      Env()
    )
  }

  property("Rec verifies that its expression matches the reported type") {
    val fakeFactorialFunction = Rec("factorial", "n", IntType(), IntType(), Bool(true))
    val fakeFactorialFunction2 = Rec(
      "factorial",
      "n",
      IntType(),
      IntType(),
      Apply(Lambda("f", Func(IntType(), IntType()), Var("f")), Var("factorial"))
    )

    fakeFactorialFunction.typeCheck() shouldBe a[RecursiveFunctionExpressionOutTypeMismatch]
    fakeFactorialFunction2.typeCheck() shouldBe a[RecursiveFunctionExpressionOutTypeMismatch]
  }

  property("Applying with Rec type-checks correctly") {
    Apply(factorialFunction, Num(5)).typeCheck() shouldEqual IntType()
    Apply(factorialFunction, Num(5))
      .typeCheck(Env("factorial" -> Func(IntType(), IntType()), "n" -> IntType())) shouldEqual IntType()
    Apply(Rec("f", "x", IntType(), Func(IntType(), BoolType()), Lambda("y", IntType(), Equal(Var("y"), Num(0)))), Num(5))
      .typeCheck() shouldEqual Func(IntType(), BoolType())
    Apply(nestedOverridingRecFunction1, Num(5)).typeCheck() shouldEqual IntType()
  }

  property("Applying with Rec evaluates correctly") {
    Apply(factorialFunction, Num(5)).eval() shouldEqual NumV(120)
    Apply(factorialFunction, Num(5))
      .eval(Env("factorial" -> factorialFunction.eval(), "n" -> NumV(6))) shouldEqual NumV(120)

    val recWithLambda =
      Rec("f", "x", IntType(), Func(IntType(), BoolType()), Lambda("y", IntType(), Equal(Var("y"), Num(0))))
    Apply(recWithLambda, Num(5)).eval() shouldEqual
      LambdaV("y", IntType(), Equal(Var("y"), Num(0)), Env("f" -> recWithLambda.eval(), "x" -> NumV(5)))
  }

  property("Applying with Rec correctly shows a phantom tree") {
    val factorialExpr = Apply(factorialFunction, Num(3))
    val factorialTree = VariableNode.fromExpr(factorialExpr)

    val children = factorialTree.getVisibleChildren(DisplayMode.Evaluation)
    children.length shouldEqual 3

    children.head shouldBe a[VariableNode]
    children.head.asInstanceOf[VariableNode].exprName shouldEqual "Rec"

    children(1) shouldEqual VariableNode("Num", List(LiteralNode(LiteralInt(3))))

    children(2) shouldBe a[VariableNode]
    val phantomNode = children(2).asInstanceOf[VariableNode]
    phantomNode.isPhantom shouldEqual true
    phantomNode.getEvalEnv shouldEqual Env("factorial" -> factorialFunction.eval(), "n" -> NumV(3))
    phantomNode.getValue shouldEqual NumV(6)
  }

  property("Infinite recursion results in a stack overflow error") {
    val infiniteRec = Rec("f", "x", IntType(), IntType(), Apply(Var("f"), Var("x")))
    failAfter(Span(1000, Millis)) {
      Apply(infiniteRec, Num(1)).eval() shouldBe an[EvalException]
      Apply(infiniteRec, Num(1)).eval() match {
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
              LiteralNode(LiteralIdentifierBind("f")),
              LiteralNode(LiteralIdentifierBind("x")),
              SubTypeNode(TypeNode("IntType", Nil)),
              SubTypeNode(TypeNode("IntType", Nil)),
              SubExprNode(
                VariableNode(
                  "Apply",
                  List(
                    SubExprNode(VariableNode("Var", List(LiteralNode(LiteralIdentifierLookup("f"))))),
                    SubExprNode(VariableNode("Var", List(LiteralNode(LiteralIdentifierLookup("x")))))
                  )
                )
              )
            )
          )
        ),
        SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(1)))))
      )
    )

    failAfter(Span(500, Millis)) {
      noException should be thrownBy HTMLConvertor(LRec, DisplayMode.Edit).convert(node)
    }

    failAfter(Span(500, Millis)) {
      noException should be thrownBy HTMLConvertor(LRec, DisplayMode.TypeCheck).convert(node)
    }

    failAfter(Span(500, Millis)) {
      a[DepthLimitExceededException] should be thrownBy HTMLConvertor(LRec, DisplayMode.Evaluation).convert(node)
    }
  }

  property("Rec returns an error when the function or parameter names are not valid identifiers") {
    val expressions: TableFor1[Expr] = Table(
      "expr",
      Rec(LiteralIdentifierBind("6"), LiteralIdentifierBind("x"), IntType(), IntType(), Num(1)),
      Rec(LiteralIdentifierBind("f"), LiteralIdentifierBind("-71"), IntType(), IntType(), Num(1)),
//      Rec(LiteralIdentifier("true"), LiteralIdentifier("y"), IntType(), IntType(), Num(1)),
//      Rec(LiteralIdentifier("g"), LiteralIdentifier("false"), IntType(), IntType(), Num(1)),
//      Rec(LiteralIdentifier("true"), LiteralIdentifier("false"), IntType(), IntType(), Num(1)),
      Rec(LiteralIdentifierBind("foo"), LiteralIdentifierBind("\"z\""), IntType(), IntType(), Num(1)),
      Rec(LiteralIdentifierBind("h"), LiteralIdentifierBind("\"bar\""), IntType(), IntType(), Num(1)),
      Rec(LiteralIdentifierBind("\"foo\""), LiteralIdentifierBind("bar"), IntType(), IntType(), Num(1)),
      Rec(" x", "y", IntType(), IntType(), Num(1)),
      Rec("1foo", "bar", IntType(), IntType(), Num(1))
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
    val factorialPretty = "rec factorial(n)"

    factorialFunction.prettyPrint shouldEqual factorialPretty

    factorialFunction.eval().prettyPrint shouldEqual "rec factorial(n)"
  }
}
