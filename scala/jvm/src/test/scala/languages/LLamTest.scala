package languages

import convertors.{DisplayMode, HTMLConvertor}
import languages.LLam.*
import org.scalatest.matchers.should.Matchers.{an, shouldBe, shouldEqual}
import org.scalatest.prop.TableFor1
import org.scalatest.propspec.AnyPropSpec

class LLamTest extends TestTemplate[Expr, Value, Type] {
  val incrementFunction: Lambda = Lambda("x", IntType(), Plus(Var("x"), Num(1)))
  val twiceFunction: Lambda =
    Lambda("f", Func(IntType(), IntType()), Lambda("x", IntType(), Apply(Var("f"), Apply(Var("f"), Var("x")))))
  val incrementTwiceFunction: Apply = Apply(twiceFunction, incrementFunction)

  testExpression(
    "Lambda",
    createExprTable(
      (incrementFunction, LambdaV("x", IntType(), Plus(Var("x"), Num(1)), Env()), Func(IntType(), IntType())),
      (
        Lambda("x", BoolType(), Equal(Var("x"), Bool(true))),
        LambdaV("x", BoolType(), Equal(Var("x"), Bool(true)), Env()),
        Func(BoolType(), BoolType())
      ),
      (
        twiceFunction,
        LambdaV("f", twiceFunction.typ, twiceFunction.e, Env()),
        Func(Func(IntType(), IntType()), Func(IntType(), IntType()))
      )
    )
  )

  property("Lambda correctly type-checks with existing environment") {
    val exampleEnv: TypeEnv = Env("x" -> BoolType(), "y" -> BoolType())
    incrementFunction.typeCheck(exampleEnv) shouldEqual Func(IntType(), IntType())
  }

  property("Lambda correctly evaluates with existing environment") {
    val exampleEnv: ValueEnv = Env("x" -> BoolV(true), "y" -> NumV(76))
    incrementFunction.eval(exampleEnv) shouldEqual LambdaV("x", IntType(), Plus(Var("x"), Num(1)), exampleEnv)
  }

  property("Apply correctly type-checks") {
    Apply(incrementFunction, Num(24)).typeCheck() shouldEqual IntType()
    Apply(incrementFunction, Num(24)).typeCheck(Env("x" -> BoolType(), "y" -> IntType())) shouldEqual IntType()

    Apply(Lambda("a", BoolType(), IfThenElse(Var("a"), Num(5), Num(10))), Bool(true))
      .typeCheck() shouldEqual IntType()
    Apply(IfThenElse(Bool(false), incrementFunction, Lambda("b", IntType(), Times(Var("b"), Num(-1)))), Num(-3))
      .typeCheck() shouldEqual IntType()
  }

  property("Apply correctly evaluates") {
    Apply(incrementFunction, Num(24)).eval() shouldEqual NumV(25)
    Apply(incrementFunction, Num(78)).eval(Env("x" -> NumV(2))) shouldEqual NumV(79)

    Apply(IfThenElse(Bool(false), incrementFunction, Lambda("b", IntType(), Times(Var("b"), Num(-1)))), Num(-3))
      .eval() shouldEqual NumV(3)

    Apply(Apply(twiceFunction, incrementFunction), Num(4)).eval() shouldEqual NumV(6)
  }

  property("Apply results in error when left side is not a function") {
    val leftExpressions: List[Expr] = List(
      Num(4),
      Bool(false),
      IfThenElse(Apply(incrementFunction, Num(4)), Num(2), Num(3)),
      Apply(incrementFunction, Num(8))
    )
    leftExpressions.foreach { l =>
      Apply(l, Num(4)).typeCheck() shouldBe an[ApplyToNonFunctionErrorType]
      Apply(l, Num(4)).eval() shouldBe an[ApplyToNonFunctionError]
    }
  }

  property("Apply results in error when the right side does not match the function input type") {
    val env = Env("bool" -> BoolType(), "int" -> IntType())
    val expressions: TableFor1[Expr] = Table(
      "expr",
      Apply(incrementFunction, Bool(true)),
      Apply(incrementFunction, Var("bool")),
      Apply(incrementFunction, incrementFunction),
      Apply(incrementFunction, Equal(Var("int"), Var("int"))),
      Apply(Lambda("x", BoolType(), Var("x")), Num(4)),
      Apply(Lambda("x", BoolType(), Var("x")), Plus(Num(1), Var("int")))
    )

    forAll(expressions) { expr =>
      expr.typeCheck(env) shouldBe an[IncompatibleTypeErrorType]
    }
  }

