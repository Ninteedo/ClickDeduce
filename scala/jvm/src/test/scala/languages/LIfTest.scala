package languages

import convertors.{DisplayMode, HTMLConvertor}
import languages.LIf.*
import languages.env.*
import languages.terms.*
import languages.terms.builders.*
import languages.terms.errors.*
import languages.terms.exprs.Expr
import languages.terms.literals.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.prop.{TableFor1, TableFor2}
import org.scalatest.propspec.AnyPropSpec

import scala.util.Random

class LIfTest extends TestTemplate {
  def genRandBool(): Boolean = Random.nextBoolean()

  def genRandInt(): BigInt = Random.nextInt(200) - 100

  val expressions: TableFor1[Expr] = Table(
    "expressions",
    Num(1),
    Bool(true),
    Bool(false),
    Plus(Num(1), Num(2)),
    Equal(Num(1), Num(2)),
    IfThenElse(Bool(true), Num(1), Num(2)),
    IfThenElse(Bool(false), Num(1), Num(2)),
    IfThenElse(Equal(Num(1), Num(2)), Num(1), Num(2)),
    IfThenElse(Equal(Num(1), Num(1)), IfThenElse(Bool(false), Num(5), Plus(Num(1), Num(-1))), Num(2))
  )
  val newExprClasses: TableFor1[String] = Table("newExprClasses", "Bool", "Equal", "IfThenElse")

  testExpression(
    "Bool",
    createExprTable(List(Bool(true), Bool(false)), List(BoolV(true), BoolV(false)), List(BoolType(), BoolType()))
  )

  property("BoolV's type is BoolType") {
    forAll(bools) { b =>
      BoolV(b).typ shouldEqual BoolType()
    }
  }

  testExpression(
    "Basic IfThenElse expressions",
    createExprTable(
      (IfThenElse(Bool(true), Num(1), Num(2)), NumV(1), IntType()),
      (IfThenElse(Bool(true), Bool(true), Bool(false)), BoolV(true), BoolType()),
      (IfThenElse(Bool(false), Num(1), Num(2)), NumV(2), IntType()),
      (IfThenElse(Bool(false), Bool(true), Bool(false)), BoolV(false), BoolType())
    )
  )

  property("IfThenElse correctly type-checks when both branches have the same type") {
    IfThenElse(Bool(true), Num(1), Num(2)).typeCheck() shouldEqual IntType()
    IfThenElse(Bool(true), Bool(true), Bool(false)).typeCheck() shouldEqual BoolType()
  }

  property("IfThenElse type-checks to an error when the branches have different types") {
    IfThenElse(Bool(true), Num(1), Bool(false)).typeCheck() shouldBe a[TypeError]
    IfThenElse(Bool(true), Bool(true), Num(2)).typeCheck() shouldBe a[TypeError]
  }

  property("IfThenElse type-checks to an error when the condition is not a BoolType") {
    IfThenElse(Num(1), Num(1), Num(2)).typeCheck() shouldBe a[TypeMismatchType]
    IfThenElse(Plus(Num(1), Num(2)), Bool(true), Bool(false)).typeCheck() shouldBe a[TypeMismatchType]
    IfThenElse(IfThenElse(Bool(true), Num(1), Num(0)), Num(1), Num(2)).typeCheck() shouldBe a[TypeMismatchType]
  }

  property("Eq type-checks to BoolType when both sides have the same type") {
    Equal(Num(1), Num(2)).typeCheck() shouldEqual BoolType()
    Equal(Bool(true), Bool(false)).typeCheck() shouldEqual BoolType()
    Equal(Bool(true), Bool(true)).typeCheck() shouldEqual BoolType()
    Equal(Num(1), Num(1)).typeCheck() shouldEqual BoolType()
  }

  property("Eq type-checks to an error when the sides have different types") {
    Equal(Num(1), Bool(false)).typeCheck() shouldBe a[TypeMismatchType]
    Equal(Bool(true), Num(2)).typeCheck() shouldBe a[TypeMismatchType]
  }

  property("Eq evaluates to an error when the sides have different types") {
    Equal(Num(1), Bool(false)).eval() shouldBe a[TypeMismatchError]
    Equal(Bool(true), Num(2)).eval() shouldBe a[TypeMismatchError]
  }

