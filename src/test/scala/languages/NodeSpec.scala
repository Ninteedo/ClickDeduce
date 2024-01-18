package languages

import languages.LLam.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatest.wordspec.AnyWordSpec

class NodeSpec extends AnyWordSpec with Matchers with TableDrivenPropertyChecks {
  "Node parent" should {
    val nodes: TableFor1[OuterNode] = TableFor1(
      "node",
      VariableNode("Test", Nil),
      VariableNode("Test", List(SubExprNode(VariableNode("Test", Nil)), SubExprNode(VariableNode("Test", Nil)))),
      VariableNode("Test", List(LiteralNode("foo"), SubExprNode(ExprChoiceNode()))),
      VariableNode(
        "Root",
        List(
          SubExprNode(
            VariableNode(
              "Level1",
              List(SubExprNode(VariableNode("Level2", Nil)), SubExprNode(VariableNode("Level2", Nil)))
            )
          )
        )
      ),
      VariableNode.fromExpr(Plus(Times(Num(5), Var("a")), IfThenElse(Bool(true), Num(5), Num(6)))),
      VariableNode.fromExpr(Let("x", Plus(Var("y"), Num(-1)), Plus(Var("x"), Var("y")))),
      VariableNode.fromExpr(
        Lambda("foo", Func(Func(IntType(), IntType()), BoolType()), Eq(Apply(Var("foo"), Num(1)), Num(0)))
      ),
      VariableNode.fromExpr(BlankExprDropDown()),
      VariableNode.fromExpr(Plus(BlankExprDropDown(), BlankExprDropDown())),
      TypeNode.fromType(IntType()),
      TypeNode.fromType(Func(IntType(), BoolType())),
      TypeNode.fromType(Func(Func(IntType(), IntType()), BoolType())),
      TypeNode.fromType(Func(Func(Func(IntType(), IntType()), Func(IntType(), BoolType())), BoolType())),
      TypeNode.fromType(BlankTypeDropDown()),
      TypeNode.fromType(Func(BlankTypeDropDown(), BlankTypeDropDown()))
    )

    "be none for root node" in {
      forAll(nodes) { node =>
        node.getParent shouldBe None
      }
    }

    "be the root node for first level children" in {
      forAll(nodes) { node =>
        node.children.foreach(_.getParent shouldBe Some(node))
      }
    }

    "be the first level children for the second level children" in {
      forAll(nodes) { node =>
        node.children.foreach(firstLevel =>
          firstLevel.children.foreach(secondLevel => secondLevel.getParent shouldBe Some(firstLevel))
        )
      }
    }

    "be the second level children for the third level children" in {
      forAll(nodes) { node =>
        node.children.foreach(firstLevel =>
          firstLevel.children.foreach(secondLevel =>
            secondLevel.children.foreach(thirdLevel => thirdLevel.getParent shouldBe Some(secondLevel))
          )
        )
      }
    }
  }

  "SubExprNode" should {
    "correctly return its parent" in {
      val node = VariableNode(
        "Plus",
        List(SubExprNode(ExprChoiceNode()), SubExprNode(VariableNode("Num", List(LiteralNode("5")))))
      )
      node.args.head.getParent shouldBe Some(node)
      node.args(1).getParent shouldBe Some(node)
    }

    "not be able to have a TypeNode as a parent" in {
      a[NodeParentWrongTypeException] should be thrownBy
        SubExprNode(ExprChoiceNode()).setParent(Some(TypeNode.fromType(IntType())))

      a[NodeParentWrongTypeException] should be thrownBy
        TypeNode("Func", List(SubTypeNode(TypeNode("Int", Nil)), SubExprNode(ExprChoiceNode())))
    }

    "cannot be a root node" in {
      a[InnerNodeCannotBeRootException] should be thrownBy SubExprNode(ExprChoiceNode()).setParent(None)
    }
  }

  "TypeNode" should {
    "convert to HTML without error with multiple levels" in {
      forAll(Table("mode", DisplayMode.values: _*)) { mode =>
        noException should be thrownBy TypeNode
          .fromType(Func(Func(Func(IntType(), IntType()), Func(IntType(), BoolType())), BoolType()))
          .toHtml(mode)
      }
    }

    "be able to parse types from strings" in {
      val types =
        Table("type", IntType(), BoolType(), Func(IntType(), IntType()), Func(Func(IntType(), IntType()), BoolType()))

      forAll(types) { t =>
        TypeNode.fromType(t).getType shouldBe t
        readType(t.toString) shouldBe Some(t)
      }
    }
  }

  "Tree paths" should {
    "return the correct child" in {
      val node =
        VariableNode.fromExpr(Apply(Lambda("x", IntType(), IfThenElse(Eq(Var("x"), Num(0)), Num(1), Num(0))), Num(5)))
      node.findChild(List()) shouldBe Some(node)
      node.findChild(List(0)) shouldBe Some(
        VariableNode.fromExpr(Lambda("x", IntType(), IfThenElse(Eq(Var("x"), Num(0)), Num(1), Num(0))))
      )
      node.findChild(List(1)) shouldBe Some(VariableNode.fromExpr(Num(5)))
      node.findChild(List(0, 0)) shouldBe Some(LiteralNode("x"))
      node.findChild(List(0, 1)) shouldBe Some(TypeNode.fromType(IntType()))
      node.findChild(List(0, 2)) shouldBe Some(VariableNode.fromExpr(IfThenElse(Eq(Var("x"), Num(0)), Num(1), Num(0))))
      node.findChild(List(0, 2, 0)) shouldBe Some(VariableNode.fromExpr(Eq(Var("x"), Num(0))))
      node.findChild(List(0, 2, 1)) shouldBe Some(VariableNode.fromExpr(Num(1)))
      node.findChild(List(0, 2, 2)) shouldBe Some(VariableNode.fromExpr(Num(0)))
    }

    "error on invalid paths" in {
      val node =
        VariableNode.fromExpr(Apply(Lambda("x", IntType(), IfThenElse(Eq(Var("x"), Num(0)), Num(1), Num(0))), Num(5)))

      val invalidPaths = Table(
        "path",
        List(-1),
        List(2),
        List(0, -1),
        List(0, 3),
        List(0, 1, -1),
        List(0, 1, 0),
        List(0, 1, 1),
        List(0, 2, -1),
        List(0, 2, 3),
        List(0, 2, 0, -1),
        List(0, 2, 0, 3),
        List(0, 2, 1, -1),
        List(0, 2, 1, 2),
        List(0, 2, 2, -1)
      )

      forAll(invalidPaths) { path =>
        an[InvalidTreePathException] should be thrownBy node.findChild(path)
      }
    }

    "string is correctly interpreted" in {
      val paths = Table(
        ("string", "path"),
        ("", Some(List())),
        ("0", Some(List(0))),
        ("0-1", Some(List(0, 1))),
        ("0-1-2", Some(List(0, 1, 2))),
        ("1-0", Some(List(1, 0))),
        ("65-1-56897", Some(List(65, 1, 56897))),
        ("x", None),
        ("1.56", None),
        ("1_4", None),
        ("0-e", None),
      )

      forAll(paths) { (string, path) =>
        path match {
          case Some(p) => Node.readPathString(string) shouldBe p
          case None    => an[InvalidTreePathStringException] should be thrownBy Node.readPathString(string)
        }
      }
    }
  }
}
