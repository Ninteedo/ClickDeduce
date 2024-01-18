package languages

import languages.LLet.*
import org.scalatest.matchers.should.Matchers.{an, shouldBe, shouldEqual}
import org.scalatest.prop.TableFor1
import org.scalatest.propspec.AnyPropSpec

import scala.util.Random

class LLetTest extends TestTemplate[Expr, Value, Type] {
  val assortedValues: TableFor1[Value] = Table("value", intValues.map(NumV.apply) ++ bools.map(BoolV.apply): _*)

  def randomElement[A](l: List[A]): A = l(Random.nextInt(l.length))

  property("Var correctly type-checks with simple environment") {
    forAll(assortedValues) { value =>
      {
        val v = randomElement(variableNames)
        Var(v).typeCheck(Map(v -> value.typ)) shouldEqual value.typ
      }
    }
  }

  property("Var correctly type-checks with big environment") {
    forAll(assortedValues) { value =>
      {
        var env: TypeEnv = Map()
        for (i <- 0 until Math.min(assortedValues.length, variableNames.length)) {
          val v = randomElement(variableNames)
          val value = randomElement(assortedValues.toList)
          env += v -> value.typ
        }
        val v: Variable = randomElement(env.keys.toList)
        Var(v).typeCheck(env) shouldEqual env(v)
      }
    }
  }

  testExpression(
    "Let with single Let in expression",
    createExprTable(
      (Let("x", Bool(false), Var("x")), BoolV(false), BoolType()),
      (Let("y", Num(51), Var("y")), NumV(51), IntType()),
      (Let("gh2", Bool(false), Eq(Var("gh2"), Bool(false))), BoolV(true), BoolType()),
      (Let("gri3hga3", Bool(true), IfThenElse(Var("gri3hga3"), Num(1), Num(2))), NumV(1), IntType()),
      (Let("iou", Plus(Num(1), Num(5)), Times(Var("iou"), Var("iou"))), NumV(36), IntType()),
      (Eq(Num(0), Let("abc", Num(3), Plus(Num(-3), Var("abc")))), BoolV(true), BoolType()),
      (Plus(Num(1), Let("x", Num(2), Plus(Var("x"), Num(3)))), NumV(6), IntType())
    )
  )

  testExpression(
    "Let with multiple Lets in expression",
    createExprTable(
      (Let("x", Num(43), Let("y", Num(12), Plus(Var("x"), Var("y")))), NumV(55), IntType()),
      (
        Let("hello", Plus(Let("world", Num(2), Times(Var("world"), Num(-1))), Num(6)), Eq(Num(4), Var("hello"))),
        BoolV(true),
        BoolType()
      ),
      (
        Let("x", Num(1), Let("y", Num(2), Let("z", Num(3), Plus(Plus(Var("z"), Var("y")), Var("x"))))),
        NumV(6),
        IntType()
      ),
      (
        Plus(Let("x", Num(20), Plus(Var("x"), Num(1))), Let("x", Num(34), Times(Var("x"), Num(-1)))),
        NumV(-13),
        IntType()
      ),
      (Let("x", Num(1), Let("x", Num(2), Var("x"))), NumV(2), IntType())
    )
  )

  property("Var results an error when variable not found") {
    Var("x")
      .typeCheck(Map("y" -> IntType(), "xx" -> IntType(), "w" -> BoolType())) shouldBe an[UnknownVariableTypeError]
    Var("x").eval(Map("y" -> NumV(4), "xx" -> NumV(1), "w" -> BoolV(true))) shouldBe an[UnknownVariableEvalError]

    Plus(Var("foo"), Let("foo", Num(1), Var("foo"))).eval(Map()) shouldBe an[UnknownVariableEvalError]
  }

  property("Let behaviour is correct with actions") {
    val tree = VariableNode.createFromExprName("Let")
    tree.args shouldEqual List(LiteralNode(""), SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode()))

    val v: Variable = "x"
    val setVarNameAction = EditLiteralAction(tree, List(0), v)
    setVarNameAction.newTree.args shouldEqual List(
      LiteralNode(v),
      SubExprNode(ExprChoiceNode()),
      SubExprNode(ExprChoiceNode())
    )

    val assignExprChoiceAction = SelectExprAction(setVarNameAction.newTree, List(1), "Num")
    assignExprChoiceAction.newTree.args shouldEqual List(
      LiteralNode(v),
      SubExprNode(VariableNode("Num", List(LiteralNode("")))),
      SubExprNode(ExprChoiceNode())
    )

