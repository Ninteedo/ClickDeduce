package languages

import languages.LIf.*
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1, TableFor3}
import org.scalatest.propspec.AnyPropSpec

import scala.util.Random

class LIfTest extends AnyPropSpec with TableDrivenPropertyChecks with GivenWhenThen {
  Random.setSeed(2024)

  def genRandBool(): Boolean = Random.nextBoolean()

  def genRandInt(): BigInt = Random.nextInt(200) - 100

  val bools: TableFor1[Boolean] = Table("bool", true, false)
  val expressions: TableFor1[Expr] = Table(
    "expressions",
    Num(1),
    Bool(true),
    Bool(false),
    Plus(Num(1), Num(2)),
    Eq(Num(1), Num(2)),
    IfThenElse(Bool(true), Num(1), Num(2)),
    IfThenElse(Bool(false), Num(1), Num(2)),
    IfThenElse(Eq(Num(1), Num(2)), Num(1), Num(2)),
    IfThenElse(Eq(Num(1), Num(1)), IfThenElse(Bool(false), Num(5), Plus(Num(1), Num(-1))), Num(2)),
  )
  val newExprClasses: TableFor1[String] = Table(
    "newExprClasses",
    "Bool",
    "Eq",
    "IfThenElse"
  )

  property("Bool type-checks to BoolType") {
    forAll(bools) { b =>
      Bool(b).typeCheck(Map()) shouldEqual BoolType()
    }
  }

  property("Bool correctly evaluates to BoolV") {
    forAll(bools) { b =>
      Bool(b).eval(Map()) shouldEqual BoolV(b)
    }
  }

  property("BoolV's type is BoolType") {
    forAll(bools) { b =>
      BoolV(b).typ shouldEqual BoolType()
    }
  }

  property("IfThenElse returns then_expr when cond is true") {
    IfThenElse(Bool(true), Num(1), Num(2)).eval(Map()) shouldEqual NumV(1)
    IfThenElse(Bool(true), Bool(true), Bool(false)).eval(Map()) shouldEqual BoolV(true)
  }

  property("IfThenElse returns else_expr when cond is false") {
    IfThenElse(Bool(false), Num(1), Num(2)).eval(Map()) shouldEqual NumV(2)
    IfThenElse(Bool(false), Bool(true), Bool(false)).eval(Map()) shouldEqual BoolV(false)
  }

  property("IfThenElse correctly type-checks when both branches have the same type") {
    IfThenElse(Bool(true), Num(1), Num(2)).typeCheck(Map()) shouldEqual IntType()
    IfThenElse(Bool(true), Bool(true), Bool(false)).typeCheck(Map()) shouldEqual BoolType()
  }

  property("IfThenElse type-checks to an error when the branches have different types") {
    IfThenElse(Bool(true), Num(1), Bool(false)).typeCheck(Map()) shouldBe a[TypeError]
    IfThenElse(Bool(true), Bool(true), Num(2)).typeCheck(Map()) shouldBe a[TypeError]
  }

  property("Eq type-checks to BoolType when both sides have the same type") {
    Eq(Num(1), Num(2)).typeCheck(Map()) shouldEqual BoolType()
    Eq(Bool(true), Bool(false)).typeCheck(Map()) shouldEqual BoolType()
    Eq(Bool(true), Bool(true)).typeCheck(Map()) shouldEqual BoolType()
    Eq(Num(1), Num(1)).typeCheck(Map()) shouldEqual BoolType()
  }

  property("Eq type-checks to an error when the sides have different types") {
    Eq(Num(1), Bool(false)).typeCheck(Map()) shouldBe a[TypeError]
    Eq(Bool(true), Num(2)).typeCheck(Map()) shouldBe a[TypeError]
  }

  property("Eq correctly evaluates to BoolV") {
    Eq(Num(1), Num(2)).eval(Map()) shouldEqual BoolV(false)
    Eq(Bool(true), Bool(false)).eval(Map()) shouldEqual BoolV(false)
    Eq(Bool(true), Bool(true)).eval(Map()) shouldEqual BoolV(true)
    Eq(Num(1), Num(1)).eval(Map()) shouldEqual BoolV(true)
  }

  property("Can correctly load expressions in LIf") {
    forAll(expressions) { e =>
      readExpr(e.toString).get shouldEqual e
    }
  }

  property("Can create VariableNode for expression kinds in LIf") {
    forAll(newExprClasses) { c =>
      VariableNode.createFromExprName(c) shouldBe a[VariableNode]
    }
  }

