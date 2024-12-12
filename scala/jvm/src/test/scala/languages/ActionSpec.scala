package languages

import actions.*
import actions.exceptions.*
import languages.LLam.*
import languages.terms.*
import languages.terms.blanks.{BlankExprDropDown, BlankTypeDropDown}
import languages.terms.literals.*
import languages.terms.types.*
import nodes.*
import nodes.exceptions.{InvalidTreePathStringException, NodeStringParseException}
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.{TableFor1, TableFor2, TableFor3, TableFor4}
import org.scalatest.wordspec.AnyWordSpec

class ActionSpec extends AnyWordSpec with Matchers {
  "Can create Actions using createAction" should {
    val trees = TableFor1(
      "node",
      ExprNode.fromExpr(LLam, Plus(Num(1), Times(Num(2), Num(3)))),
      ExprNode.fromExpr(LLam, IfThenElse(Bool(true), Num(1), Num(2))),
      ExprNode.fromExpr(LLam, Let("score", Num(1), Times(Var("score"), Num(10))))
    )

    "create a SelectExprAction" in {
      val tree = ExprChoiceNode(LLam)
      val action = createAction("SelectExprAction", tree.toString, "", List("Equal"))
      action shouldBe a[SelectExprAction]
      action.originalTree shouldBe tree
      action.treePath shouldBe List()
      action.asInstanceOf[SelectExprAction].exprChoiceName shouldBe "Equal"
    }

    "create a SelectTypeAction" in {
      val tree = ExprNode.fromExpr(LLam, Lambda("x", UnknownType(), Plus(Var("x"), Num(1))))
      val action = createAction("SelectTypeAction", tree.toString, "1", List("IntType"))
      action shouldBe a[SelectTypeAction]
      action.originalTree shouldBe tree
      action.treePath shouldBe List(1)
      action.asInstanceOf[SelectTypeAction].typeChoiceName shouldBe "IntType"
    }

    "create an EditLiteralAction" in {
      val tree = ExprNode.fromExpr(LLam, Num(1))
      val action = createAction("EditLiteralAction", tree.toString, "0", List("hello"))
      action shouldBe an[EditLiteralAction]
      action.originalTree shouldBe tree
      action.treePath shouldBe List(0)
      action.asInstanceOf[EditLiteralAction].newLiteralText shouldBe "hello"
    }

    "create a DeleteAction" in {
      val tree = ExprNode.fromExpr(LLam, Plus(Num(1), Num(2)))
      val action = createAction("DeleteAction", tree.toString, "0", List())
      action shouldBe a[DeleteAction]
      action.originalTree shouldBe tree
      action.treePath shouldBe List(0)
    }

    "create a PasteAction" in {
      val tree = ExprNode.fromExpr(LLam, Plus(Num(5), Times(Num(1), Num(0))))
      val pasteTree = ExprNode.fromExpr(LLam, Num(2))
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
        TableFor1("exprChoiceName", "Num", "Plus", "Times", "IfThenElse", "Equal", "Bool", "Let", "Var", "Lambda")

      val tree = ExprChoiceNode(LLam)
      forAll(selectOptions) { exprChoiceName =>
        val action = SelectExprAction(tree, List(), LLam, exprChoiceName)
        action.newTree shouldBe a[ExprNode]
        action.newTree.asInstanceOf[ExprNode].exprName shouldBe exprChoiceName
        action.newTree shouldEqual ExprNode.createFromExprName(LLam, exprChoiceName).get
      }
    }

    "replace a nested ExprChoiceNode with selection" in {
      val trees: TableFor4[ExprNodeParent, List[Int], String, ExprNodeParent] = TableFor4(
        ("tree", "treePath", "exprChoiceName", "result"),
        (
          ExprNode.fromExpr(LLam, Plus(BlankExprDropDown(LLam), Num(1))),
          List(0),
          "Num",
          ExprNode.fromExpr(LLam, Plus(Num(LiteralInt(0)), Num(1)))
        ),
        (
          ExprNode.fromExpr(LLam, Plus(Num(1), BlankExprDropDown(LLam))),
          List(1),
          "Bool",
          ExprNode.fromExpr(LLam, Plus(Num(1), Bool(LiteralBool(false))))
        ),
        (
          ExprNode.fromExpr(LLam, IfThenElse(Plus(Num(1), BlankExprDropDown(LLam)), Num(6), BlankExprDropDown(LLam))),
          List(0, 1),
          "Lambda",
          ExprNode
            .fromExpr(LLam, IfThenElse(Plus(Num(1), BlankExprDropDown(LLam)), Num(6), BlankExprDropDown(LLam)))
            .replace(List(0, 1), ExprNode.createFromExprName(LLam, "Lambda").get)
            .asInstanceOf[ExprNode]
        )
      )

      forAll(trees) { (tree, treePath, exprChoiceName, result) =>
        val action = SelectExprAction(tree, treePath, LLam, exprChoiceName)
        action.newTree shouldBe a[ExprNode]
        action.newTree shouldEqual result
      }
    }

    "throw an error when attempting to replace something other than an ExprChoiceNode" in {
      val trees: TableFor3[ExprNodeParent, List[Int], String] = TableFor3(
        ("tree", "treePath", "exprChoiceName"),
        (ExprNode.fromExpr(LLam, Num(1)), List(), "Plus"),
        (ExprNode.fromExpr(LLam, Plus(Bool(true), Num(-4))), List(0), "Num"),
        (ExprNode.fromExpr(LLam, IfThenElse(Bool(true), BlankExprDropDown(LLam), BlankExprDropDown(LLam))), List(0), "Equal")
      )
      forAll(trees) { (tree, treePath, exprChoiceName) =>
        an[InvalidSelectTargetException] should be thrownBy SelectExprAction(tree, treePath, LLam, exprChoiceName).newTree
      }
    }

    "throw an error if the expression kind is not defined in the language" in {
      val cases: TableFor2[ClickDeduceLanguage, String] = TableFor2(
        ("lang", "exprName"),
        (LRec(), "fake"),
        (LArith(), "Rec"),
        (LIf(), "If Then Else"),
        (LArith(), "If Then Else"),
        (LArith(), "fake"),
        (LArith(), "Let"),
        (LArith(), "Var"),
        (LArith(), "Lambda"),
        (LArith(), "IntType")
      )

      forAll(cases) { (lang, exprName) =>
        an[InvalidSelectValueNameException] should be thrownBy
          SelectExprAction(ExprChoiceNode(lang), List(), lang, exprName).newTree
      }
    }
  }

