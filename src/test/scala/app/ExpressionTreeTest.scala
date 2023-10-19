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
    val expressions = children.map(_.expr)
    expressions should be (List(Num(1), Num(2)))
  }

  test("ExpressionTree width matches expected text width when it has no children") {
    val expr = Plus(Num(1), Num(2))
    val tree = ExpressionEvalTree(Plus(Num(1), Num(2)), None, None, Nil)
    val width = tree.treeSize._1
    width should be (FontWidthCalculator.calculateWidth(prettyPrint(expr), tree.FONT))
  }

  test("ExpressionTree width does not exceed expected text width when it has children") {
    val expr = Plus(Num(1), Num(2))
    val children = List(ExpressionEvalTree(Num(1), Some(NumV(1)), None, Nil), ExpressionEvalTree(Num(2), Some(NumV(2)), None, Nil))
    val tree = ExpressionEvalTree(Plus(Num(1), Num(2)), Some(NumV(3)), None, children)
    val wholeWidth = tree.treeSize._1
    val childrenWidth = children.map(_.treeSize._1).sum + tree.GROUP_X_GAP

    wholeWidth should be <= childrenWidth
  }
}
