package app

import app.ExpressionEvalTree
import languages.LArith
import languages.LArith.*
import org.scalatest.GivenWhenThen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

import scala.util.Random

class ExpressionTreeTest extends AnyFunSuite {
  // TODO: implement actual tests for ExpressionTreeTest

  test("ExpressionTree with just an expression can be converted to correct SVG") {
    val tree = ExpressionEvalTree(LArith, Plus(Num(1), Num(2)), None, None, Nil)
    println(tree.toSvg)
  }

  test("ExpressionTree with an expression and value can be converted to correct SVG") {
    val tree = ExpressionEvalTree(LArith, Plus(Num(1), Num(2)), Some(NumV(3)), None, Nil)
    println(tree.toSvg)
  }

  test("ExpressionTree with an expression and type can be converted to correct SVG") {
    val tree = ExpressionEvalTree(LArith, Plus(Num(1), Num(2)), None, Some(Map("x" -> NumV(5))), Nil)
    println(tree.toSvg)
  }
}