  property("IfThenElse.getEvalChildren returns the appropriate children") {
    val exampleEnv: Env = Map("a" -> NumV(1), "b" -> NumV(2), "c" -> NumV(3))
    val ifThenElse = IfThenElse(Bool(true), Num(1), Num(2))
    ifThenElse.getChildrenEval(exampleEnv) shouldEqual List((Bool(true), exampleEnv), (Num(1), exampleEnv))

    val ifThenElse2 = IfThenElse(Bool(false), Num(1), Num(2))
    ifThenElse2.getChildrenEval(exampleEnv) shouldEqual List((Bool(false), exampleEnv), (Num(2), exampleEnv))
  }

  property("IfThenElse behaviour is correct when using actions") {
    val initialTree = ExprChoiceNode()

    val selectIfThenElseAction = createAction("SelectExprAction", initialTree.toString, "", List("IfThenElse"))
    selectIfThenElseAction.newTree shouldEqual VariableNode(
      "IfThenElse", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode()))
    )

    val selectCondAction = createAction("SelectExprAction", selectIfThenElseAction.newTree.toString, "0", List("Bool"))
    selectCondAction.newTree shouldEqual VariableNode(
      "IfThenElse", List(
        SubExprNode(VariableNode("Bool", List(LiteralNode("")))), SubExprNode(ExprChoiceNode()),
        SubExprNode(ExprChoiceNode())
      )
    )

    val enterBoolAction = createAction("EditLiteralAction", selectCondAction.newTree.toString, "0-0", List("true"))
    enterBoolAction.newTree shouldEqual VariableNode(
      "IfThenElse", List(
        SubExprNode(VariableNode("Bool", List(LiteralNode("true")))), SubExprNode(ExprChoiceNode()),
        SubExprNode(ExprChoiceNode())
      )
    )

    val selectThenExprAction = createAction("SelectExprAction", enterBoolAction.newTree.toString, "1", List("Num"))
    selectThenExprAction.newTree shouldEqual VariableNode(
      "IfThenElse", List(
        SubExprNode(VariableNode("Bool", List(LiteralNode("true")))),
        SubExprNode(VariableNode("Num", List(LiteralNode("")))),
        SubExprNode(ExprChoiceNode())
      )
    )

    val enterThenExprAction = createAction("EditLiteralAction", selectThenExprAction.newTree.toString, "1-0", List("1"))
    enterThenExprAction.newTree shouldEqual VariableNode(
      "IfThenElse", List(
        SubExprNode(VariableNode("Bool", List(LiteralNode("true")))),
        SubExprNode(VariableNode("Num", List(LiteralNode("1")))),
        SubExprNode(ExprChoiceNode())
      )
    )

    val selectElseExprAction = createAction("SelectExprAction", enterThenExprAction.newTree.toString, "2", List("Num"))
    selectElseExprAction.newTree shouldEqual VariableNode(
      "IfThenElse", List(
        SubExprNode(VariableNode("Bool", List(LiteralNode("true")))),
        SubExprNode(VariableNode("Num", List(LiteralNode("1")))),
        SubExprNode(VariableNode("Num", List(LiteralNode(""))))
      )
    )

    val enterElseExprAction = createAction("EditLiteralAction", selectElseExprAction.newTree.toString, "2-0", List("2"))
    enterElseExprAction.newTree shouldEqual VariableNode(
      "IfThenElse", List(
        SubExprNode(VariableNode("Bool", List(LiteralNode("true")))),
        SubExprNode(VariableNode("Num", List(LiteralNode("1")))),
        SubExprNode(VariableNode("Num", List(LiteralNode("2"))))
      )
    )

    enterElseExprAction.newTree match {
      case n: VariableNode => n.getExpr shouldEqual IfThenElse(Bool(true), Num(1), Num(2))
    }
  }

  property("IfThenElse tree can be converted to HTML without error") {
    val tree = VariableNode(
      "IfThenElse", List(
        SubExprNode(VariableNode("Bool", List(LiteralNode("true")))),
        SubExprNode(VariableNode("Num", List(LiteralNode("1")))),
        SubExprNode(VariableNode("Num", List(LiteralNode("2"))))
      )
    )

    tree.toHtml(DisplayMode.Edit)
    tree.toHtml(DisplayMode.TypeCheck)
    tree.toHtml(DisplayMode.Evaluation)
  }
}
