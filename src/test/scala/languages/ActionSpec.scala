package languages

import languages.LLam.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.{TableFor1, TableFor3, TableFor4}
import org.scalatest.wordspec.AnyWordSpec

class ActionSpec extends AnyWordSpec with Matchers {
  "Can create Actions using createAction" should {
    val trees = TableFor1(
      "node",
      VariableNode.fromExpr(Plus(Num(1), Times(Num(2), Num(3)))),
      VariableNode.fromExpr(IfThenElse(Bool(true), Num(1), Num(2))),
      VariableNode.fromExpr(Let("score", Num(1), Times(Var("score"), Num(10))))
    )

    "create a SelectExprAction" in {
      val tree = ExprChoiceNode()
      val action = createAction("SelectExprAction", tree.toString, "", List("Eq"))
      action shouldBe a[SelectExprAction]
      action.originalTree shouldBe tree
      action.treePath shouldBe List()
      action.asInstanceOf[SelectExprAction].exprChoiceName shouldBe "Eq"
    }

    "create a SelectTypeAction" in {
      val tree = VariableNode.fromExpr(Lambda("x", UnknownType(), Plus(Var("x"), Num(1))))
      val action = createAction("SelectTypeAction", tree.toString, "1", List("IntType"))
      action shouldBe a[SelectTypeAction]
      action.originalTree shouldBe tree
      action.treePath shouldBe List(1)
      action.asInstanceOf[SelectTypeAction].typeChoiceName shouldBe "IntType"
    }

    "create an EditLiteralAction" in {
      val tree = VariableNode.fromExpr(Num(1))
      val action = createAction("EditLiteralAction", tree.toString, "0", List("hello"))
      action shouldBe an[EditLiteralAction]
      action.originalTree shouldBe tree
      action.treePath shouldBe List(0)
      action.asInstanceOf[EditLiteralAction].newLiteralText shouldBe "hello"
    }

    "create a DeleteAction" in {
      val tree = VariableNode.fromExpr(Plus(Num(1), Num(2)))
      val action = createAction("DeleteAction", tree.toString, "0", List())
      action shouldBe a[DeleteAction]
      action.originalTree shouldBe tree
      action.treePath shouldBe List(0)
    }

    "create a PasteAction" in {
      val tree = VariableNode.fromExpr(Plus(Num(5), Times(Num(1), Num(0))))
      val pasteTree = VariableNode.fromExpr(Num(2))
      val action = createAction("PasteAction", tree.toString, "1-1", List(pasteTree.toString))
      action shouldBe a[PasteAction]
      action.originalTree shouldBe tree
      action.treePath shouldBe List(1, 1)
      action.asInstanceOf[PasteAction].pasteNodeString shouldBe pasteTree.toString
    }

    "create an IdentityAction" in {
      forAll(trees) { tree =>
        val action = createAction("IdentityAction", tree.toString, "", List())
        action shouldBe an[IdentityAction]
        action.originalTree shouldBe tree
        action.treePath shouldBe Nil
      }
    }
  }

  "SelectExprAction" should {
    "replace a root ExprChoiceNode with selection" in {
      val selectOptions =
        TableFor1("exprChoiceName", "Num", "Plus", "Times", "IfThenElse", "Eq", "Bool", "Let", "Var", "Lambda")

      val tree = ExprChoiceNode()
      forAll(selectOptions) { exprChoiceName =>
        val action = SelectExprAction(tree, List(), exprChoiceName)
        action.newTree shouldBe a[VariableNode]
        action.newTree.asInstanceOf[VariableNode].exprName shouldBe exprChoiceName
        action.newTree shouldEqual VariableNode.createFromExprName(exprChoiceName)
      }
    }

    "replace a nested ExprChoiceNode with selection" in {
      val trees: TableFor4[ExprNode, List[Int], String, ExprNode] = TableFor4(
        ("tree", "treePath", "exprChoiceName", "result"),
        (
          VariableNode.fromExpr(Plus(BlankExprDropDown(), Num(1))),
          List(0),
          "Num",
          VariableNode.fromExpr(Plus(Num(LiteralAny("")), Num(1)))
        ),
        (
          VariableNode.fromExpr(Plus(Num(1), BlankExprDropDown())),
          List(1),
          "Bool",
          VariableNode.fromExpr(Plus(Num(1), Bool(LiteralAny(""))))
        ),
        (
          VariableNode.fromExpr(IfThenElse(Plus(Num(1), BlankExprDropDown()), Num(6), BlankExprDropDown())),
          List(0, 1),
          "Lambda",
          VariableNode
            .fromExpr(IfThenElse(Plus(Num(1), BlankExprDropDown()), Num(6), BlankExprDropDown()))
            .replace(List(0, 1), VariableNode.createFromExprName("Lambda"))
            .asInstanceOf[ExprNode]
        )
      )

      forAll(trees) { (tree, treePath, exprChoiceName, result) =>
        val action = SelectExprAction(tree, treePath, exprChoiceName)
        action.newTree shouldBe a[VariableNode]
        action.newTree shouldEqual result
      }
    }

    "throw an error when attempting to replace something other than an ExprChoiceNode" in {
      val trees: TableFor3[ExprNode, List[Int], String] = TableFor3(
        ("tree", "treePath", "exprChoiceName"),
        (VariableNode.fromExpr(Num(1)), List(), "Plus"),
        (VariableNode.fromExpr(Plus(Bool(true), Num(-4))), List(0), "Num"),
        (VariableNode.fromExpr(IfThenElse(Bool(true), BlankExprDropDown(), BlankExprDropDown())), List(0), "Eq")
      )
      forAll(trees) { (tree, treePath, exprChoiceName) =>
        an[InvalidSelectTargetException] should be thrownBy SelectExprAction(tree, treePath, exprChoiceName).newTree
      }
    }
  }

