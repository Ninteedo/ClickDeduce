package languages

import convertors.{DisplayMode, HTMLConvertor}
import languages.LRec.*
import languages.env.*
import languages.terms.*
import languages.terms.blanks.{BlankExprDropDown, BlankTypeDropDown}
import languages.terms.builders.*
import languages.terms.errors.*
import languages.terms.literals.*
import nodes.*
import nodes.exceptions.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatest.wordspec.AnyWordSpec

class NodeSpec extends AnyWordSpec with Matchers with TableDrivenPropertyChecks {
  "Node parent" should {
    val nodes: TableFor1[OuterNode] = TableFor1(
      "node",
      ExprNode(LRec, "Test", Nil),
      ExprNode(LRec, "Test", List(SubExprNode(ExprNode(LRec, "Test", Nil)), SubExprNode(ExprNode(LRec, "Test", Nil)))),
      ExprNode(LRec, "Test", List(LiteralNode(LiteralAny("foo")), SubExprNode(ExprChoiceNode(LRec)))),
      ExprNode(LRec,
        "Root",
        List(
          SubExprNode(
            ExprNode(LRec,
              "Level1",
              List(SubExprNode(ExprNode(LRec, "Level2", Nil)), SubExprNode(ExprNode(LRec, "Level2", Nil)))
            )
          )
        )
      ),
      ExprNode.fromExpr(LRec, Plus(Times(Num(5), Var("a")), IfThenElse(Bool(true), Num(5), Num(6)))),
      ExprNode.fromExpr(LRec, Let("x", Plus(Var("y"), Num(-1)), Plus(Var("x"), Var("y")))),
      ExprNode.fromExpr(LRec,
        Lambda("foo", Func(Func(IntType(), IntType()), BoolType()), Equal(Apply(Var("foo"), Num(1)), Num(0)))
      ),
      ExprNode.fromExpr(LRec, BlankExprDropDown(LRec)),
      ExprNode.fromExpr(LRec, Plus(BlankExprDropDown(LRec), BlankExprDropDown(LRec))),
      TypeNode.fromType(LRec, IntType()),
      TypeNode.fromType(LRec, Func(IntType(), BoolType())),
      TypeNode.fromType(LRec, Func(Func(IntType(), IntType()), BoolType())),
      TypeNode.fromType(LRec, Func(Func(Func(IntType(), IntType()), Func(IntType(), BoolType())), BoolType())),
      TypeNode.fromType(LRec, BlankTypeDropDown(LRec)),
      TypeNode.fromType(LRec, Func(BlankTypeDropDown(LRec), BlankTypeDropDown(LRec)))
    )

    "be none for root node" in {
      forAll(nodes) { node =>
        node.getParent shouldBe None
      }
    }

    "be the root node for first level children" in {
      forAll(nodes) { node =>
        node.children.foreach(_.getParent shouldBe Some(node))
      }
    }

    "be the first level children for the second level children" in {
      forAll(nodes) { node =>
        node.children.foreach(firstLevel =>
          firstLevel.children.foreach(secondLevel => secondLevel.getParent shouldBe Some(firstLevel))
        )
      }
    }

    "be the second level children for the third level children" in {
      forAll(nodes) { node =>
        node.children.foreach(firstLevel =>
          firstLevel.children.foreach(secondLevel =>
            secondLevel.children.foreach(thirdLevel => thirdLevel.getParent shouldBe Some(secondLevel))
          )
        )
      }
    }
  }