  property("Lambda has appropriate children expressions in type-check mode") {
    incrementFunction.getChildrenTypeCheck() shouldEqual List((incrementFunction.e, Env("x" -> IntType())))

    incrementFunction.getChildrenTypeCheck(Env("y" -> BoolType())) shouldEqual List(
      (incrementFunction.e, Env("x" -> IntType(), "y" -> BoolType()))
    )
  }

  property("Lambda node behaves appropriately with simple argument type") {
    val initialTree = VariableNode.createFromExprName("Lambda").get
    initialTree.args shouldEqual List(LiteralNode(""), SubTypeNode(TypeChoiceNode()), SubExprNode(ExprChoiceNode()))

    val argName: String = "foo"
    val editVarNameAction = EditLiteralAction(initialTree, List(0), argName)
    editVarNameAction.newTree.args shouldEqual List(
      LiteralNode(argName),
      SubTypeNode(TypeChoiceNode()),
      SubExprNode(ExprChoiceNode())
    )

    val argType: Type = IntType()
    val argTypeName: String = argType.getClass.getSimpleName
    val setArgTypeAction = SelectTypeAction(editVarNameAction.newTree, List(1), argTypeName)
    setArgTypeAction.newTree.args shouldEqual List(
      LiteralNode(argName),
      SubTypeNode(TypeNode(argTypeName, Nil)),
      SubExprNode(ExprChoiceNode())
    )

    val setExprKindAction = SelectExprAction(setArgTypeAction.newTree, List(2), "Var")
    setExprKindAction.newTree.args shouldEqual List(
      LiteralNode(argName),
      SubTypeNode(TypeNode(argTypeName, Nil)),
      SubExprNode(VariableNode("Var", List(LiteralNode(""))))
    )

    val setVarExprLiteral = EditLiteralAction(setExprKindAction.newTree, List(2, 0), argName)
    setVarExprLiteral.newTree.args shouldEqual List(
      LiteralNode(argName),
      SubTypeNode(TypeNode(argTypeName, Nil)),
      SubExprNode(VariableNode("Var", List(LiteralNode(argName))))
    )
  }

  property("Lambda node behaves appropriately with complex argument type") {
    val initialTree = VariableNode.createFromExprName("Lambda").get
    initialTree.args shouldEqual List(LiteralNode(""), SubTypeNode(TypeChoiceNode()), SubExprNode(ExprChoiceNode()))

    val argName: String = "bar"
    val editVarNameAction = EditLiteralAction(initialTree, List(0), argName)
    editVarNameAction.newTree.args shouldEqual List(
      LiteralNode(argName),
      SubTypeNode(TypeChoiceNode()),
      SubExprNode(ExprChoiceNode())
    )

    val completeTypeNode =
      SubTypeNode(TypeNode("Func", List(SubTypeNode(TypeNode("IntType", Nil)), SubTypeNode(TypeNode("IntType", Nil)))))

    val setArgFuncTypeAction = SelectTypeAction(editVarNameAction.newTree, List(1), "Func")
    setArgFuncTypeAction.newTree.args shouldEqual List(
      LiteralNode(argName),
      SubTypeNode(TypeNode("Func", List(SubTypeNode(TypeChoiceNode()), SubTypeNode(TypeChoiceNode())))),
      SubExprNode(ExprChoiceNode())
    )

    val setArgFuncInTypeAction = SelectTypeAction(setArgFuncTypeAction.newTree, List(1, 0), "IntType")
    setArgFuncInTypeAction.newTree.args shouldEqual List(
      LiteralNode(argName),
      SubTypeNode(TypeNode("Func", List(SubTypeNode(TypeNode("IntType", Nil)), SubTypeNode(TypeChoiceNode())))),
      SubExprNode(ExprChoiceNode())
    )

    val setArgFuncOutTypeAction = SelectTypeAction(setArgFuncInTypeAction.newTree, List(1, 1), "IntType")
    setArgFuncOutTypeAction.newTree.args shouldEqual List(
      LiteralNode(argName),
      SubTypeNode(TypeNode("Func", List(SubTypeNode(TypeNode("IntType", Nil)), SubTypeNode(TypeNode("IntType", Nil))))),
      SubExprNode(ExprChoiceNode())
    )

    val setExprKindAction = SelectExprAction(setArgFuncOutTypeAction.newTree, List(2), "Var")
    setExprKindAction.newTree.args shouldEqual List(
      LiteralNode(argName),
      completeTypeNode,
      SubExprNode(VariableNode("Var", List(LiteralNode(""))))
    )

    val setVarExprLiteral = EditLiteralAction(setExprKindAction.newTree, List(2, 0), argName)
    setVarExprLiteral.newTree.args shouldEqual List(
      LiteralNode(argName),
      completeTypeNode,
      SubExprNode(VariableNode("Var", List(LiteralNode(argName))))
    )
  }