    val assignValue: Int = 34
    val assignExprValueAction = EditLiteralAction(assignExprChoiceAction.newTree, List(1, 0), assignValue.toString)
    assignExprValueAction.newTree.args shouldEqual List(
      LiteralNode(v),
      SubExprNode(VariableNode("Num", List(LiteralNode(assignValue.toString)))),
      SubExprNode(ExprChoiceNode())
    )

    val boundExprChoiceAction = SelectExprAction(assignExprValueAction.newTree, List(2), "Var")
    boundExprChoiceAction.newTree.args shouldEqual List(
      LiteralNode(v),
      SubExprNode(VariableNode("Num", List(LiteralNode(assignValue.toString)))),
      SubExprNode(VariableNode("Var", List(LiteralNode(""))))
    )

    val boundExprValueAction = EditLiteralAction(boundExprChoiceAction.newTree, List(2, 0), v)
    boundExprValueAction.newTree.args shouldEqual List(
      LiteralNode(v),
      SubExprNode(VariableNode("Num", List(LiteralNode(assignValue.toString)))),
      SubExprNode(VariableNode("Var", List(LiteralNode(v))))
    )

    val finalTree = boundExprValueAction.newTree.asInstanceOf[ExprNode]

    finalTree.getExpr shouldEqual Let(v, Num(assignValue), Var(v))
    finalTree.getEvalEnv shouldEqual Map()
    finalTree.findChild(List(1)).get.asInstanceOf[ExprNode].getEvalEnv shouldEqual Map()
    finalTree.findChild(List(2)).get.asInstanceOf[ExprNode].getEvalEnv shouldEqual Map(v -> NumV(assignValue))
  }

  property("Invalid variable names result in an error") {
    val env = Map("x" -> NumV(1), "y" -> NumV(2), "z" -> NumV(3))
    val tEnv = envToTypeEnv(env)
    forAll(Table("name", invalidVariableNames: _*)) { name =>
      val expr1 = Var(name)
      expr1.eval(env + (name -> NumV(1))) shouldBe an[InvalidIdentifierEvalError]
      expr1.typeCheck(tEnv + (name -> IntType())) shouldBe an[InvalidIdentifierTypeError]

      val expr2 = Let(name, Num(2), Num(3))
      expr2.eval(env) shouldBe an[EvalError]
      expr2.typeCheck(tEnv) shouldBe an[TypeError]
    }
  }

  property("Edit tree with bound variables is correct with IfThenElse in edit mode") {
    val cond = VariableNode("Bool", List(LiteralNode("true")))
    val thenExpr = VariableNode("Var", List(LiteralNode("x")))
    val elseExpr = VariableNode("Var", List(LiteralNode("x")))
    val ifThenElseNode =
      VariableNode("IfThenElse", List(SubExprNode(cond), SubExprNode(thenExpr), SubExprNode(elseExpr)))
    val numNode = VariableNode("Num", List(LiteralNode("1")))
    val tree = VariableNode("Let", List(LiteralNode("x"), SubExprNode(numNode), SubExprNode(ifThenElseNode)))

    tree.getEditValueResult shouldEqual NumV(1)
    ifThenElseNode.getEditValueResult shouldEqual NumV(1)
    thenExpr.getEditValueResult shouldEqual NumV(1)
    elseExpr.getEditValueResult shouldEqual NumV(1)
  }

  property("Var pretty prints correctly") {
    prettyPrint(Var("x")) shouldEqual "x"
    prettyPrint(Var("y")) shouldEqual "y"
    prettyPrint(Var("z")) shouldEqual "z"

    prettyPrint(Var(Literal.fromString("iuahg546 27__"))) shouldEqual "iuahg546 27__"
  }

  property("Let pretty prints correctly") {
    prettyPrint(Let("x", Num(1), Var("x"))) shouldEqual "let x = 1 in x"
    prettyPrint(Let("y", Num(2), Var("y"))) shouldEqual "let y = 2 in y"
    prettyPrint(Let("z", Num(3), Var("z"))) shouldEqual "let z = 3 in z"

    prettyPrint(Let("x", Num(1), Let("y", Num(2), Plus(Var("x"), Var("y"))))) shouldEqual
      "let x = 1 in let y = 2 in (x + y)"
    prettyPrint(Let("x", Bool(true), Let("y", Num(2), Plus(Var("x"), Var("y"))))) shouldEqual
      "let x = true in let y = 2 in (x + y)"
    prettyPrint(Let("x", Var("y"), Let("y", Num(2), Times(Var("x"), Bool(false))))) shouldEqual
      "let x = y in let y = 2 in (x × false)"

    prettyPrint(Let("x", Let("y", Num(3), Var("y")), Var("x"))) shouldEqual
      "let x = (let y = 3 in y) in x"
  }
}
