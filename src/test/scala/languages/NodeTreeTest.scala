package languages

import languages.LArith.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

class NodeTreeTest extends AnyFunSuite {
  test("Can correctly represent a complete simple addition tree") {
    val expr = Plus(Num(1), Num(2))
    val tree = VariableNode.fromExpr(expr)
    tree.exprName shouldEqual "Plus"
    tree.children.zipWithIndex.foreach { (child, i) =>
      child shouldBe a[VariableNode]
      child.asInstanceOf[VariableNode].exprName shouldEqual "Num"
      child.treePath shouldEqual List(i)
    }
    tree.treePath shouldEqual List()
  }

  test("Can correctly represent a simple arithmetic tree with a literal field open") {
    val expr = Times(Num(-3), Num(5))
    val concreteChild = SubExprNode(VariableNode(Num(-3).toString))
    val variableChildInner = VariableNode("Num", List(LiteralNode("5")))
    val variableChild = SubExprNode(variableChildInner)
    val children = List(concreteChild, variableChild)
    val tree = VariableNode("Times", children)
    children.foreach(_.node.setParent(Some(tree)))
    tree.exprName shouldEqual "Times"
    children(0).node.treePath shouldEqual List(0)
    children(1).node.treePath shouldEqual List(1)
    tree.treePath shouldEqual List()
    tree.children shouldEqual children.map(_.node)
    variableChild.children shouldEqual List(variableChildInner)
  }

  test("Can correctly represent an arithmetic tree with an unselected sub-expression") {
    // Unselected + (Times(Num(2), Unselected))
    val tree = VariableNode(
      "Plus",
      List(
        SubExprNode(ExprChoiceNode()),
        SubExprNode(
          VariableNode("Times", List(SubExprNode(VariableNode(Num(2).toString)), SubExprNode(ExprChoiceNode())))
        )
      )
    )
  }

  def correctNodeRead(node: Node): Unit = {
    val nodeString = node.toString
    val nodeRead = Node.read(nodeString).get
    nodeRead shouldEqual node
  }

  test("Can correctly read a VariableNode with 2 VariableNode children from a String") {
    val tree =
      VariableNode("Plus", List(SubExprNode(VariableNode(Num(1).toString)), SubExprNode(VariableNode(Num(2).toString))))
    correctNodeRead(tree)
  }