  "SelectTypeAction" should {
    "replace a root TypeChoiceNode with selection" in {
      val selectOptions = TableFor1("typeChoiceName", "IntType", "BoolType", "UnknownType", "Func")

      val tree = TypeChoiceNode(LLam)
      forAll(selectOptions) { typeChoiceName =>
        val action = SelectTypeAction(tree, List(), LLam, typeChoiceName)
        action.newTree shouldBe a[TypeNode]
        action.newTree.asInstanceOf[TypeNode].typeName shouldBe typeChoiceName
        typeChoiceName match {
          case "Func" =>
            action.newTree shouldEqual TypeNode(
              LLam,
              typeChoiceName,
              List(SubTypeNode(TypeChoiceNode(LLam)), SubTypeNode(TypeChoiceNode(LLam)))
            )
          case _ => action.newTree shouldEqual TypeNode(LLam, typeChoiceName, List())
        }
      }
    }

    "replace a nested TypeChoiceNode with selection" in {
      val trees: TableFor4[ExprNodeParent, List[Int], String, ExprNodeParent] = TableFor4(
        ("tree", "treePath", "typeChoiceName", "result"),
        (
          ExprNode.fromExpr(LLam, Lambda("x", BlankTypeDropDown(LLam), Var("x"))),
          List(1),
          "IntType",
          ExprNode.fromExpr(LLam, Lambda("x", IntType(), Var("x")))
        ),
        (
          ExprNode.fromExpr(LLam,
            IfThenElse(
              Bool(false),
              Lambda("foo2", BlankTypeDropDown(LLam), Num(3)),
              Lambda("foo2", Func(IntType(), BlankTypeDropDown(LLam)), Var("foo2"))
            )
          ),
          List(2, 1, 1),
          "BoolType",
          ExprNode.fromExpr(LLam,
            IfThenElse(
              Bool(false),
              Lambda("foo2", BlankTypeDropDown(LLam), Num(3)),
              Lambda("foo2", Func(IntType(), BoolType()), Var("foo2"))
            )
          )
        )
      )

      forAll(trees) { (tree, treePath, typeChoiceName, result) =>
        val action = SelectTypeAction(tree, treePath, LLam, typeChoiceName)
        action.newTree shouldBe a[ExprNode]
        action.newTree shouldEqual result
      }
    }

    "throw an error when attempting to replace something other than a TypeChoiceNode" in {
      val trees: TableFor3[ExprNodeParent, List[Int], String] = TableFor3(
        ("tree", "treePath", "typeChoiceName"),
        (ExprNode.fromExpr(LLam, Num(1)), List(), "IntType"),
        (ExprNode.fromExpr(LLam, Lambda("x", IntType(), Num(1))), List(0), "BoolType"),
        (
          ExprNode.fromExpr(LLam,
            IfThenElse(
              Bool(false),
              Lambda("foo2", BlankTypeDropDown(LLam), Num(3)),
              Lambda("foo2", Func(IntType(), BoolType()), Var("foo2"))
            )
          ),
          List(2, 1),
          "Func"
        )
      )

      forAll(trees) { (tree, treePath, typeChoiceName) =>
        an[InvalidSelectTargetException] should be thrownBy SelectTypeAction(tree, treePath, LLam, typeChoiceName).newTree
      }
    }

    "throw an error if the type kind is not defined in the language" in {
      val cases: TableFor1[String] =
        TableFor1("typeName", "fake", "Num", "Plus", "Lambda", "Type", "Func(IntType(), IntType())")

      forAll(cases) { typeName =>
        an[InvalidSelectValueNameException] should be thrownBy SelectTypeAction(
          ExprNode.fromExpr(LLam, Lambda("x", BlankTypeDropDown(LLam), BlankExprDropDown(LLam))),
          List(1),
          LLam,
          typeName
        ).newTree
      }
    }
  }

