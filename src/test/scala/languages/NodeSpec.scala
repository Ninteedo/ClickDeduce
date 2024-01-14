package languages

import languages.LArith.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatest.wordspec.AnyWordSpec

class NodeSpec extends AnyWordSpec with Matchers with TableDrivenPropertyChecks {
  "Node children should" should {
    "be none for root node" in {
      val node = VariableNode("Test", Nil)
      node.getParent shouldBe None
    }

    "be the root node for first level children" in {
      val nodes = TableFor1(
        "node",
        VariableNode("Test", List(SubExprNode(VariableNode("Test", Nil)), SubExprNode(VariableNode("Test", Nil)))),
        VariableNode("Test", List(LiteralNode("foo"), SubExprNode(ExprChoiceNode())))
      )

      forAll(nodes) { node =>
        node.children.foreach(_.getParent shouldBe Some(node))
      }
    }

    "be the first level children for the second level children" in {
      val nodes = TableFor1(
        "node",
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
        )
      )

      forAll(nodes) { node =>
        node.children.foreach(firstLevel =>
          firstLevel.children.foreach(secondLevel => secondLevel.getParent shouldBe Some(firstLevel))
        )
      }
    }
  }
}