  property("Lambda expression string can be correctly read") {
    val tree = incrementFunction
    val treeString = "Lambda(\"x\",IntType(),Plus(Var(\"x\"),Num(1)))"
    readExpr(tree.toString) shouldEqual Some(incrementFunction)

    val tree2 = incrementTwiceFunction
    readExpr(tree2.toString) shouldEqual Some(incrementTwiceFunction)
  }

  property("Lambda is converted to HTML without error") {
    val node1 = VariableNode(
      "Apply",
      List(
        SubExprNode(
          VariableNode(
            "Lambda",
            List(
              LiteralNode("x"),
              SubTypeNode(TypeNode("IntType", Nil)),
              SubExprNode(
                VariableNode(
                  "Plus",
                  List(
                    SubExprNode(VariableNode("Var", List(LiteralNode("x")))),
                    SubExprNode(VariableNode("Num", List(LiteralNode("1"))))
                  )
                )
              )
            )
          )
        ),
        SubExprNode(VariableNode("Num", List(LiteralNode("3"))))
      )
    )

    val node2 = VariableNode(
      "Lambda",
      List(LiteralNode(""), SubTypeNode(TypeChoiceNode()), SubExprNode(VariableNode("Var", List(LiteralNode("bar")))))
    )

    val nodes: TableFor1[VariableNode] = Table("node", node1, node2)
    val modes: TableFor1[DisplayMode] = Table("mode", DisplayMode.values: _*)

    modes.forEvery({ mode =>
      nodes.forEvery({ node =>
        HTMLConvertor(LLam, mode).convert(node)
      })
    })
  }

  property("Lambda node createAction behaves appropriately with complex argument type") {
    val initialTree = VariableNode.createFromExprName("Lambda").get
    initialTree.args shouldEqual List(LiteralNode(""), SubTypeNode(TypeChoiceNode()), SubExprNode(ExprChoiceNode()))

    val argName: String = "super1984"
    val editVarNameAction = createAction("EditLiteralAction", initialTree.toString, "0", List(argName))
    editVarNameAction.newTree.args shouldEqual List(
      LiteralNode(argName),
      SubTypeNode(TypeChoiceNode()),
      SubExprNode(ExprChoiceNode())
    )

    val completeTypeNode =
      SubTypeNode(TypeNode("Func", List(SubTypeNode(TypeNode("IntType", Nil)), SubTypeNode(TypeNode("IntType", Nil)))))

    val setArgFuncTypeAction =
      createAction("SelectTypeAction", editVarNameAction.newTree.toString, "1", List("Func"))
    setArgFuncTypeAction.newTree.args shouldEqual List(
      LiteralNode(argName),
      SubTypeNode(TypeNode("Func", List(SubTypeNode(TypeChoiceNode()), SubTypeNode(TypeChoiceNode())))),
      SubExprNode(ExprChoiceNode())
    )

    val setArgFuncInTypeAction =
      createAction("SelectTypeAction", setArgFuncTypeAction.newTree.toString, "1-0", List("IntType"))
    setArgFuncInTypeAction.newTree.args shouldEqual List(
      LiteralNode(argName),
      SubTypeNode(TypeNode("Func", List(SubTypeNode(TypeNode("IntType", Nil)), SubTypeNode(TypeChoiceNode())))),
      SubExprNode(ExprChoiceNode())
    )

    val setArgFuncOutTypeAction =
      createAction("SelectTypeAction", setArgFuncInTypeAction.newTree.toString, "1-1", List("IntType"))
    setArgFuncOutTypeAction.newTree.args shouldEqual List(
      LiteralNode(argName),
      SubTypeNode(TypeNode("Func", List(SubTypeNode(TypeNode("IntType", Nil)), SubTypeNode(TypeNode("IntType", Nil))))),
      SubExprNode(ExprChoiceNode())
    )

    val setExprKindAction =
      createAction("SelectExprAction", setArgFuncOutTypeAction.newTree.toString, "2", List("Var"))
    setExprKindAction.newTree.args shouldEqual List(
      LiteralNode(argName),
      completeTypeNode,
      SubExprNode(VariableNode("Var", List(LiteralNode(""))))
    )

    val setVarExprLiteral =
      createAction("EditLiteralAction", setExprKindAction.newTree.toString, "2-0", List(argName))
    setVarExprLiteral.newTree.args shouldEqual List(
      LiteralNode(argName),
      completeTypeNode,
      SubExprNode(VariableNode("Var", List(LiteralNode(argName))))
    )
  }