  "EditLiteralAction" should {
    "replace the contents of a nested LiteralNode" in {
      val trees: TableFor4[ExprNodeParent, List[Int], String, ExprNodeParent] = TableFor4(
        ("tree", "treePath", "newLiteralText", "result"),
        (ExprNode.fromExpr(LLam, Plus(Num(1), Num(2))), List(0, 0), "3", ExprNode.fromExpr(LLam, Plus(Num(3), Num(2)))),
        (
          ExprNode.fromExpr(LLam,
            Times(Num(61), IfThenElse(Equal(Num(5), Bool(LiteralBool(false))), Num(1), Num(-62)))
          ),
          List(1, 0, 1, 0),
          "true",
          ExprNode.fromExpr(LLam,
            Times(Num(61), IfThenElse(Equal(Num(5), Bool(LiteralBool(true))), Num(1), Num(-62)))
          )
        ),
        (
          ExprNode.fromExpr(LLam, Lambda("", IntType(), Plus(Var("eg"), Num(1)))),
          List(0),
          "be123",
          ExprNode.fromExpr(LLam, Lambda("be123", IntType(), Plus(Var("eg"), Num(1))))
        ),
        (
          ExprNode.fromExpr(LLam, Lambda("be123", IntType(), Plus(Var("eg"), Num(1)))),
          List(2, 0, 0),
          "",
          ExprNode.fromExpr(LLam, Lambda("be123", IntType(), Plus(Var(""), Num(1))))
        )
      )

      forAll(trees) { (tree, treePath, newLiteralText, result) =>
        val action = EditLiteralAction(tree, treePath, LLam, newLiteralText)
        action.newTree shouldBe a[ExprNode]
        action.newTree shouldEqual result
      }
    }

    "throw an error when attempting to replace something other than a LiteralNode" in {
      val trees: TableFor3[ExprNodeParent, List[Int], String] = TableFor3(
        ("tree", "treePath", "newLiteralText"),
        (ExprNode.fromExpr(LLam, Num(1)), List(), "st"),
        (ExprNode.fromExpr(LLam, Plus(Num(1), Num(2))), List(0), "tj461"),
        (
          ExprNode.fromExpr(LLam, IfThenElse(Bool(true), Lambda("z", IntType(), Bool(LiteralBool(true))), Num(2))),
          List(1, 2),
          "err"
        )
      )

      forAll(trees) { (tree, treePath, newLiteralText) =>
        an[InvalidEditTargetException] should be thrownBy EditLiteralAction(tree, treePath, LLam, newLiteralText).newTree
      }
    }
  }

