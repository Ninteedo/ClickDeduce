package languages

import actions.{EditLiteralAction, SelectExprAction}
import convertors.DisplayMode
import languages.LLet.*
import languages.env.*
import languages.terms.*
import languages.terms.builders.*
import languages.terms.errors.*
import languages.terms.literals.*
import languages.terms.values.Value
import nodes.*
import org.scalatest.matchers.should.Matchers.{a, an, shouldBe, shouldEqual}
import org.scalatest.prop.TableFor1
import org.scalatest.propspec.AnyPropSpec

import scala.util.Random

class LLetTest extends TestTemplate {
  val assortedValues: TableFor1[Value] = Table("value", intValues.map(NumV.apply) ++ bools.map(BoolV.apply): _*)

  def randomElement[A](l: List[A]): A = l(Random.nextInt(l.length))

  property("Var correctly type-checks with simple environment") {
    forAll(assortedValues) { value =>
      {
        val v = randomElement(variableNames)
        Var(v).typeCheck(Env(v -> value.typ)) shouldEqual value.typ
      }
    }
  }

  property("Var correctly type-checks with big environment") {
    forAll(assortedValues) { value =>
      {
        var env: TypeEnv = Env()
        for (i <- 0 until Math.min(assortedValues.length, variableNames.length)) {
          val v = randomElement(variableNames)
          val value = randomElement(assortedValues.toList)
          env += v -> value.typ
        }
        val v: Variable = randomElement(env.keys.toList)
        Var(v).typeCheck(env) shouldEqual env.get(v).get
      }
    }
  }

  property("Var correctly evaluates with simple environment") {
    forAll(assortedValues) { value =>
      {
        val v = randomElement(variableNames)
        Var(v).eval(Env(v -> value)) shouldEqual value
      }
    }
  }

  property("Var correctly evaluates with big environment") {
    forAll(assortedValues) { value =>
      {
        var env: ValueEnv = Env()
        for (i <- 0 until Math.min(assortedValues.length, variableNames.length)) {
          val v = randomElement(variableNames)
          val value = randomElement(assortedValues.toList)
          env += v -> value
        }
        val v: Variable = randomElement(env.keys.toList)
        Var(v).eval(env) shouldEqual env.get(v).get
      }
    }
  }

  testExpression(
    "Let with single Let in expression",
    createExprTable(
      (Let("x", Bool(false), Var("x")), BoolV(false), BoolType()),
      (Let("y", Num(51), Var("y")), NumV(51), IntType()),
      (Let("gh2", Bool(false), Equal(Var("gh2"), Bool(false))), BoolV(true), BoolType()),
      (Let("gri3hga3", Bool(true), IfThenElse(Var("gri3hga3"), Num(1), Num(2))), NumV(1), IntType()),
      (Let("iou", Plus(Num(1), Num(5)), Times(Var("iou"), Var("iou"))), NumV(36), IntType()),
      (Equal(Num(0), Let("abc", Num(3), Plus(Num(-3), Var("abc")))), BoolV(true), BoolType()),
      (Plus(Num(1), Let("x", Num(2), Plus(Var("x"), Num(3)))), NumV(6), IntType())
    )
  )

