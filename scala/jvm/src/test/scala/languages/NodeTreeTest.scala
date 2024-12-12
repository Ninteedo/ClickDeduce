package languages

import languages.LArith.*
import languages.terms.*
import languages.terms.literals.*
import nodes.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

class NodeTreeTest extends AnyFunSuite {
  test("Can correctly represent a complete simple addition tree") {
    val expr = Plus(Num(1), Num(2))
    val tree = ExprNode.fromExpr(LArith, expr)
    tree.exprName shouldEqual "Plus"
    tree.children.zipWithIndex.foreach { (child, i) =>
      child shouldBe a[ExprNode]
      child.asInstanceOf[ExprNode].exprName shouldEqual "Num"
      child.treePath shouldEqual List(i)
    }
    tree.treePath shouldEqual List()
  }

  test("Can correctly represent a simple arithmetic tree with a literal field open") {
    val expr = Times(Num(-3), Num(5))
    val concreteChild = SubExprNode(ExprNode(LArith, Num(-3).toString))
    val variableChildInner = ExprNode(LArith, "Num", List(LiteralNode(LiteralInt(5))))
    val variableChild = SubExprNode(variableChildInner)
    val children = List(concreteChild, variableChild)
    val tree = ExprNode(LArith, "Times", children)
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
    val tree = ExprNode(LArith,
      "Plus",
      List(
        SubExprNode(ExprChoiceNode(LArith)),
        SubExprNode(
          ExprNode(LArith, "Times", List(SubExprNode(ExprNode(LArith, Num(2).toString)), SubExprNode(ExprChoiceNode(LArith))))
        )
      )
    )
  }

  def correctNodeRead(node: Node): Unit = {
    val nodeString = node.toString
    val nodeRead = Node.read(LArith, nodeString).get
    nodeRead shouldEqual node
  }

  test("Can correctly read a ExprNode with 2 ExprNode children from a String") {
    val tree =
      ExprNode(LArith, "Plus", List(SubExprNode(ExprNode(LArith, Num(1).toString)), SubExprNode(ExprNode(LArith, Num(2).toString))))
    correctNodeRead(tree)
  }

  test(
    "Can correctly read a ExprNode with a ExprNode and a ExprNode with a ExprChoiceNode child from a String"
  ) {
    val tree = ExprNode(LArith,
      "Plus",
      List(
        SubExprNode(ExprNode(LArith, Num(1).toString)),
        SubExprNode(ExprNode(LArith, "Times", List(SubExprNode(ExprChoiceNode(LArith)), SubExprNode(ExprChoiceNode(LArith)))))
      )
    )
    correctNodeRead(tree)
  }

  test("Can correctly read a ExprNode with a ExprChoiceNode") {
    val tree = ExprNode(LArith, "Plus", List(SubExprNode(ExprChoiceNode(LArith)), SubExprNode(ExprChoiceNode(LArith))))
    correctNodeRead(tree)
  }

  test("Can correctly read a ExprNode with a LiteralNode") {
    val tree1 = ExprNode(LArith, "Num", List(LiteralNode(LiteralInt(0))))
    correctNodeRead(tree1)
    val tree2 = ExprNode(LArith, "Num", List(LiteralNode(LiteralInt(1))))
    correctNodeRead(tree2)
  }

  def correctReadParentsCheck(node: Node): Unit = {
    val nodeString = node.toString
    val nodeRead = Node.read(LArith, nodeString).get
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
    val tree = ExprNode(LArith,
      "Plus",
      List(
        SubExprNode(ExprNode(LArith, Num(1).toString)),
        SubExprNode(ExprNode(LArith, "Times", List(SubExprNode(ExprChoiceNode(LArith)), SubExprNode(ExprChoiceNode(LArith)))))
      )
    )
    checkAllChildrenHaveCorrectParent(tree)
  }