  "DeleteAction" should {
    "delete an expr node from a tree" in {
      val trees: TableFor3[ExprNodeParent, List[Int], ExprNodeParent] = TableFor3(
        ("tree", "treePath", "result"),
        (ExprNode.fromExpr(LLam, Num(1)), List(), ExprChoiceNode(LLam)),
        (
          ExprNode.fromExpr(LLam, Let("x", Plus(Num(54), Num(-1)), Var("x"))),
          List(1, 0),
          ExprNode.fromExpr(LLam, Let("x", Plus(BlankExprDropDown(LLam), Num(-1)), Var("x")))
        ),
        (
          ExprNode.fromExpr(LLam, Let("x", Plus(Num(54), Num(-1)), Var("x"))),
          List(1),
          ExprNode.fromExpr(LLam, Let("x", BlankExprDropDown(LLam), Var("x")))
        ),
        (
          ExprNode.fromExpr(LLam, Let("x", Plus(Num(54), Num(-1)), Var("x"))),
          List(2),
          ExprNode.fromExpr(LLam, Let("x", Plus(Num(54), Num(-1)), BlankExprDropDown(LLam)))
        ),
        (
          ExprNode.fromExpr(LLam, Apply(Lambda("x", IntType(), Plus(Var("x"), Num(1))), Num(65))),
          List(0, 2),
          ExprNode.fromExpr(LLam, Apply(Lambda("x", IntType(), BlankExprDropDown(LLam)), Num(65)))
        ),
        (
          ExprNode.fromExpr(LLam, Apply(Lambda("x", IntType(), Plus(Var("x"), Num(1))), Num(65))),
          List(1),
          ExprNode.fromExpr(LLam, Apply(Lambda("x", IntType(), Plus(Var("x"), Num(1))), BlankExprDropDown(LLam)))
        )
      )

      forAll(trees) { (tree, treePath, result) =>
        val action = DeleteAction(tree, treePath, LLam)
        action.newTree shouldEqual result
      }
    }

    "delete a type node from a tree" in {
      val equalsZeroFunction = Lambda("value", Func(IntType(), BoolType()), Equal(Var("value"), Num(0)))

      val trees: TableFor3[ExprNodeParent, List[Int], ExprNodeParent] = TableFor3(
        ("tree", "treePath", "result"),
        (
          ExprNode.fromExpr(LLam, Lambda("x", IntType(), Plus(Var("x"), Num(1)))),
          List(1),
          ExprNode.fromExpr(LLam, Lambda("x", BlankTypeDropDown(LLam), Plus(Var("x"), Num(1))))
        ),
        (
          ExprNode.fromExpr(LLam, Apply(Lambda("x", IntType(), Plus(Var("x"), Num(1))), Num(65))),
          List(0, 1),
          ExprNode.fromExpr(LLam, Apply(Lambda("x", BlankTypeDropDown(LLam), Plus(Var("x"), Num(1))), Num(65)))
        ),
        (
          ExprNode.fromExpr(LLam, equalsZeroFunction),
          List(1),
          ExprNode.fromExpr(LLam, Lambda("value", BlankTypeDropDown(LLam), Equal(Var("value"), Num(0))))
        ),
        (
          ExprNode.fromExpr(LLam, equalsZeroFunction),
          List(1, 0),
          ExprNode.fromExpr(LLam, Lambda("value", Func(BlankTypeDropDown(LLam), BoolType()), Equal(Var("value"), Num(0))))
        ),
        (
          ExprNode.fromExpr(LLam, equalsZeroFunction),
          List(1, 1),
          ExprNode.fromExpr(LLam, Lambda("value", Func(IntType(), BlankTypeDropDown(LLam)), Equal(Var("value"), Num(0))))
        )
      )

      forAll(trees) { (tree, treePath, result) =>
        val action = DeleteAction(tree, treePath, LLam)
        action.newTree shouldEqual result
      }
    }

    "throws an error when attempting to delete a literal node" in {
      val f = Lambda("x", IntType(), Plus(Var("x"), Num(1)))

      val trees: TableFor2[ExprNodeParent, List[Int]] = TableFor2(
        ("tree", "treePath"),
        (ExprNode.fromExpr(LLam, Num(1)), List(0)),
        (ExprNode.fromExpr(LLam, f), List(0)),
        (ExprNode.fromExpr(LLam, f), List(2, 0, 0)),
        (ExprNode.fromExpr(LLam, f), List(2, 1, 0))
      )

      forAll(trees) { (tree, treePath) =>
        an[InvalidDeleteTargetException] should be thrownBy DeleteAction(tree, treePath, LLam).newTree
      }
    }
  }

