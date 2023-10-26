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
    val children = List(ConcreteNode(Num(1)), ConcreteNode(Num(2)))
    val tree = ConcreteNode(expr, children)
    children.foreach(_.parent = Some(tree))
    tree.exprName shouldEqual "Plus"
    children(0).exprName shouldEqual "Num"
    children(1).exprName shouldEqual "Num"
    children(0).treePath shouldEqual List(0)
    children(1).treePath shouldEqual List(1)
    tree.treePath shouldEqual List()
    println(tree.toHtml)
  }

  test("Can correctly represent a simple addition tree with a literal field open") {
    val expr = Times(Num(-3), Num(5))
    val concreteChild = SubExpr(ConcreteNode(Num(-3)))
    val variableChildInner = VariableNode(Num.getClass.asInstanceOf[Class[Expr]], List(LiteralNode("5")))
    val variableChild = SubExpr(variableChildInner)
    val children = List(concreteChild, variableChild)
    val tree = VariableNode(expr.getClass.asInstanceOf[Class[Expr]], children)
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
}
