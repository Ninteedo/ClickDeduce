package languages

import languages.LLam.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatest.wordspec.AnyWordSpec

class NodeSpec extends AnyWordSpec with Matchers with TableDrivenPropertyChecks {
  "Node parent" should {
    val nodes: TableFor1[Node] = TableFor1(
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
      TypeNode.fromType(Func(BlankTypeDropDown(), BlankTypeDropDown())),
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
}