  property("Apply node has a third tree shown") {
    val expr = Apply(incrementFunction, Num(8))
    val tree = VariableNode.fromExpr(expr)

    tree.getExpr shouldEqual expr

    tree.getVisibleChildren(DisplayMode.Edit) shouldEqual tree.children
    tree.getVisibleChildren(DisplayMode.TypeCheck) shouldEqual tree.children

    tree.getVisibleChildren(DisplayMode.Evaluation) shouldEqual
      tree.children :+ VariableNode.fromExpr(Plus(Var("x"), Num(1)))

    val phantomTree = tree.getVisibleChildren(DisplayMode.Evaluation).last.asInstanceOf[ExprNode]
    phantomTree.getExpr shouldEqual Plus(Var("x"), Num(1))
    phantomTree.getEvalEnv shouldEqual Env("x" -> NumV(8))
    phantomTree.getValue shouldEqual NumV(9)

    val exprChoicePhantomExpr =
      Apply(Lambda(LiteralIdentifier("x"), BlankTypeDropDown(), BlankExprDropDown()), BlankExprDropDown())
    val exprChoicePhantomTree = VariableNode.fromExpr(exprChoicePhantomExpr)
    exprChoicePhantomTree.getVisibleChildren(DisplayMode.Evaluation) shouldEqual
      exprChoicePhantomTree.children :+ ExprChoiceNode()
    exprChoicePhantomTree.getVisibleChildren(DisplayMode.Evaluation).last.isPhantom shouldEqual true
  }

  property("Apply doesn't show a third child if the left-hand side is not a function") {
    val expressions: TableFor1[Expr] =
      Table("expr", Apply(Num(4), Num(4)), Apply(Equal(Apply(incrementFunction, Num(4)), Num(5)), Bool(false)))

    forAll(expressions) { expr =>
      expr.getChildrenEval().length shouldEqual 2
    }
  }

  property("Lambda expression has correct environment when editing subexpression") {
    val expr = incrementFunction
    val tree = VariableNode(
      "Lambda",
      List(
        LiteralNode("x"),
        SubTypeNode(TypeNode("IntType", Nil)),
        SubExprNode(
          VariableNode(
            "Plus",
            List(
              SubExprNode(VariableNode("Var", List(LiteralNode("x")))),
              SubExprNode(VariableNode("Num", List(LiteralNode("1"))))
            )
          )
        )
      )
    )

    val subExprNode = tree.children.last.asInstanceOf[VariableNode]
    subExprNode.getEditEnv shouldEqual Env("x" -> HiddenValue(IntType()))
    subExprNode.getTypeEnv shouldEqual Env("x" -> IntType())
    subExprNode.getType shouldEqual IntType()
  }

  property("Lambda pretty prints correctly") {
    incrementFunction.prettyPrint shouldEqual "λx: Int. x + 1"
    twiceFunction.prettyPrint shouldEqual "λf: (Int → Int). λx: Int. f (f x)"

    incrementFunction.eval().prettyPrint shouldEqual "λx: Int. x + 1"
    twiceFunction.eval().prettyPrint shouldEqual "λf: (Int → Int). λx: Int. f (f x)"
  }

  property("Apply pretty prints correctly") {
    Apply(incrementFunction, Num(8)).prettyPrint shouldEqual "(λx: Int. x + 1) 8"
    Apply(Bool(false), Num(45)).prettyPrint shouldEqual "false 45"
    Apply(
      twiceFunction,
      incrementFunction
    ).prettyPrint shouldEqual "(λf: (Int → Int). λx: Int. f (f x)) (λx: Int. x + 1)"
  }

  property("Func pretty prints correctly") {
    Func(IntType(), IntType()).prettyPrint shouldEqual "Int → Int"
    Func(BoolType(), IntType()).prettyPrint shouldEqual "Bool → Int"
    Func(BoolType(), Func(IntType(), IntType())).prettyPrint shouldEqual "Bool → (Int → Int)"
    Func(Func(IntType(), IntType()), IntType()).prettyPrint shouldEqual "(Int → Int) → Int"
  }

  property("Lambda node type selects are not both editable") {
    val convertor = HTMLConvertor(LLam, DisplayMode.Edit)
    val l = convertor.lang
    val tree = l.VariableNode.createFromExprName("Lambda").get
    val res: String = convertor.convert(tree)
    val regex = """<select class="type-dropdown[\s\w"-=]+>""".r
    val typeSelects = regex.findAllIn(res).toList
    println(typeSelects.mkString("\n"))
    typeSelects.count(_.contains("disabled")) shouldEqual 1
  }
}