  testExpression(
    "LessThan",
    Table(
      testExpressionTableHeading,
      (LessThan(Num(1), Num(2)), BoolV(true), BoolType()),
      (LessThan(Num(2), Num(1)), BoolV(false), BoolType()),
      (LessThan(Num(1), Num(1)), BoolV(false), BoolType()),
      (LessThan(Num(-100), Num(3)), BoolV(true), BoolType()),
      (LessThan(Bool(true), Bool(false)), ComparisonWithNonOrdinalError(BoolType(), BoolType()), ComparisonWithNonOrdinalType(BoolType(), BoolType())),
      (LessThan(Num(-1), Bool(true)), ComparisonWithNonOrdinalError(IntType(), BoolType()), ComparisonWithNonOrdinalType(IntType(), BoolType())),
      (LessThan(Bool(false), Num(1)), ComparisonWithNonOrdinalError(BoolType(), IntType()), ComparisonWithNonOrdinalType(BoolType(), IntType()))
    )
  )

  testExpression(
    "Basic Eq expression",
    createExprTable(
      (Equal(Num(1), Num(2)), BoolV(false), BoolType()),
      (Equal(Bool(true), Bool(false)), BoolV(false), BoolType()),
      (Equal(Bool(true), Bool(true)), BoolV(true), BoolType()),
      (Equal(Num(1), Num(1)), BoolV(true), BoolType())
    )
  )

  property("Can correctly load expressions in LIf") {
    forAll(expressions) { e =>
      readExpr(e.toString).get shouldEqual e
    }
  }

  property("Can create VariableNode for expression kinds in LIf") {
    forAll(newExprClasses) { c =>
      VariableNode.createFromExprName(c).get shouldBe a[VariableNode]
    }
  }

  property("IfThenElse.getChildrenEval returns the appropriate children") {
    val exampleEnv: ValueEnv = Env("a" -> NumV(1), "b" -> NumV(2), "c" -> NumV(3))
    val ifThenElseTable: TableFor2[Expr, List[(Expr, ValueEnv)]] = Table(
      ("expr", "children"),
      (IfThenElse(Bool(true), Num(1), Num(2)), List((Bool(true), exampleEnv), (Num(1), exampleEnv))),
      (IfThenElse(Bool(false), Num(1), Num(2)), List((Bool(false), exampleEnv), (Num(2), exampleEnv))),
      (IfThenElse(Equal(Num(1), Num(2)), Num(1), Num(2)), List((Equal(Num(1), Num(2)), exampleEnv), (Num(2), exampleEnv))),
      (IfThenElse(Num(5), Num(1), Num(2)), List((Num(5), exampleEnv), (Num(1), exampleEnv), (Num(2), exampleEnv)))
    )

    forAll(ifThenElseTable) { (expr, children) =>
      expr.getChildrenEval(exampleEnv) shouldEqual children
    }
  }