  testExpression(
    "Let with multiple Lets in expression",
    createExprTable(
      (Let("x", Num(43), Let("y", Num(12), Plus(Var("x"), Var("y")))), NumV(55), IntType()),
      (
        Let("hello", Plus(Let("world", Num(2), Times(Var("world"), Num(-1))), Num(6)), Equal(Num(4), Var("hello"))),
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
      .typeCheck(Env("y" -> IntType(), "xx" -> IntType(), "w" -> BoolType())) shouldBe an[UnknownVariableTypeError]
    Var("x").eval(Env("y" -> NumV(4), "xx" -> NumV(1), "w" -> BoolV(true))) shouldBe an[UnknownVariableEvalError]

    Plus(Var("foo"), Let("foo", Num(1), Var("foo"))).eval(Env()) shouldBe an[UnknownVariableEvalError]
  }

  property("Let behaviour is correct with actions") {
    val tree = ExprNode.createFromExprName(LLet, "Let").get
    tree.args shouldEqual List(LiteralNode(LiteralIdentifierBind("")), SubExprNode(ExprChoiceNode(LLet)), SubExprNode(ExprChoiceNode(LLet)))

    val v: Variable = "x"
    val setVarNameAction = EditLiteralAction(tree, List(0), LLet, v)
    setVarNameAction.newTree.args shouldEqual List(
      LiteralNode(LiteralIdentifierBind(v)),
      SubExprNode(ExprChoiceNode(LLet)),
      SubExprNode(ExprChoiceNode(LLet))
    )

    val assignExprChoiceAction = SelectExprAction(setVarNameAction.newTree, List(1), LLet, "Num")
    assignExprChoiceAction.newTree.args shouldEqual List(
      LiteralNode(LiteralIdentifierBind(v)),
      SubExprNode(ExprNode(LLet, "Num", List(LiteralNode(LiteralInt(0))))),
      SubExprNode(ExprChoiceNode(LLet))
    )

    val assignValue: Int = 34
    val assignExprValueAction = EditLiteralAction(assignExprChoiceAction.newTree, List(1, 0), LLet, assignValue.toString)
    assignExprValueAction.newTree.args shouldEqual List(
      LiteralNode(LiteralIdentifierBind(v)),
      SubExprNode(ExprNode(LLet, "Num", List(LiteralNode(LiteralInt(assignValue))))),
      SubExprNode(ExprChoiceNode(LLet))
    )

    val boundExprChoiceAction = SelectExprAction(assignExprValueAction.newTree, List(2), LLet, "Var")
    boundExprChoiceAction.newTree.args shouldEqual List(
      LiteralNode(LiteralIdentifierBind(v)),
      SubExprNode(ExprNode(LLet, "Num", List(LiteralNode(LiteralInt(assignValue))))),
      SubExprNode(ExprNode(LLet, "Var", List(LiteralNode(LiteralIdentifierLookup("")))))
    )

    val boundExprValueAction = EditLiteralAction(boundExprChoiceAction.newTree, List(2, 0), LLet, v)
    boundExprValueAction.newTree.args shouldEqual List(
      LiteralNode(LiteralIdentifierBind(v)),
      SubExprNode(ExprNode(LLet, "Num", List(LiteralNode(LiteralInt(assignValue))))),
      SubExprNode(ExprNode(LLet, "Var", List(LiteralNode(LiteralIdentifierLookup(v)))))
    )

    val finalTree = boundExprValueAction.newTree.asInstanceOf[ExprNode]

    finalTree.getExpr shouldEqual Let(v, Num(assignValue), Var(v))
    finalTree.getEvalEnv shouldEqual Env()
    finalTree.findChild(List(1)).get.asInstanceOf[ExprNode].getEvalEnv shouldEqual Env()
    finalTree.findChild(List(2)).get.asInstanceOf[ExprNode].getEvalEnv shouldEqual Env(v -> NumV(assignValue))
  }

  property("Invalid variable names result in an error") {
    val env: ValueEnv = Env("x" -> NumV(1), "y" -> NumV(2), "z" -> NumV(3))
    val tEnv = TypeEnv.fromValueEnv(env)
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
    val cond = ExprNode(LLet, "Bool", List(LiteralNode(LiteralBool(true))))
    val thenExpr = ExprNode(LLet, "Var", List(LiteralNode(LiteralIdentifierLookup("x"))))
    val elseExpr = ExprNode(LLet, "Var", List(LiteralNode(LiteralIdentifierLookup("x"))))
    val ifThenElseNode =
      ExprNode(LLet, "IfThenElse", List(SubExprNode(cond), SubExprNode(thenExpr), SubExprNode(elseExpr)))
    val numNode = ExprNode(LLet, "Num", List(LiteralNode(LiteralInt(1))))
    val tree = ExprNode(LLet, "Let", List(LiteralNode(LiteralIdentifierBind("x")), SubExprNode(numNode), SubExprNode(ifThenElseNode)))

    tree.getEditValueResult shouldEqual NumV(1)
    ifThenElseNode.getEditValueResult shouldEqual NumV(1)
    thenExpr.getEditValueResult shouldEqual NumV(1)
    elseExpr.getEditValueResult shouldEqual NumV(1)
  }

  property("If the assign expression in a Let is an error, then that error is returned") {
    Let("x", Equal(Num(1), Bool(false)), Var("x")).eval() shouldBe a[TypeMismatchError]
    Let("x", Equal(Num(1), Bool(false)), Var("x")).typeCheck() shouldBe a[TypeMismatchType]

    Let("x", Plus(Num(1), Bool(false)), Var("x")).eval() shouldBe a[UnexpectedArgValue]
    Let("x", Plus(Num(1), Bool(false)), Var("x")).typeCheck() shouldBe a[UnexpectedArgType]
  }

  property("Var pretty prints correctly") {
    forAll(Table("identifier", variableNames: _*)) { v =>
      Var(LiteralIdentifierLookup(v)).prettyPrint shouldEqual v
    }

    forAll(Table("identifier", invalidVariableNames: _*)) { v =>
      Var(LiteralIdentifierLookup(v)).prettyPrint shouldEqual v
    }
  }

  property("Let pretty prints correctly") {
    Let("x", Num(1), Var("x")).prettyPrint shouldEqual "let x = 1 in x"
    Let("y", Num(2), Var("y")).prettyPrint shouldEqual "let y = 2 in y"
    Let("z", Num(3), Var("z")).prettyPrint shouldEqual "let z = 3 in z"

    Let("x", Num(1), Let("y", Num(2), Plus(Var("x"), Var("y")))).prettyPrint shouldEqual
      "let x = 1 in (let y = 2 in (x + y))"
    Let("x", Bool(true), Let("y", Num(2), Plus(Var("x"), Var("y")))).prettyPrint shouldEqual
      "let x = true in (let y = 2 in (x + y))"
    Let("x", Var("y"), Let("y", Num(2), Times(Var("x"), Bool(false)))).prettyPrint shouldEqual
      "let x = y in (let y = 2 in (x × false))"

    Let("x", Let("y", Num(3), Var("y")), Var("x")).prettyPrint shouldEqual
      "let x = (let y = 3 in y) in x"
  }

  property("Let has the correct children") {
    val let = Let("x", Num(1), Var("x"))

    let.getChildrenBase() shouldEqual List(
      (LiteralIdentifierBind("x"), Env()),
      (Num(1), Env()),
      (Var("x"), Env("x" -> NumV(1)))
    )
    let.getChildrenEval() shouldEqual List((Num(1), Env()), (Var("x"), Env("x" -> NumV(1))))
    let.getChildrenTypeCheck() shouldEqual List((Num(1), Env()), (Var("x"), Env("x" -> IntType())))
  }

  property("Let with unselected expression has correct children") {
    val letNode = ExprNode(LLet, 
      "Let",
      List(LiteralNode(LiteralIdentifierBind("x")), SubExprNode(ExprNode(LLet, "Num", List(LiteralNode(LiteralInt(1))))), SubExprNode(ExprChoiceNode(LLet)))
    )

    DisplayMode.values.foreach(mode => {
      letNode.getVisibleChildren(mode) shouldEqual List(ExprNode(LLet, "Num", List(LiteralNode(LiteralInt(1)))), ExprChoiceNode(LLet))
      letNode.getVisibleChildren(mode).last match {
        case n: ExprChoiceNode =>
          n.getEnv(mode) shouldEqual (mode match {
            case DisplayMode.TypeCheck => Env("x" -> IntType())
            case _                     => Env("x" -> NumV(1))
          })
      }
    })
  }

  property("OverwriteVarTask is checked correctly") {
    val task: Task = OverwriteVarTask

    val x = "x"
    val y = "y"
    task.checkFulfilled(Let(x, Num(1), Var(x))) shouldEqual false
    task.checkFulfilled(Let(x, Num(1), Let(y, Num(2), Var(x)))) shouldEqual false
    task.checkFulfilled(Let(x, Num(1), Let(x, Num(2), Var(x)))) shouldEqual false
    task.checkFulfilled(Let(x, Num(1), Plus(Let(x, Num(2), Var(x)), Var(x)))) shouldEqual true
    task.checkFulfilled(Let(x, Num(1), Let(x, Times(Var(x), Num(2)), Var(x)))) shouldEqual true
    task.checkFulfilled(Let(x, Num(1), Let(y, Num(2), Plus(Var(x), Let(x, Num(3), Times(Var(y), Var(x))))))) shouldEqual true
  }
}