  test("Can correctly read the parents of a Node tree") {
    val tree = ExprNode(LArith,
      "Plus",
      List(
        SubExprNode(ExprNode(LArith, Num(1).toString)),
        SubExprNode(
          ExprNode(LArith,
            "Times",
            List(
              SubExprNode(ExprChoiceNode(LArith)),
              SubExprNode(
                ExprNode(LArith,
                  "Plus",
                  List(SubExprNode(ExprChoiceNode(LArith)), SubExprNode(ExprNode(LArith, "Num", List(LiteralNode(LiteralInt(2))))))
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
    val originalTree = ExprNode(LArith,
      "Plus",
      List(
        SubExprNode(ExprNode(LArith, Num(1).toString)),
        SubExprNode(
          ExprNode(LArith,
            "Times",
            List(
              SubExprNode(ExprChoiceNode(LArith)),
              SubExprNode(
                ExprNode(LArith,
                  "Plus",
                  List(SubExprNode(ExprChoiceNode(LArith)), SubExprNode(ExprNode(LArith, "Num", List(LiteralNode(LiteralInt(2))))))
                )
              )
            )
          )
        )
      )
    )
    val updated = originalTree.replace(List(1, 1, 0), ExprNode.createFromExprName(LArith, "Num").get)
    updated shouldEqual ExprNode(LArith,
      "Plus",
      List(
        SubExprNode(ExprNode(LArith, Num(1).toString)),
        SubExprNode(
          ExprNode(LArith,
            "Times",
            List(
              SubExprNode(ExprChoiceNode(LArith)),
              SubExprNode(
                ExprNode(LArith,
                  "Plus",
                  List(
                    SubExprNode(ExprNode(LArith, "Num", List(LiteralNode(LiteralInt(0))))),
                    SubExprNode(ExprNode(LArith, "Num", List(LiteralNode(LiteralInt(2)))))
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

  test("Correctly read expression from ExprNode with all children completed") {
    val tree1 = ExprNode(LArith,
      "Plus",
      List(
        SubExprNode(ExprNode(LArith, "Num", List(LiteralNode(LiteralInt(75))))),
        SubExprNode(
          ExprNode(LArith,
            "Times",
            List(
              SubExprNode(ExprNode(LArith, "Num", List(LiteralNode(LiteralInt(3))))),
              SubExprNode(
                ExprNode(LArith,
                  "Plus",
                  List(
                    SubExprNode(ExprNode(LArith, "Num", List(LiteralNode(LiteralInt(2))))),
                    SubExprNode(ExprNode(LArith, "Num", List(LiteralNode(LiteralInt(3)))))
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

//  test("Correctly read expression from ExprNode with incomplete or incorrect literal values") {
//    val tree1 = ExprNode(LArith,
//      "Plus",
//      List(
//        SubExprNode(ExprNode(LArith, "Num", List(LiteralNode("")))),
//        SubExprNode(ExprNode(LArith, "Num", List(LiteralNode("\"Hello!\""))))
//      )
//    )
//    tree1.getExpr shouldEqual Plus(Num(LiteralAny("")), Num(LiteralString("Hello!")))
//
//    val tree2 = ExprNode(LArith,
//      "Times",
//      List(
//        SubExprNode(ExprNode(LArith, "Num", List(LiteralNode("true")))),
//        SubExprNode(
//          ExprNode(LArith,
//            "Plus",
//            List(
//              SubExprNode(ExprNode(LArith, "Num", List(LiteralNode("2")))),
//              SubExprNode(ExprNode(LArith, "Num", List(LiteralNode("\"3\""))))
//            )
//          )
//        )
//      )
//    )
//    tree2.getExpr shouldEqual Times(Num(LiteralBool(true)), Plus(Num(LiteralInt(2)), Num(LiteralString("3"))))
//  }

  test("Nested TypeNodes have correct tree paths") {
    val tree = ExprNode(LLam,
      "Lambda",
      List(
        LiteralNode(LiteralIdentifierLookup("x")),
        SubTypeNode(
          TypeNode(
            LLam,
            "Func",
            List(SubTypeNode(TypeNode(LLam, "IntType", Nil)), SubTypeNode(TypeNode(LLam, "BoolType", Nil)))
          )
        ),
        SubExprNode(ExprNode(LLam, "Var", List(LiteralNode(LiteralIdentifierLookup("x")))))
      )
    )

    tree.findChild(List(1)) shouldEqual Some(
      TypeNode(
        LLam,
        "Func",
        List(SubTypeNode(TypeNode(LLam, "IntType", Nil)), SubTypeNode(TypeNode(LLam, "BoolType", Nil)))
      )
    )
    tree.findChild(List(1)).get.treePath shouldEqual List(1)

    tree.findChild(List(1, 0)) shouldEqual Some(TypeNode(LLam, "IntType", Nil))
    tree.findChild(List(1, 0)).get.treePath shouldEqual List(1, 0)
    tree.findChild(List(1, 1)) shouldEqual Some(TypeNode(LLam, "BoolType", Nil))
    tree.findChild(List(1, 0)).get.treePath shouldEqual List(1, 0)
  }
}
