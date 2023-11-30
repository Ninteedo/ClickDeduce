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
  val incrementTwiceFunction: Apply = Apply(twiceFunction, incrementFunction)

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

  property("Lambda node behaves appropriately with simple argument type") {
    val initialTree = VariableNode.createFromExprName("Lambda")
    initialTree.args shouldEqual List(
      LiteralNode(""),
      SubTypeNode(TypeChoiceNode()),
      SubExprNode(ExprChoiceNode())
    )

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
    val initialTree = VariableNode.createFromExprName("Lambda")
    initialTree.args shouldEqual List(
      LiteralNode(""),
      SubTypeNode(TypeChoiceNode()),
      SubExprNode(ExprChoiceNode())
    )

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
    val tree = Apply(incrementFunction, Num(3))
    val node = VariableNode(
      "Apply",
      List(
        SubExprNode(VariableNode(
          "Lambda",
          List(
            LiteralNode("x"),
            SubTypeNode(TypeNode("IntType", Nil)),
            SubExprNode(VariableNode(
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
    val htmlVersion = node.toHtml(NodeDisplayMode.Edit).toString
  }
}