  test(
    "Can correctly read a VariableNode with a VariableNode and a VariableNode with a ExprChoiceNode child from a String"
  ) {
    val tree = VariableNode(
      "Plus",
      List(
        SubExprNode(VariableNode(Num(1).toString)),
        SubExprNode(VariableNode("Times", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode()))))
      )
    )
    correctNodeRead(tree)
  }

  test("Can correctly read a VariableNode with a ExprChoiceNode") {
    val tree = VariableNode("Plus", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))
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
      read.getParent shouldEqual original.getParent
      if (!isRoot) {
        read.getParent shouldNot be(None)
      }
      read.children.zip(original.children).foreach { case (readChild, originalChild) =>
        checkParents(originalChild, readChild, isRoot = false)
      }
    }

    checkParents(node, nodeRead, isRoot = true)
  }

  def checkAllChildrenHaveCorrectParent(n: Node): Unit = {
    n.children.foreach { child =>
      child.getParent shouldEqual Some(n)
      checkAllChildrenHaveCorrectParent(child)
    }
  }

  test("Created Node trees have correct parents") {
    val tree = VariableNode(
      "Plus",
      List(
        SubExprNode(VariableNode(Num(1).toString)),
        SubExprNode(VariableNode("Times", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode()))))
      )
    )
    checkAllChildrenHaveCorrectParent(tree)
  }

  test("Can correctly read the parents of a Node tree") {
    val tree = VariableNode(
      "Plus",
      List(
        SubExprNode(VariableNode(Num(1).toString)),
        SubExprNode(
          VariableNode(
            "Times",
            List(
              SubExprNode(ExprChoiceNode()),
              SubExprNode(
                VariableNode(
                  "Plus",
                  List(SubExprNode(ExprChoiceNode()), SubExprNode(VariableNode("Num", List(LiteralNode("2")))))
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
        SubExprNode(VariableNode(Num(1).toString)),
        SubExprNode(
          VariableNode(
            "Times",
            List(
              SubExprNode(ExprChoiceNode()),
              SubExprNode(
                VariableNode(
                  "Plus",
                  List(SubExprNode(ExprChoiceNode()), SubExprNode(VariableNode("Num", List(LiteralNode("2")))))
                )
              )
            )
          )
        )
      )
    )
    val updated = originalTree.replace(List(1, 1, 0), VariableNode.createFromExprName("Num"))
    updated shouldEqual VariableNode(
      "Plus",
      List(
        SubExprNode(VariableNode(Num(1).toString)),
        SubExprNode(
          VariableNode(
            "Times",
            List(
              SubExprNode(ExprChoiceNode()),
              SubExprNode(
                VariableNode(
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

  test("Correctly read expression from VariableNode with all children completed") {
    val tree1 = VariableNode(
      "Plus",
      List(
        SubExprNode(VariableNode("Num", List(LiteralNode("75")))),
        SubExprNode(
          VariableNode(
            "Times",
            List(
              SubExprNode(VariableNode("Num", List(LiteralNode("3")))),
              SubExprNode(
                VariableNode(
                  "Plus",
                  List(
                    SubExprNode(VariableNode("Num", List(LiteralNode("2")))),
                    SubExprNode(VariableNode("Num", List(LiteralNode("3"))))
                  )
                )
              )
            )
          )
        )
      )
    )
    tree1.getExpr shouldEqual Plus(Num(75), Times(Num(3), Plus(Num(2), Num(3))))
  }

  test("Correctly read expression from VariableNode with incomplete or incorrect literal values") {
    val tree1 = VariableNode(
      "Plus",
      List(
        SubExprNode(VariableNode("Num", List(LiteralNode("")))),
        SubExprNode(VariableNode("Num", List(LiteralNode("\"Hello!\""))))
      )
    )
    tree1.getExpr shouldEqual Plus(Num(LiteralAny("")), Num(LiteralString("Hello!")))

    val tree2 = VariableNode(
      "Times",
      List(
        SubExprNode(VariableNode("Num", List(LiteralNode("true")))),
        SubExprNode(
          VariableNode(
            "Plus",
            List(
              SubExprNode(VariableNode("Num", List(LiteralNode("2")))),
              SubExprNode(VariableNode("Num", List(LiteralNode("\"3\""))))
            )
          )
        )
      )
    )
    tree2.getExpr shouldEqual Times(Num(LiteralBool(true)), Plus(Num(LiteralInt(2)), Num(LiteralString("3"))))
  }

  test("Nested TypeNodes have correct tree paths") {
    val tree = LLam.VariableNode(
      "Lambda",
      List(
        LLam.LiteralNode("x"),
        LLam.SubTypeNode(
          LLam.TypeNode(
            "Func",
            List(LLam.SubTypeNode(LLam.TypeNode("IntType", Nil)), LLam.SubTypeNode(LLam.TypeNode("BoolType", Nil)))
          )
        ),
        LLam.SubExprNode(LLam.VariableNode("Var", List(LLam.LiteralNode("x"))))
      )
    )

    tree.findChild(List(1)) shouldEqual Some(
      LLam.TypeNode(
        "Func",
        List(LLam.SubTypeNode(LLam.TypeNode("IntType", Nil)), LLam.SubTypeNode(LLam.TypeNode("BoolType", Nil)))
      )
    )
    tree.findChild(List(1)).get.treePath shouldEqual List(1)

    tree.findChild(List(1, 0)) shouldEqual Some(LLam.TypeNode("IntType", Nil))
    tree.findChild(List(1, 0)).get.treePath shouldEqual List(1, 0)
    tree.findChild(List(1, 1)) shouldEqual Some(LLam.TypeNode("BoolType", Nil))
    tree.findChild(List(1, 0)).get.treePath shouldEqual List(1, 0)
  }
}
