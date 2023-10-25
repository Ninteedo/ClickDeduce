package app

import languages.LArith
import languages.LArith.*
import org.scalatest.GivenWhenThen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

import scala.util.Random

class ExpressionTreeTest extends AnyFunSuite {
  // TODO: implement actual tests for ExpressionTreeTest

  test("ExpressionTree with just an expression can be converted to correct SVG") {
    val tree = ExpressionEvalTree(Plus(Num(1), Num(2)), None, None, Nil)
    println(tree.toSvg)
  }

  test("ExpressionTree with an expression and value can be converted to correct SVG") {
    val children = List(ExpressionEvalTree(Num(1), Some(NumV(1)), None, Nil), ExpressionEvalTree(Num(2), Some(NumV(2)), None, Nil))
    val tree = ExpressionEvalTree(Plus(Num(1), Num(2)), Some(NumV(3)), None, children)
    println(tree.toSvg)
  }

  test("ExpressionTree with an expression and type can be converted to correct SVG") {
    val tree = ExpressionEvalTree(Plus(Num(1), Num(2)), None, Some(Map("x" -> NumV(5))), Nil)
    println(tree.toSvg)
  }

  test("ExpressionTree can find child expressions") {
    val expr = Plus(Num(1), Num(2))
    val tree = ExpressionEvalTree.exprToTree(expr)
    val children = tree.children
    children should have size 2
    // children should all be of type ExpressionEvalTree
    children(0) shouldBe a [ExpressionEvalTree]
    children(1) shouldBe a [ExpressionEvalTree]
    // children should have correct expressions
    children(0).asInstanceOf[ExpressionEvalTree].expr should be (Num(1))
    children(1).asInstanceOf[ExpressionEvalTree].expr should be (Num(2))
  }

  test("ExpressionTree width does not exceed expected text width when it has children") {
    val expr = Plus(Num(1), Num(2))
    val children = List(ExpressionEvalTree(Num(1), Some(NumV(1)), None, Nil), ExpressionEvalTree(Num(2), Some(NumV(2)), None, Nil))
    val tree = ExpressionEvalTree(Plus(Num(1), Num(2)), Some(NumV(3)), None, children)
    val wholeWidth = tree.treeSvgSize._1
    val childrenWidth = children.map(_.treeSvgSize._1).sum + tree.GROUP_X_GAP

    wholeWidth should be <= childrenWidth
  }

  test("ExpressionTree children paths are correct") {
    val expr = Plus(Plus(Plus(Num(1), Plus(Num(2), Num(3))), Num(4)), Num(5))
    val tree = ExpressionEvalTree.exprToTree(expr)

    tree.treePath should be (Nil)
    tree.children(0).treePath should be (List(0))
    tree.children(0).children(0).treePath should be (List(0, 0))
    tree.children(0).children(1).treePath should be (List(0, 1))
    tree.children(1).treePath should be (List(1))
  }

  test("Can create an Expr with blank arguments") {
    createUnfilledExpr("Plus") should be (Plus(BlankChildPlaceholder(), BlankChildPlaceholder()))
    createUnfilledExpr("Num") should be (Num(BlankLiteral()))
  }
}
