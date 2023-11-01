package languages

import languages.LArith
import languages.LArith.*
import org.scalatest.GivenWhenThen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

import scala.util.Random

class NodeTreeTest extends AnyFunSuite {
  test("Can correctly represent a complete simple addition tree") {
    val expr = Plus(Num(1), Num(2))
    val children = List(ConcreteNode(Num(1).toString), ConcreteNode(Num(2).toString))
    val args = children.map(SubExprNode(_))
    val tree = ConcreteNode(expr.toString, args)
    children.foreach(_.parent = Some(tree))
    tree.exprName shouldEqual "Plus"
    children(0).exprName shouldEqual "Num"
    children(1).exprName shouldEqual "Num"
    children(0).treePath shouldEqual List(0)
    children(1).treePath shouldEqual List(1)
    tree.treePath shouldEqual List()
    println(tree.toHtml)
  }

  test("Can correctly represent a simple arithmetic tree with a literal field open") {
    val expr = Times(Num(-3), Num(5))
    val concreteChild = SubExprNode(ConcreteNode(Num(-3).toString))
    val variableChildInner = VariableNode("Num", List(LiteralNode("5")))
    val variableChild = SubExprNode(variableChildInner)
    val children = List(concreteChild, variableChild)
    val tree = VariableNode("Times", children)
    children.foreach(_.node.parent = Some(tree))
    tree.exprName shouldEqual "Times"
    //    children(0).node.exprName shouldBe "Num"
    //    children(1).node.exprName shouldBe "Num"
    children(0).node.treePath shouldEqual List(0)
    children(1).node.treePath shouldEqual List(1)
    tree.treePath shouldEqual List()
    tree.children shouldEqual children.map(_.node)
    variableChild.children shouldEqual List(variableChildInner)
    //    tree.toHtml should include (concreteChild.toHtmlLine)
    //    tree.toHtml should include (variableChild.toHtmlLine)
    println(tree.toHtml)
  }

  test("Can correctly represent an arithmetic tree with an unselected sub-expression") {
    // Unselected + (Times(Num(2), Unselected))
    val tree = VariableNode("Plus", List(
      SubExprNode(ExprChoiceNode()),
      SubExprNode(VariableNode("Times", List(
        SubExprNode(ConcreteNode(Num(2).toString)),
        SubExprNode(ExprChoiceNode())
      )))
    )
    )
    //    tree.exprName shouldEqual "Plus"
    println(tree.toHtml)
  }

  def correctNodeRead(node: Node): Unit = {
    val nodeString = node.toString
    val nodeRead = Node.read(nodeString).get
    nodeRead shouldEqual node
  }

  test("Can correctly read a VariableNode with 2 ConcreteNode children from a String") {
    val tree = VariableNode("Plus", List(
      SubExprNode(ConcreteNode(Num(1).toString)),
      SubExprNode(ConcreteNode(Num(2).toString))
    ))
    correctNodeRead(tree)
  }

