package languages

import languages.LLam.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.{TableFor1, TableFor2, TableFor3, TableFor4}
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

  "DeleteAction" should {
    "delete an expr node from a tree" in {
      val trees: TableFor3[ExprNode, List[Int], ExprNode] = TableFor3(
        ("tree", "treePath", "result"),
        (VariableNode.fromExpr(Num(1)), List(), ExprChoiceNode()),
        (
          VariableNode.fromExpr(Let("x", Plus(Num(54), Num(-1)), Var("x"))),
          List(1, 0),
          VariableNode.fromExpr(Let("x", Plus(BlankExprDropDown(), Num(-1)), Var("x")))
        ),
        (
          VariableNode.fromExpr(Let("x", Plus(Num(54), Num(-1)), Var("x"))),
          List(1),
          VariableNode.fromExpr(Let("x", BlankExprDropDown(), Var("x")))
        ),
        (
          VariableNode.fromExpr(Let("x", Plus(Num(54), Num(-1)), Var("x"))),
          List(2),
          VariableNode.fromExpr(Let("x", Plus(Num(54), Num(-1)), BlankExprDropDown()))
        ),
        (
          VariableNode.fromExpr(Apply(Lambda("x", IntType(), Plus(Var("x"), Num(1))), Num(65))),
          List(0, 2),
          VariableNode.fromExpr(Apply(Lambda("x", IntType(), BlankExprDropDown()), Num(65)))
        ),
        (
          VariableNode.fromExpr(Apply(Lambda("x", IntType(), Plus(Var("x"), Num(1))), Num(65))),
          List(1),
          VariableNode.fromExpr(Apply(Lambda("x", IntType(), Plus(Var("x"), Num(1))), BlankExprDropDown()))
        )
      )

      forAll(trees) { (tree, treePath, result) =>
        val action = DeleteAction(tree, treePath)
        action.newTree shouldEqual result
      }
    }

    "delete a type node from a tree" in {
      val equalsZeroFunction = Lambda("value", Func(IntType(), BoolType()), Eq(Var("value"), Num(0)))

      val trees: TableFor3[ExprNode, List[Int], ExprNode] = TableFor3(
        ("tree", "treePath", "result"),
        (
          VariableNode.fromExpr(Lambda("x", IntType(), Plus(Var("x"), Num(1)))),
          List(1),
          VariableNode.fromExpr(Lambda("x", BlankTypeDropDown(), Plus(Var("x"), Num(1))))
        ),
        (
          VariableNode.fromExpr(Apply(Lambda("x", IntType(), Plus(Var("x"), Num(1))), Num(65))),
          List(0, 1),
          VariableNode.fromExpr(Apply(Lambda("x", BlankTypeDropDown(), Plus(Var("x"), Num(1))), Num(65)))
        ),
        (
          VariableNode.fromExpr(equalsZeroFunction),
          List(1),
          VariableNode.fromExpr(Lambda("value", BlankTypeDropDown(), Eq(Var("value"), Num(0))))
        ),
        (
          VariableNode.fromExpr(equalsZeroFunction),
          List(1, 0),
          VariableNode.fromExpr(Lambda("value", Func(BlankTypeDropDown(), BoolType()), Eq(Var("value"), Num(0))))
        ),
        (
          VariableNode.fromExpr(equalsZeroFunction),
          List(1, 1),
          VariableNode.fromExpr(Lambda("value", Func(IntType(), BlankTypeDropDown()), Eq(Var("value"), Num(0))))
        )
      )

      forAll(trees) { (tree, treePath, result) =>
        val action = DeleteAction(tree, treePath)
        action.newTree shouldEqual result
      }
    }

    "throws an error when attempting to delete a literal node" in {
      val f = Lambda("x", IntType(), Plus(Var("x"), Num(1)))

      val trees: TableFor2[ExprNode, List[Int]] = TableFor2(
        ("tree", "treePath"),
        (VariableNode.fromExpr(Num(1)), List(0)),
        (VariableNode.fromExpr(f), List(0)),
        (VariableNode.fromExpr(f), List(2, 0, 0)),
        (VariableNode.fromExpr(f), List(2, 1, 0))
      )

      forAll(trees) { (tree, treePath) =>
        an[InvalidDeleteTargetException] should be thrownBy DeleteAction(tree, treePath).newTree
      }
    }
  }

  "PasteAction" should {
    "correctly paste an expr node string into a tree" in {
      val trees: TableFor4[ExprNode, List[Int], String, ExprNode] = TableFor4(
        ("tree", "treePath", "pasteNodeString", "result"),
        (
          VariableNode.fromExpr(Plus(Num(1), Num(2))),
          List(0),
          VariableNode.fromExpr(IfThenElse(Bool(true), Num(-1), Num(2))).toString,
          VariableNode.fromExpr(Plus(IfThenElse(Bool(true), Num(-1), Num(2)), Num(2)))
        ),
        (
          VariableNode.fromExpr(Plus(Num(1), IfThenElse(Bool(true), BlankExprDropDown(), Bool(false)))),
          List(1, 1),
          VariableNode.fromExpr(Var("hello")).toString,
          VariableNode.fromExpr(Plus(Num(1), IfThenElse(Bool(true), Var("hello"), Bool(false))))
        ),
        (
          VariableNode.fromExpr(Plus(Num(1), Num(2))),
          List(1),
          VariableNode.fromExpr(Let("x", Num(1), Num(2))).toString,
          VariableNode.fromExpr(Plus(Num(1), Let("x", Num(1), Num(2))))
        ),
        (
          VariableNode.fromExpr(Let("x", Let("y", Num(6), Num(0)), Var("x"))),
          List(1, 2),
          VariableNode.fromExpr(Plus(Var("y"), Num(1))).toString,
          VariableNode.fromExpr(Let("x", Let("y", Num(6), Plus(Var("y"), Num(1))), Var("x")))
        )
      )

      forAll(trees) { (tree, treePath, pasteNodeString, result) =>
        val action = PasteAction(tree, treePath, pasteNodeString)
        action.newTree shouldEqual result
      }
    }

    "correctly paste a type node string into a tree" in {
      val trees: TableFor4[ExprNode, List[Int], String, ExprNode] = TableFor4(
        ("tree", "treePath", "pasteNodeString", "result"),
        (
          VariableNode.fromExpr(Lambda("x", IntType(), Plus(Var("x"), Num(1)))),
          List(1),
          TypeNode.fromType(BoolType()).toString,
          VariableNode.fromExpr(Lambda("x", BoolType(), Plus(Var("x"), Num(1))))
        ),
        (
          VariableNode.fromExpr(Lambda("x", Func(BlankTypeDropDown(), IntType()), Plus(Var("x"), Num(1)))),
          List(1, 1),
          TypeNode.fromType(BlankTypeDropDown()).toString,
          VariableNode.fromExpr(Lambda("x", Func(BlankTypeDropDown(), BlankTypeDropDown()), Plus(Var("x"), Num(1))))
        ),
        (
          VariableNode.fromExpr(Lambda("x", Func(BlankTypeDropDown(), IntType()), Plus(Var("x"), Num(1)))),
          List(1),
          TypeNode.fromType(Func(Func(BoolType(), BoolType()), IntType())).toString,
          VariableNode.fromExpr(Lambda("x", Func(Func(BoolType(), BoolType()), IntType()), Plus(Var("x"), Num(1))))
        )
      )

      forAll(trees) { (tree, treePath, pasteNodeString, result) =>
        val action = PasteAction(tree, treePath, pasteNodeString)
        action.newTree shouldEqual result
      }
    }

    "throws an error when attempting to paste a type node into an expr node" in {
      val trees: TableFor3[ExprNode, List[Int], String] = TableFor3(
        ("tree", "treePath", "pasteNodeString"),
        (VariableNode.fromExpr(Num(1)), List(), TypeNode.fromType(IntType()).toString),
        (VariableNode.fromExpr(Plus(Num(1), Num(2))), List(0), TypeNode.fromType(BoolType()).toString),
        (
          VariableNode.fromExpr(Lambda("x", IntType(), Plus(Var("x"), Num(1)))),
          List(2),
          TypeNode.fromType(BoolType()).toString
        ),
        (VariableNode.fromExpr(Num(1)), List(), TypeNode.fromType(BlankTypeDropDown()).toString)
      )

      forAll(trees) { (tree, treePath, pasteNodeString) =>
        an[InvalidPasteTargetException] should be thrownBy PasteAction(tree, treePath, pasteNodeString).newTree
      }
    }

    "throws an error when attempting to paste an expr node into a type node" in {
      val trees: TableFor3[ExprNode, List[Int], String] = TableFor3(
        ("tree", "treePath", "pasteNodeString"),
        (
          VariableNode.fromExpr(Lambda("x", IntType(), Plus(Var("x"), Num(1)))),
          List(1),
          VariableNode.fromExpr(Num(1)).toString
        ),
        (
          VariableNode.fromExpr(Lambda("gee", Func(BoolType(), IntType()), Num(73))),
          List(1, 1),
          VariableNode.fromExpr(Bool(true)).toString
        ),
        (
          VariableNode.fromExpr(Lambda("gee", Func(BoolType(), IntType()), Num(73))),
          List(1, 0),
          VariableNode.fromExpr(BlankExprDropDown()).toString
        )
      )

      forAll(trees) { (tree, treePath, pasteNodeString) =>
        an[InvalidPasteTargetException] should be thrownBy PasteAction(tree, treePath, pasteNodeString).newTree
      }
    }

    "throws an error when attempting to paste into a literal node" in {
      val trees: TableFor3[ExprNode, List[Int], String] = TableFor3(
        ("tree", "treePath", "pasteNodeString"),
        (VariableNode.fromExpr(Num(1)), List(0), VariableNode.fromExpr(Num(1)).toString),
        (VariableNode.fromExpr(Plus(Num(1), Num(2))), List(0, 0), VariableNode.fromExpr(Num(1)).toString),
        (
          VariableNode.fromExpr(Lambda("x", IntType(), Plus(Var("x"), Num(1)))),
          List(0),
          VariableNode.fromExpr(Num(1)).toString
        ),
        (
          VariableNode.fromExpr(Lambda("x", IntType(), Plus(Var("x"), Num(1)))),
          List(2, 0, 0),
          TypeNode.fromType(IntType()).toString
        )
      )

      forAll(trees) { (tree, treePath, pasteNodeString) =>
        an[InvalidPasteTargetException] should be thrownBy PasteAction(tree, treePath, pasteNodeString).newTree
      }
    }

    "throws an error when attempting to paste an invalid node string" in {
      val trees: TableFor3[ExprNode, List[Int], String] = TableFor3(
        ("tree", "treePath", "pasteNodeString"),
        (VariableNode.fromExpr(Num(1)), List(0), "Num(5)"),
        (VariableNode.fromExpr(Plus(Num(1), Num(2))), List(0, 0), "VariableNode()"),
        (
          VariableNode.fromExpr(Lambda("x", IntType(), Plus(Var("x"), Num(1)))),
          List(0),
          "VariableNode(\"Num\", List())"
        ),
        (VariableNode.fromExpr(Lambda("x", IntType(), Plus(Var("x"), Num(1)))), List(1), "TypeNode(BoolType(), List())")
      )

      forAll(trees) { (tree, treePath, pasteNodeString) =>
        an[Exception] should be thrownBy PasteAction(tree, treePath, pasteNodeString).newTree
      }
    }
  }

  "Invalid actions creation" should {
    val realActionNames = TableFor1(
      "actionName",
      "SelectExprAction",
      "SelectTypeAction",
      "EditLiteralAction",
      "DeleteAction",
      "PasteAction",
      "IdentityAction"
    )

    "throw an error when attempting to create an action with an invalid action name" in {
      val fakeNames = TableFor1(
        "actionName",
        "FakeAction",
        "InvalidAction",
        "Paste",
        "ActionPaste",
        "IdentityAtion",
        "SelectExpr",
        "giuhahnan",
        "Boo",
        "Foo",
        "Bar",
        "SelectTypeAction1"
      )

      forAll(fakeNames) { actionName =>
        an[ActionInvocationException] should be thrownBy createAction(actionName, "ExprChoiceNode()", "", List())
      }
    }

    "throw an error when attempting to create an action with an invalid node string" in {
      val invalidNodeStrings = TableFor1(
        "nodeString",
        "ExprChoiceNode",
        "TypeChoiceNode",
        "VariableNode",
        "SubExprNode(ExprChoiceNode())",
        "SubTypeNode(TypeChoiceNode())",
        "VariableNode()",
        "VariableNode('Num', List())",
        "NotARealNode(\"Num\")",
        "LiteralNode(\"x\")"
      )

      forAll(realActionNames) { actionName =>
        forAll(invalidNodeStrings) { nodeString =>
          val exception = intercept[Exception] {
            createAction(actionName, nodeString, "", List())
          }
          assert(exception.isInstanceOf[NodeStringParseException] || exception.isInstanceOf[ActionInvocationException])
        }
      }
    }

    "throw an error when provided with an invalid tree path" in {
      val treePaths = TableFor1("treePath", "-1", "x", "foo", "0-foo", "0--2")

      forAll(treePaths) { treePath =>
        an[InvalidTreePathStringException] should be thrownBy createAction(
          "IdentityAction",
          "ExprChoiceNode()",
          treePath,
          List()
        )
      }
    }

    "throw an error when not provided with too few extra arguments" in {
      val extraArgs = TableFor2(
        ("actionName", "extraArgs"),
        ("SelectExprAction", List()),
        ("SelectTypeAction", List()),
        ("EditLiteralAction", List()),
        ("PasteAction", List())
      )

      forAll(extraArgs) { (actionName, extraArgs) =>
        an[ActionInvocationException] should be thrownBy createAction(actionName, "ExprChoiceNode()", "", extraArgs)
      }
    }

    "throw an error when provided with too many extra arguments" in {
      val extraArgs = TableFor2(
        ("actionName", "extraArgs"),
        ("SelectExprAction", List("Eq", "foo")),
        ("SelectTypeAction", List("IntType", "foo")),
        ("EditLiteralAction", List("foo", "bar")),
        ("PasteAction", List(VariableNode.fromExpr(Num(1)).toString, "foo")),
        ("PasteAction", List("foo", "bar", "baz")),
        ("DeleteAction", List("foo")),
        ("IdentityAction", List("foo"))
      )

      forAll(extraArgs) { (actionName, extraArgs) =>
        an[ActionInvocationException] should be thrownBy createAction(actionName, "ExprChoiceNode()", "", extraArgs)
      }
    }
  }
}