  "SelectTypeAction" should {
    "replace a root TypeChoiceNode with selection" in {
      val selectOptions = TableFor1("typeChoiceName", "IntType", "BoolType", "UnknownType", "Func")

      val tree = TypeChoiceNode()
      forAll(selectOptions) { typeChoiceName =>
        val action = SelectTypeAction(tree, List(), typeChoiceName)
        action.newTree shouldBe a[TypeNode]
        action.newTree.asInstanceOf[TypeNode].typeName shouldBe typeChoiceName
        typeChoiceName match {
          case "Func" =>
            action.newTree shouldEqual TypeNode(
              typeChoiceName,
              List(SubTypeNode(TypeChoiceNode()), SubTypeNode(TypeChoiceNode()))
            )
          case _ => action.newTree shouldEqual TypeNode(typeChoiceName, List())
        }
      }
    }

    "replace a nested TypeChoiceNode with selection" in {
      val trees: TableFor4[ExprNode, List[Int], String, ExprNode] = TableFor4(
        ("tree", "treePath", "typeChoiceName", "result"),
        (
          VariableNode.fromExpr(Lambda("x", BlankTypeDropDown(), Var("x"))),
          List(1),
          "IntType",
          VariableNode.fromExpr(Lambda("x", IntType(), Var("x")))
        ),
        (
          VariableNode.fromExpr(
            IfThenElse(
              Bool(false),
              Lambda("foo2", BlankTypeDropDown(), Num(3)),
              Lambda("foo2", Func(IntType(), BlankTypeDropDown()), Var("foo2"))
            )
          ),
          List(2, 1, 1),
          "BoolType",
          VariableNode.fromExpr(
            IfThenElse(
              Bool(false),
              Lambda("foo2", BlankTypeDropDown(), Num(3)),
              Lambda("foo2", Func(IntType(), BoolType()), Var("foo2"))
            )
          )
        )
      )

      forAll(trees) { (tree, treePath, typeChoiceName, result) =>
        val action = SelectTypeAction(tree, treePath, typeChoiceName)
        action.newTree shouldBe a[VariableNode]
        action.newTree shouldEqual result
      }
    }

    "throw an error when attempting to replace something other than a TypeChoiceNode" in {
      val trees: TableFor3[ExprNode, List[Int], String] = TableFor3(
        ("tree", "treePath", "typeChoiceName"),
        (VariableNode.fromExpr(Num(1)), List(), "IntType"),
        (VariableNode.fromExpr(Lambda("x", IntType(), Num(1))), List(0), "BoolType"),
        (
          VariableNode.fromExpr(
            IfThenElse(
              Bool(false),
              Lambda("foo2", BlankTypeDropDown(), Num(3)),
              Lambda("foo2", Func(IntType(), BoolType()), Var("foo2"))
            )
          ),
          List(2, 1),
          "Func"
        )
      )

      forAll(trees) { (tree, treePath, typeChoiceName) =>
        an[InvalidSelectTargetException] should be thrownBy SelectTypeAction(tree, treePath, typeChoiceName).newTree
      }
    }
  }

  "EditLiteralAction" should {
    "replace the contents of a nested LiteralNode" in {
      val trees: TableFor4[ExprNode, List[Int], String, ExprNode] = TableFor4(
        ("tree", "treePath", "newLiteralText", "result"),
        (VariableNode.fromExpr(Plus(Num(1), Num(2))), List(0, 0), "3", VariableNode.fromExpr(Plus(Num(3), Num(2)))),
        (
          VariableNode.fromExpr(
            Times(Num(61), IfThenElse(Eq(Num(5), Bool(Literal.fromString("foo"))), Num(1), Num(-62)))
          ),
          List(1, 0, 1, 0),
          "bar",
          VariableNode.fromExpr(
            Times(Num(61), IfThenElse(Eq(Num(5), Bool(Literal.fromString("bar"))), Num(1), Num(-62)))
          )
        ),
        (
          VariableNode.fromExpr(Lambda("", IntType(), Plus(Var("eg"), Num(1)))),
          List(0),
          "be123",
          VariableNode.fromExpr(Lambda("be123", IntType(), Plus(Var("eg"), Num(1))))
        ),
        (
          VariableNode.fromExpr(Lambda("be123", IntType(), Plus(Var("eg"), Num(1)))),
          List(2, 0, 0),
          "",
          VariableNode.fromExpr(Lambda("be123", IntType(), Plus(Var(""), Num(1))))
        )
      )

      forAll(trees) { (tree, treePath, newLiteralText, result) =>
        val action = EditLiteralAction(tree, treePath, newLiteralText)
        action.newTree shouldBe a[VariableNode]
        action.newTree shouldEqual result
      }
    }

    "throw an error when attempting to replace something other than a LiteralNode" in {
      val trees: TableFor3[ExprNode, List[Int], String] = TableFor3(
        ("tree", "treePath", "newLiteralText"),
        (VariableNode.fromExpr(Num(1)), List(), "st"),
        (VariableNode.fromExpr(Plus(Num(1), Num(2))), List(0), "tj461"),
        (
          VariableNode.fromExpr(IfThenElse(Bool(true), Lambda("z", IntType(), Bool(LiteralAny("hi"))), Num(2))),
          List(1, 2),
          "err"
        )
      )

      forAll(trees) { (tree, treePath, newLiteralText) =>
        an[InvalidEditTargetException] should be thrownBy EditLiteralAction(tree, treePath, newLiteralText).newTree
      }
    }
  }
}