  test("Can correctly read a VariableNode with a ConcreteNode and a VariableNode with a ExprChoiceNode child from a String") {
    val tree = VariableNode("Plus", List(
      SubExprNode(ConcreteNode(Num(1).toString)),
      SubExprNode(VariableNode("Times", List(
        SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())
      )))
    ))
    correctNodeRead(tree)
  }

  test("Can correctly read a VariableNode with a ExprChoiceNode") {
    val tree = VariableNode("Plus", List(
      SubExprNode(ExprChoiceNode()),
      SubExprNode(ExprChoiceNode())
    ))
    correctNodeRead(tree)
  }

  test("Can correctly read a VariableNode with a LiteralNode") {
    val tree1 = VariableNode("Num", List(LiteralNode("")))
    correctNodeRead(tree1)
    val tree2 = VariableNode("Num", List(LiteralNode("1")))
    correctNodeRead(tree2)
  }

  def correctReadParentsCheck(node: Node): Unit = {
    val nodeString = node.toString
    val nodeRead = Node.read(nodeString).get
    nodeRead shouldEqual node

    def checkParents(original: Node, read: Node, isRoot: Boolean): Unit = {
      read.parent shouldEqual original.parent
      if (!isRoot) {
        read.parent shouldNot be(None)
      }
      read.children.zip(original.children).foreach { case (readChild, originalChild) =>
        checkParents(originalChild, readChild, isRoot = false)
      }
    }

    checkParents(node, nodeRead, isRoot = true)
  }

  def checkAllChildrenHaveCorrectParent(n: Node): Unit = {
    n.children.foreach { child =>
      child.parent shouldEqual Some(n)
      checkAllChildrenHaveCorrectParent(child)
    }
  }

  test("Created Node trees have correct parents") {
    val tree = VariableNode(
      "Plus",
      List(
        SubExprNode(ConcreteNode(Num(1).toString)),
        SubExprNode(VariableNode("Times", List(
          SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())
        )))
      )
    )
    checkAllChildrenHaveCorrectParent(tree)
  }

  test("Can correctly read the parents of a Node tree") {
    val tree = VariableNode(
      "Plus",
      List(
        SubExprNode(ConcreteNode(Num(1).toString)),
        SubExprNode(VariableNode("Times", List(
          SubExprNode(ExprChoiceNode()),
          SubExprNode(VariableNode(
            "Plus",
            List(
              SubExprNode(ExprChoiceNode()),
              SubExprNode(VariableNode("Num", List(LiteralNode("2"))))
            )
          )
          )
        )
        )
        )
      )
    )
    correctReadParentsCheck(tree)
  }

  test("Can correctly replace a ExprChoiceNode") {
    val originalTree = VariableNode(
      "Plus",
      List(
        SubExprNode(ConcreteNode(Num(1).toString)),
        SubExprNode(VariableNode("Times", List(
          SubExprNode(ExprChoiceNode()),
          SubExprNode(VariableNode(
            "Plus",
            List(
              SubExprNode(ExprChoiceNode()),
              SubExprNode(VariableNode("Num", List(LiteralNode("2"))))
            )
          )
          )
        )
        )
        )
      )
    )
    val updated = originalTree.insertExpr("Num", List(1, 1, 0))
    updated shouldEqual VariableNode(
      "Plus",
      List(
        SubExprNode(ConcreteNode(Num(1).toString)),
        SubExprNode(VariableNode("Times", List(
          SubExprNode(ExprChoiceNode()),
          SubExprNode(VariableNode(
            "Plus",
            List(
              SubExprNode(VariableNode("Num", List(LiteralNode("")))),
              SubExprNode(VariableNode("Num", List(LiteralNode("2"))))
            )
          )
          )
        )
        )
        )
      )
    )
  }

  // TODO: test that a tree with a string literal works

  test("Can create Actions using createAction") {
    val tree1 = VariableNode(
      "Plus",
      List(
        SubExprNode(ConcreteNode(Num(1).toString)),
        SubExprNode(VariableNode("Times", List(
          SubExprNode(ExprChoiceNode()),
          SubExprNode(VariableNode(
            "Plus",
            List(
              SubExprNode(ExprChoiceNode()),
              SubExprNode(VariableNode("Num", List(LiteralNode("2"))))
            )
          )
          )
        )
        )
        )
      )
    )
    val action1 = createAction("SelectExprAction", tree1.toString, tree1.children(1).children(0).treePathString, List("Num"))
    action1.newTree shouldEqual VariableNode(
      "Plus",
      List(
        SubExprNode(ConcreteNode(Num(1).toString)),
        SubExprNode(VariableNode("Times", List(
          SubExprNode(VariableNode("Num", List(LiteralNode("")))),
          SubExprNode(VariableNode(
            "Plus",
            List(
              SubExprNode(ExprChoiceNode()),
              SubExprNode(VariableNode("Num", List(LiteralNode("2"))))
            )
          )
          )
        )
        )
        )
      )
    )

    val tree2 = ExprChoiceNode()
    val action2 = createAction("SelectExprAction", tree2.toString, "", List("Plus"))
    action2.newTree shouldEqual VariableNode(
      "Plus",
      List(
        SubExprNode(ExprChoiceNode()),
        SubExprNode(ExprChoiceNode())
      )
    )
  }
}