  "PasteAction" should {
    "correctly paste an expr node string into a tree" in {
      val trees: TableFor4[ExprNodeParent, List[Int], String, ExprNodeParent] = TableFor4(
        ("tree", "treePath", "pasteNodeString", "result"),
        (ExprChoiceNode(LLam), List(), ExprNode.fromExpr(LLam, Num(1)).toString, ExprNode.fromExpr(LLam, Num(1))),
        (
          ExprNode.fromExpr(LLam, Plus(Num(1), Num(2))),
          List(0),
          ExprNode.fromExpr(LLam, IfThenElse(Bool(true), Num(-1), Num(2))).toString,
          ExprNode.fromExpr(LLam, Plus(IfThenElse(Bool(true), Num(-1), Num(2)), Num(2)))
        ),
        (
          ExprNode.fromExpr(LLam, Plus(Num(1), IfThenElse(Bool(true), BlankExprDropDown(LLam), Bool(false)))),
          List(1, 1),
          ExprNode.fromExpr(LLam, Var("hello")).toString,
          ExprNode.fromExpr(LLam, Plus(Num(1), IfThenElse(Bool(true), Var("hello"), Bool(false))))
        ),
        (
          ExprNode.fromExpr(LLam, Plus(Num(1), Num(2))),
          List(1),
          ExprNode.fromExpr(LLam, Let("x", Num(1), Num(2))).toString,
          ExprNode.fromExpr(LLam, Plus(Num(1), Let("x", Num(1), Num(2))))
        ),
        (
          ExprNode.fromExpr(LLam, Let("x", Let("y", Num(6), Num(0)), Var("x"))),
          List(1, 2),
          ExprNode.fromExpr(LLam, Plus(Var("y"), Num(1))).toString,
          ExprNode.fromExpr(LLam, Let("x", Let("y", Num(6), Plus(Var("y"), Num(1))), Var("x")))
        )
      )

      forAll(trees) { (tree, treePath, pasteNodeString, result) =>
        val action = PasteAction(tree, treePath, LLam, pasteNodeString)
        action.newTree shouldEqual result
      }
    }

    "correctly paste a type node string into a tree" in {
      val trees: TableFor4[ExprNodeParent, List[Int], String, ExprNodeParent] = TableFor4(
        ("tree", "treePath", "pasteNodeString", "result"),
        (
          ExprNode.fromExpr(LLam, Lambda("x", IntType(), Plus(Var("x"), Num(1)))),
          List(1),
          TypeNode.fromType(LLam, BoolType()).toString,
          ExprNode.fromExpr(LLam, Lambda("x", BoolType(), Plus(Var("x"), Num(1))))
        ),
        (
          ExprNode.fromExpr(LLam, Lambda("x", Func(BlankTypeDropDown(LLam), IntType()), Plus(Var("x"), Num(1)))),
          List(1, 1),
          TypeNode.fromType(LLam, BlankTypeDropDown(LLam)).toString,
          ExprNode.fromExpr(LLam, Lambda("x", Func(BlankTypeDropDown(LLam), BlankTypeDropDown(LLam)), Plus(Var("x"), Num(1))))
        ),
        (
          ExprNode.fromExpr(LLam, Lambda("x", Func(BlankTypeDropDown(LLam), IntType()), Plus(Var("x"), Num(1)))),
          List(1),
          TypeNode.fromType(LLam, Func(Func(BoolType(), BoolType()), IntType())).toString,
          ExprNode.fromExpr(LLam, Lambda("x", Func(Func(BoolType(), BoolType()), IntType()), Plus(Var("x"), Num(1))))
        )
      )

      forAll(trees) { (tree, treePath, pasteNodeString, result) =>
        val action = PasteAction(tree, treePath, LLam, pasteNodeString)
        action.newTree shouldEqual result
      }
    }

    "throws an error when attempting to paste a type node into an expr node" in {
      val trees: TableFor3[ExprNodeParent, List[Int], String] = TableFor3(
        ("tree", "treePath", "pasteNodeString"),
        (ExprNode.fromExpr(LLam, Num(1)), List(), TypeNode.fromType(LLam, IntType()).toString),
        (ExprNode.fromExpr(LLam, Plus(Num(1), Num(2))), List(0), TypeNode.fromType(LLam, BoolType()).toString),
        (
          ExprNode.fromExpr(LLam, Lambda("x", IntType(), Plus(Var("x"), Num(1)))),
          List(2),
          TypeNode.fromType(LLam, BoolType()).toString
        ),
        (ExprNode.fromExpr(LLam, Num(1)), List(), TypeNode.fromType(LLam, BlankTypeDropDown(LLam)).toString)
      )

      forAll(trees) { (tree, treePath, pasteNodeString) =>
        an[InvalidPasteTargetException] should be thrownBy PasteAction(tree, treePath, LLam, pasteNodeString).newTree
      }
    }

    "throws an error when attempting to paste an expr node into a type node" in {
      val trees: TableFor3[ExprNodeParent, List[Int], String] = TableFor3(
        ("tree", "treePath", "pasteNodeString"),
        (
          ExprNode.fromExpr(LLam, Lambda("x", IntType(), Plus(Var("x"), Num(1)))),
          List(1),
          ExprNode.fromExpr(LLam, Num(1)).toString
        ),
        (
          ExprNode.fromExpr(LLam, Lambda("gee", Func(BoolType(), IntType()), Num(73))),
          List(1, 1),
          ExprNode.fromExpr(LLam, Bool(true)).toString
        ),
        (
          ExprNode.fromExpr(LLam, Lambda("gee", Func(BoolType(), IntType()), Num(73))),
          List(1, 0),
          ExprNode.fromExpr(LLam, BlankExprDropDown(LLam)).toString
        )
      )

      forAll(trees) { (tree, treePath, pasteNodeString) =>
        an[InvalidPasteTargetException] should be thrownBy PasteAction(tree, treePath, LLam, pasteNodeString).newTree
      }
    }

    "throws an error when attempting to paste into a literal node" in {
      val trees: TableFor3[ExprNodeParent, List[Int], String] = TableFor3(
        ("tree", "treePath", "pasteNodeString"),
        (ExprNode.fromExpr(LLam, Num(1)), List(0), ExprNode.fromExpr(LLam, Num(1)).toString),
        (ExprNode.fromExpr(LLam, Plus(Num(1), Num(2))), List(0, 0), ExprNode.fromExpr(LLam, Num(1)).toString),
        (
          ExprNode.fromExpr(LLam, Lambda("x", IntType(), Plus(Var("x"), Num(1)))),
          List(0),
          ExprNode.fromExpr(LLam, Num(1)).toString
        ),
        (
          ExprNode.fromExpr(LLam, Lambda("x", IntType(), Plus(Var("x"), Num(1)))),
          List(2, 0, 0),
          TypeNode.fromType(LLam, IntType()).toString
        )
      )

      forAll(trees) { (tree, treePath, pasteNodeString) =>
        an[InvalidPasteTargetException] should be thrownBy PasteAction(tree, treePath, LLam, pasteNodeString).newTree
      }
    }

    "throws an error when attempting to paste an invalid node string" in {
      val trees: TableFor3[ExprNodeParent, List[Int], String] = TableFor3(
        ("tree", "treePath", "pasteNodeString"),
        (ExprNode.fromExpr(LLam, Num(1)), List(0), "Num(5)"),
        (ExprNode.fromExpr(LLam, Plus(Num(1), Num(2))), List(0, 0), "ExprNode()"),
        (
          ExprNode.fromExpr(LLam, Lambda("x", IntType(), Plus(Var("x"), Num(1)))),
          List(0),
          "ExprNode(\"Num\", List())"
        ),
        (ExprNode.fromExpr(LLam, Lambda("x", IntType(), Plus(Var("x"), Num(1)))), List(1), "TypeNode(BoolType(), List())")
      )

      forAll(trees) { (tree, treePath, pasteNodeString) =>
        an[Exception] should be thrownBy PasteAction(tree, treePath, LLam, pasteNodeString).newTree
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
        "ExprNode",
        "SubExprNode(ExprChoiceNode(LLam))",
        "SubTypeNode(TypeChoiceNode())",
        "ExprNode()",
        "ExprNode('Num', List())",
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
        ("SelectExprAction", List("Equal", "foo")),
        ("SelectTypeAction", List("IntType", "foo")),
        ("EditLiteralAction", List("foo", "bar")),
        ("PasteAction", List(ExprNode.fromExpr(LLam, Num(1)).toString, "foo")),
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
