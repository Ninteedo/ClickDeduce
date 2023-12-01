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
      typeOf(Bool(b)) shouldEqual BoolType()
    }
  }

  property("Bool correctly evaluates to BoolV") {
    forAll(bools) { b =>
      eval(Bool(b)) shouldEqual BoolV(b)
    }
  }

  property("BoolV's type is BoolType") {
    forAll(bools) { b =>
      BoolV(b).typ shouldEqual BoolType()
    }
  }

  property("IfThenElse returns then_expr when cond is true") {
    eval(IfThenElse(Bool(true), Num(1), Num(2))) shouldEqual NumV(1)
    eval(IfThenElse(Bool(true), Bool(true), Bool(false))) shouldEqual BoolV(true)
  }

  property("IfThenElse returns else_expr when cond is false") {
    eval(IfThenElse(Bool(false), Num(1), Num(2))) shouldEqual NumV(2)
    eval(IfThenElse(Bool(false), Bool(true), Bool(false))) shouldEqual BoolV(false)
  }

  property("IfThenElse correctly type-checks when both branches have the same type") {
    typeOf(IfThenElse(Bool(true), Num(1), Num(2))) shouldEqual IntType()
    typeOf(IfThenElse(Bool(true), Bool(true), Bool(false))) shouldEqual BoolType()
  }

  property("IfThenElse type-checks to an error when the branches have different types") {
    typeOf(IfThenElse(Bool(true), Num(1), Bool(false))) shouldBe a[TypeError]
    typeOf(IfThenElse(Bool(true), Bool(true), Num(2))) shouldBe a[TypeError]
  }

  property("Eq type-checks to BoolType when both sides have the same type") {
    typeOf(Eq(Num(1), Num(2))) shouldEqual BoolType()
    typeOf(Eq(Bool(true), Bool(false))) shouldEqual BoolType()
    typeOf(Eq(Bool(true), Bool(true))) shouldEqual BoolType()
    typeOf(Eq(Num(1), Num(1))) shouldEqual BoolType()
  }

  property("Eq type-checks to an error when the sides have different types") {
    typeOf(Eq(Num(1), Bool(false))) shouldBe a[TypeError]
    typeOf(Eq(Bool(true), Num(2))) shouldBe a[TypeError]
  }

  property("Eq correctly evaluates to BoolV") {
    eval(Eq(Num(1), Num(2))) shouldEqual BoolV(false)
    eval(Eq(Bool(true), Bool(false))) shouldEqual BoolV(false)
    eval(Eq(Bool(true), Bool(true))) shouldEqual BoolV(true)
    eval(Eq(Num(1), Num(1))) shouldEqual BoolV(true)
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
    val ifThenElse = IfThenElse(Bool(true), Num(1), Num(2))
    ifThenElse.getChildrenEval(Map()) shouldEqual List((Bool(true), Map()), (Num(1), Map()))

    val ifThenElse2 = IfThenElse(Bool(false), Num(1), Num(2))
    ifThenElse2.getChildrenEval(Map()) shouldEqual List((Bool(false), Map()), (Num(2), Map()))
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