  "VariableNode" should {
    "create a new node from an expression name" in {
      val cases = Table(
        ("name", "node"),
        ("Num", ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(0))))),
        ("Plus", ExprNode(LRec, "Plus", List(SubExprNode(ExprChoiceNode(LRec)), SubExprNode(ExprChoiceNode(LRec))))),
        ("Times", ExprNode(LRec, "Times", List(SubExprNode(ExprChoiceNode(LRec)), SubExprNode(ExprChoiceNode(LRec))))),
        ("Bool", ExprNode(LRec, "Bool", List(LiteralNode(LiteralBool(false))))),
        ("Var", ExprNode(LRec, "Var", List(LiteralNode(LiteralIdentifierLookup(""))))),
        ("Equal", ExprNode(LRec, "Equal", List(SubExprNode(ExprChoiceNode(LRec)), SubExprNode(ExprChoiceNode(LRec))))),
        (
          "IfThenElse",
          ExprNode(LRec,
            "IfThenElse",
            List(SubExprNode(ExprChoiceNode(LRec)), SubExprNode(ExprChoiceNode(LRec)), SubExprNode(ExprChoiceNode(LRec)))
          )
        ),
        (
          "Let",
          ExprNode(LRec, "Let", List(LiteralNode(LiteralIdentifierBind("")), SubExprNode(ExprChoiceNode(LRec)), SubExprNode(ExprChoiceNode(LRec))))
        ),
        (
          "Lambda",
          ExprNode(LRec,
            "Lambda",
            List(LiteralNode(LiteralIdentifierBind("")), SubTypeNode(TypeNode.fromType(LRec, BlankTypeDropDown(LRec))), SubExprNode(ExprChoiceNode(LRec)))
          )
        ),
        (
          "Rec",
          ExprNode(LRec,
            "Rec",
            List(
              LiteralNode(LiteralIdentifierBind("")),
              LiteralNode(LiteralIdentifierBind("")),
              SubTypeNode(TypeNode.fromType(LRec, BlankTypeDropDown(LRec))),
              SubTypeNode(TypeNode.fromType(LRec, BlankTypeDropDown(LRec))),
              SubExprNode(ExprChoiceNode(LRec))
            )
          )
        ),
        ("Apply", ExprNode(LRec, "Apply", List(SubExprNode(ExprChoiceNode(LRec)), SubExprNode(ExprChoiceNode(LRec)))))
      )

      forAll(cases) { (name, node) =>
        ExprNode.createFromExprName(LRec, name) shouldBe Some(node)
      }
    }

    "correctly convert from an expression (without types)" in {
      val cases = Table(
        ("expr", "node"),
        (Num(5), ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(5))))),
        (Var("x"), ExprNode(LRec, "Var", List(LiteralNode(LiteralIdentifierLookup("x"))))),
        (
          Plus(Num(5), Num(6)),
          ExprNode(LRec,
            "Plus",
            List(
              SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(5))))),
              SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(6)))))
            )
          )
        ),
        (
          Plus(Times(Num(1), Num(2)), Num(53)),
          ExprNode(LRec,
            "Plus",
            List(
              SubExprNode(
                ExprNode(LRec,
                  "Times",
                  List(
                    SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(1))))),
                    SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(2)))))
                  )
                )
              ),
              SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(53)))))
            )
          )
        ),
        (BlankExprDropDown(LRec), ExprChoiceNode(LRec)),
        (
          Plus(BlankExprDropDown(LRec), BlankExprDropDown(LRec)),
          ExprNode(LRec, "Plus", List(SubExprNode(ExprChoiceNode(LRec)), SubExprNode(ExprChoiceNode(LRec))))
        ),
        (
          IfThenElse(Equal(Num(65), Num(0)), Num(1), Num(0)),
          ExprNode(LRec,
            "IfThenElse",
            List(
              SubExprNode(
                ExprNode(LRec,
                  "Equal",
                  List(
                    SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(65))))),
                    SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(0)))))
                  )
                )
              ),
              SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(1))))),
              SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(0)))))
            )
          )
        ),
        (
          Let(LiteralIdentifierBind("5"), Num(5), Bool(false)),
          ExprNode(LRec,
            "Let",
            List(
              LiteralNode(LiteralIdentifierBind("5")),
              SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(5))))),
              SubExprNode(ExprNode(LRec, "Bool", List(LiteralNode(LiteralBool(false)))))
            )
          )
        )
      )

      forAll(cases) { (expr, node) =>
        ExprNode.fromExpr(LRec, expr) shouldBe node
      }
    }

    "correctly convert from an expression (with types)" in {
      val cases = Table(
        ("expr", "node"),
        (
          Lambda("x", IntType(), Var("x")),
          ExprNode(LRec,
            "Lambda",
            List(
              LiteralNode(LiteralIdentifierBind("x")),
              SubTypeNode(TypeNode.fromType(LRec, IntType())),
              SubExprNode(ExprNode(LRec, "Var", List(LiteralNode(LiteralIdentifierLookup("x")))))
            )
          )
        ),
        (
          Lambda("int", Func(IntType(), IntType()), Apply(Var("int"), Num(5))),
          ExprNode(LRec,
            "Lambda",
            List(
              LiteralNode(LiteralIdentifierBind("int")),
              SubTypeNode(TypeNode.fromType(LRec, Func(IntType(), IntType()))),
              SubExprNode(
                ExprNode(LRec,
                  "Apply",
                  List(
                    SubExprNode(ExprNode(LRec, "Var", List(LiteralNode(LiteralIdentifierLookup("int"))))),
                    SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(5)))))
                  )
                )
              )
            )
          )
        ),
        (
          Rec("rec", "x", Func(IntType(), IntType()), IntType(), Var("x")),
          ExprNode(LRec,
            "Rec",
            List(
              LiteralNode(LiteralIdentifierBind("rec")),
              LiteralNode(LiteralIdentifierBind("x")),
              SubTypeNode(TypeNode.fromType(LRec, Func(IntType(), IntType()))),
              SubTypeNode(TypeNode.fromType(LRec, IntType())),
              SubExprNode(ExprNode(LRec, "Var", List(LiteralNode(LiteralIdentifierLookup("x")))))
            )
          )
        )
      )

      forAll(cases) { (expr, node) =>
        ExprNode.fromExpr(LRec, expr) shouldBe node
      }
    }

    "correctly return its expression" in {
      val cases = Table(
        ("node", "expr"),
        (ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(5)))), Num(5)),
        (ExprNode(LRec, "Var", List(LiteralNode(LiteralIdentifierLookup("x")))), Var("x")),
        (
          ExprNode(LRec,
            "Plus",
            List(
              SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(5))))),
              SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(6)))))
            )
          ),
          Plus(Num(5), Num(6))
        ),
        (
          ExprNode(LRec,
            "Plus",
            List(
              SubExprNode(
                ExprNode(LRec,
                  "Times",
                  List(
                    SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(1))))),
                    SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(2)))))
                  )
                )
              ),
              SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(53)))))
            )
          ),
          Plus(Times(Num(1), Num(2)), Num(53))
        ),
        (
          ExprNode(LRec,
            "Lambda",
            List(
              LiteralNode(LiteralIdentifierBind("z")),
              SubTypeNode(TypeNode.fromType(LRec, IntType())),
              SubExprNode(ExprNode(LRec, "Var", List(LiteralNode(LiteralIdentifierLookup("z")))))
            )
          ),
          Lambda("z", IntType(), Var("z"))
        ),
        (
          ExprNode(LRec,
            "IfThenElse",
            List(
              SubExprNode(ExprChoiceNode(LRec)),
              SubExprNode(ExprNode(LRec, "Var", List(LiteralNode(LiteralIdentifierLookup("bar"))))),
              SubExprNode(ExprNode(LRec, "Bool", List(LiteralNode(LiteralBool(true)))))
            )
          ),
          IfThenElse(BlankExprDropDown(LRec), Var("bar"), Bool(true))
        )
      )

      forAll(cases) { (node, expr) =>
        node.getExpr shouldBe expr
      }
    }
  }

  "SubExprNode" should {
    "correctly return its parent" in {
      val node = ExprNode(LRec,
        "Plus",
        List(SubExprNode(ExprChoiceNode(LRec)), SubExprNode(ExprNode(LRec, "Num", List(LiteralNode(LiteralInt(5))))))
      )
      node.args.head.getParent shouldBe Some(node)
      node.args(1).getParent shouldBe Some(node)
    }

    "not be able to have a TypeNode as a parent" in {
      a[NodeParentWrongTypeException] should be thrownBy
        SubExprNode(ExprChoiceNode(LRec)).setParent(Some(TypeNode.fromType(LRec, IntType())))

      a[NodeParentWrongTypeException] should be thrownBy
        TypeNode(LRec, "Func", List(SubTypeNode(TypeNode(LRec, "Int", Nil)), SubExprNode(ExprChoiceNode(LRec))))
    }

    "cannot be a root node" in {
      a[InnerNodeCannotBeRootException] should be thrownBy SubExprNode(ExprChoiceNode(LRec)).setParent(None)
    }
  }

  "TypeNode" should {
    "convert to HTML without error with multiple levels" in {
      forAll(Table("mode", DisplayMode.values: _*)) { mode =>
        val node = TypeNode.fromType(LRec, Func(Func(Func(IntType(), IntType()), Func(IntType(), BoolType())), BoolType()))
        noException should be thrownBy HTMLConvertor(LRec, mode).convert(node)
      }
    }

    "be able to parse types from strings" in {
      val types =
        Table("type", IntType(), BoolType(), Func(IntType(), IntType()), Func(Func(IntType(), IntType()), BoolType()))

      forAll(types) { t =>
        TypeNode.fromType(LRec, t).getType shouldBe t
        readType(t.toString) shouldBe Some(t)
      }
    }

    "correctly convert from a type" in {
      val cases = Table(
        ("type", "node"),
        (IntType(), TypeNode(LRec, "IntType", List())),
        (BoolType(), TypeNode(LRec, "BoolType", List())),
        (
          Func(IntType(), BoolType()),
          TypeNode(LRec, "Func", List(SubTypeNode(TypeNode(LRec, "IntType", List())), SubTypeNode(TypeNode(LRec, "BoolType", List()))))
        ),
        (
          Func(Func(IntType(), IntType()), BoolType()),
          TypeNode(LRec,
            "Func",
            List(
              SubTypeNode(
                TypeNode(LRec,
                  "Func",
                  List(SubTypeNode(TypeNode(LRec, "IntType", List())), SubTypeNode(TypeNode(LRec, "IntType", List())))
                )
              ),
              SubTypeNode(TypeNode(LRec, "BoolType", List()))
            )
          )
        )
      )

      forAll(cases) { (t, node) =>
        TypeNode.fromType(LRec, t) shouldBe node
      }
    }

    "correctly convert to and from a string" in {
      val cases = Table(
        "node",
        TypeNode.fromType(LRec, IntType()),
        TypeNode.fromType(LRec, BoolType()),
        TypeNode.fromType(LRec, Func(IntType(), BoolType())),
        TypeNode.fromType(LRec, Func(Func(IntType(), IntType()), BoolType()))
      )

      forAll(cases) { node =>
        Node.read(LRec, node.toString) shouldBe Some(node)
      }
    }
  }

  "Tree paths" should {
    "return the correct child" in {
      val node =
        ExprNode.fromExpr(LRec,
          Apply(Lambda("x", IntType(), IfThenElse(Equal(Var("x"), Num(0)), Num(1), Num(0))), Num(5))
        )
      node.findChild(List()) shouldBe Some(node)
      node.findChild(List(0)) shouldBe Some(
        ExprNode.fromExpr(LRec, Lambda("x", IntType(), IfThenElse(Equal(Var("x"), Num(0)), Num(1), Num(0))))
      )
      node.findChild(List(1)) shouldBe Some(ExprNode.fromExpr(LRec, Num(5)))
      node.findChild(List(0, 0)) shouldBe Some(LiteralNode(LiteralIdentifierBind("x")))
      node.findChild(List(0, 1)) shouldBe Some(TypeNode.fromType(LRec, IntType()))
      node.findChild(List(0, 2)) shouldBe Some(
        ExprNode.fromExpr(LRec, IfThenElse(Equal(Var("x"), Num(0)), Num(1), Num(0)))
      )
      node.findChild(List(0, 2, 0)) shouldBe Some(ExprNode.fromExpr(LRec, Equal(Var("x"), Num(0))))
      node.findChild(List(0, 2, 1)) shouldBe Some(ExprNode.fromExpr(LRec, Num(1)))
      node.findChild(List(0, 2, 2)) shouldBe Some(ExprNode.fromExpr(LRec, Num(0)))
    }

    "error on invalid paths" in {
      val node =
        ExprNode.fromExpr(LRec,
          Apply(Lambda("x", IntType(), IfThenElse(Equal(Var("x"), Num(0)), Num(1), Num(0))), Num(5))
        )

      val invalidPaths = Table(
        "path",
        List(-1),
        List(2),
        List(0, -1),
        List(0, 3),
        List(0, 1, -1),
        List(0, 1, 0),
        List(0, 1, 1),
        List(0, 2, -1),
        List(0, 2, 3),
        List(0, 2, 0, -1),
        List(0, 2, 0, 3),
        List(0, 2, 1, -1),
        List(0, 2, 1, 2),
        List(0, 2, 2, -1)
      )

      forAll(invalidPaths) { path =>
        an[InvalidTreePathException] should be thrownBy node.findChild(path)
      }
    }

    "string is correctly interpreted" in {
      val paths = Table(
        ("string", "path"),
        ("", Some(List())),
        ("0", Some(List(0))),
        ("0-1", Some(List(0, 1))),
        ("0-1-2", Some(List(0, 1, 2))),
        ("1-0", Some(List(1, 0))),
        ("65-1-56897", Some(List(65, 1, 56897))),
        ("x", None),
        ("1.56", None),
        ("1_4", None),
        ("0-e", None)
      )

      forAll(paths) { (string, path) =>
        path match {
          case Some(p) => Node.readPathString(string) shouldBe p
          case None    => an[InvalidTreePathStringException] should be thrownBy Node.readPathString(string)
        }
      }
    }
  }
}