  property("IfThenElse behaviour is correct when using actions") {
    val initialTree = ExprChoiceNode()

    val selectIfThenElseAction = createAction("SelectExprAction", initialTree.toString, "", List("IfThenElse"))
    selectIfThenElseAction.newTree shouldEqual VariableNode(
      "IfThenElse",
      List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode()))
    )

    val selectCondAction = createAction("SelectExprAction", selectIfThenElseAction.newTree.toString, "0", List("Bool"))
    selectCondAction.newTree shouldEqual VariableNode(
      "IfThenElse",
      List(
        SubExprNode(VariableNode("Bool", List(LiteralNode(LiteralBool(false))))),
        SubExprNode(ExprChoiceNode()),
        SubExprNode(ExprChoiceNode())
      )
    )

    val enterBoolAction = createAction("EditLiteralAction", selectCondAction.newTree.toString, "0-0", List("true"))
    enterBoolAction.newTree shouldEqual VariableNode(
      "IfThenElse",
      List(
        SubExprNode(VariableNode("Bool", List(LiteralNode(LiteralBool(true))))),
        SubExprNode(ExprChoiceNode()),
        SubExprNode(ExprChoiceNode())
      )
    )

    val selectThenExprAction = createAction("SelectExprAction", enterBoolAction.newTree.toString, "1", List("Num"))
    selectThenExprAction.newTree shouldEqual VariableNode(
      "IfThenElse",
      List(
        SubExprNode(VariableNode("Bool", List(LiteralNode(LiteralBool(true))))),
        SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(0))))),
        SubExprNode(ExprChoiceNode())
      )
    )

    val enterThenExprAction = createAction("EditLiteralAction", selectThenExprAction.newTree.toString, "1-0", List("1"))
    enterThenExprAction.newTree shouldEqual VariableNode(
      "IfThenElse",
      List(
        SubExprNode(VariableNode("Bool", List(LiteralNode(LiteralBool(true))))),
        SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(1))))),
        SubExprNode(ExprChoiceNode())
      )
    )

    val selectElseExprAction = createAction("SelectExprAction", enterThenExprAction.newTree.toString, "2", List("Num"))
    selectElseExprAction.newTree shouldEqual VariableNode(
      "IfThenElse",
      List(
        SubExprNode(VariableNode("Bool", List(LiteralNode(LiteralBool(true))))),
        SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(1))))),
        SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(0)))))
      )
    )

    val enterElseExprAction = createAction("EditLiteralAction", selectElseExprAction.newTree.toString, "2-0", List("2"))
    enterElseExprAction.newTree shouldEqual VariableNode(
      "IfThenElse",
      List(
        SubExprNode(VariableNode("Bool", List(LiteralNode(LiteralBool(true))))),
        SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(1))))),
        SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(2)))))
      )
    )

    enterElseExprAction.newTree match {
      case n: VariableNode => n.getExpr shouldEqual IfThenElse(Bool(true), Num(1), Num(2))
    }
  }

  property("IfThenElse tree can be converted to HTML without error") {
    val tree = VariableNode(
      "IfThenElse",
      List(
        SubExprNode(VariableNode("Bool", List(LiteralNode(LiteralBool(true))))),
        SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(1))))),
        SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(2)))))
      )
    )

    HTMLConvertor(LIf, DisplayMode.Edit).convert(tree)
    HTMLConvertor(LIf, DisplayMode.TypeCheck).convert(tree)
    HTMLConvertor(LIf, DisplayMode.Evaluation).convert(tree)
  }

  property("Plus and Times return an error when given an argument other than a NumV") {
    Plus(Bool(true), Bool(false)).eval() shouldBe an[UnexpectedArgValue]
    Plus(Num(5), Bool(true)).eval() shouldBe an[UnexpectedArgValue]
    Plus(Bool(false), Num(5)).eval() shouldBe an[UnexpectedArgValue]
    Times(Bool(false), Bool(true)).eval() shouldBe an[UnexpectedArgValue]
    Times(Num(5), Bool(true)).eval() shouldBe an[UnexpectedArgValue]
    Times(Bool(false), Num(5)).eval() shouldBe an[UnexpectedArgValue]

    Plus(Bool(false), Bool(true)).typeCheck() shouldBe an[UnexpectedArgType]
    Plus(Num(5), Bool(true)).typeCheck() shouldBe an[UnexpectedArgType]
    Plus(Bool(false), Num(5)).typeCheck() shouldBe an[UnexpectedArgType]
    Times(Bool(false), Bool(true)).typeCheck() shouldBe an[UnexpectedArgType]
    Times(Num(5), Bool(true)).typeCheck() shouldBe an[UnexpectedArgType]
    Times(Bool(false), Num(5)).typeCheck() shouldBe an[UnexpectedArgType]
  }

  property("Bool pretty prints correctly") {
    Bool(true).prettyPrint shouldEqual "true"
    Bool(false).prettyPrint shouldEqual "false"
  }

  property("Eq pretty prints correctly") {
    Equal(Num(1), Num(2)).prettyPrint shouldEqual "1 = 2"
    Equal(Bool(true), Bool(false)).prettyPrint shouldEqual "true = false"
    Equal(Equal(Num(1), Num(2)), Equal(Num(3), Num(4))).prettyPrint shouldEqual "(1 = 2) = (3 = 4)"
    Equal(Plus(Num(1), Num(2)), Num(3)).prettyPrint shouldEqual "(1 + 2) = 3"
  }

  property("IfThenElse pretty prints correctly") {
    IfThenElse(Bool(true), Num(1), Num(2)).prettyPrint shouldEqual "if true then 1 else 2"
    IfThenElse(Equal(Num(1), Num(2)), Num(1), Num(2)).prettyPrint shouldEqual "if (1 = 2) then 1 else 2"
    IfThenElse(Bool(true), IfThenElse(Bool(false), Num(1), Num(2)), Num(3)).prettyPrint shouldEqual
      "if true then (if false then 1 else 2) else 3"
  }

  property("BoolType pretty prints correctly") {
    BoolType().prettyPrint shouldEqual "Bool"
  }

  property("BoolV pretty prints correctly") {
    BoolV(true).prettyPrint shouldEqual "true"
    BoolV(false).prettyPrint shouldEqual "false"
  }
}
