package languages

import convertors.{DisplayMode, HTMLConvertor}
import languages.LRec.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatest.wordspec.AnyWordSpec

class NodeSpec extends AnyWordSpec with Matchers with TableDrivenPropertyChecks {
  "Node parent" should {
    val nodes: TableFor1[OuterNode] = TableFor1(
      "node",
      VariableNode("Test", Nil),
      VariableNode("Test", List(SubExprNode(VariableNode("Test", Nil)), SubExprNode(VariableNode("Test", Nil)))),
      VariableNode("Test", List(LiteralNode(LiteralAny("foo")), SubExprNode(ExprChoiceNode()))),
      VariableNode(
        "Root",
        List(
          SubExprNode(
            VariableNode(
              "Level1",
              List(SubExprNode(VariableNode("Level2", Nil)), SubExprNode(VariableNode("Level2", Nil)))
            )
          )
        )
      ),
      VariableNode.fromExpr(Plus(Times(Num(5), Var("a")), IfThenElse(Bool(true), Num(5), Num(6)))),
      VariableNode.fromExpr(Let("x", Plus(Var("y"), Num(-1)), Plus(Var("x"), Var("y")))),
      VariableNode.fromExpr(
        Lambda("foo", Func(Func(IntType(), IntType()), BoolType()), Equal(Apply(Var("foo"), Num(1)), Num(0)))
      ),
      VariableNode.fromExpr(BlankExprDropDown()),
      VariableNode.fromExpr(Plus(BlankExprDropDown(), BlankExprDropDown())),
      TypeNode.fromType(IntType()),
      TypeNode.fromType(Func(IntType(), BoolType())),
      TypeNode.fromType(Func(Func(IntType(), IntType()), BoolType())),
      TypeNode.fromType(Func(Func(Func(IntType(), IntType()), Func(IntType(), BoolType())), BoolType())),
      TypeNode.fromType(BlankTypeDropDown()),
      TypeNode.fromType(Func(BlankTypeDropDown(), BlankTypeDropDown()))
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
        ("Num", VariableNode("Num", List(LiteralNode(LiteralInt(0))))),
        ("Plus", VariableNode("Plus", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))),
        ("Times", VariableNode("Times", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))),
        ("Bool", VariableNode("Bool", List(LiteralNode(LiteralBool(false))))),
        ("Var", VariableNode("Var", List(LiteralNode(LiteralIdentifierLookup(""))))),
        ("Equal", VariableNode("Equal", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))),
        (
          "IfThenElse",
          VariableNode(
            "IfThenElse",
            List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode()))
          )
        ),
        (
          "Let",
          VariableNode("Let", List(LiteralNode(LiteralIdentifierBind("")), SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))
        ),
        (
          "Lambda",
          VariableNode(
            "Lambda",
            List(LiteralNode(LiteralIdentifierBind("")), SubTypeNode(TypeNode.fromType(BlankTypeDropDown())), SubExprNode(ExprChoiceNode()))
          )
        ),
        (
          "Rec",
          VariableNode(
            "Rec",
            List(
              LiteralNode(LiteralIdentifierBind("")),
              LiteralNode(LiteralIdentifierBind("")),
              SubTypeNode(TypeNode.fromType(BlankTypeDropDown())),
              SubTypeNode(TypeNode.fromType(BlankTypeDropDown())),
              SubExprNode(ExprChoiceNode())
            )
          )
        ),
        ("Apply", VariableNode("Apply", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode()))))
      )

      forAll(cases) { (name, node) =>
        VariableNode.createFromExprName(name) shouldBe Some(node)
      }
    }

    "correctly convert from an expression (without types)" in {
      val cases = Table(
        ("expr", "node"),
        (Num(5), VariableNode("Num", List(LiteralNode(LiteralInt(5))))),
        (Var("x"), VariableNode("Var", List(LiteralNode(LiteralIdentifierLookup("x"))))),
        (
          Plus(Num(5), Num(6)),
          VariableNode(
            "Plus",
            List(
              SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(5))))),
              SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(6)))))
            )
          )
        ),
        (
          Plus(Times(Num(1), Num(2)), Num(53)),
          VariableNode(
            "Plus",
            List(
              SubExprNode(
                VariableNode(
                  "Times",
                  List(
                    SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(1))))),
                    SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(2)))))
                  )
                )
              ),
              SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(53)))))
            )
          )
        ),
        (BlankExprDropDown(), ExprChoiceNode()),
        (
          Plus(BlankExprDropDown(), BlankExprDropDown()),
          VariableNode("Plus", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))
        ),
        (
          IfThenElse(Equal(Num(65), Num(0)), Num(1), Num(0)),
          VariableNode(
            "IfThenElse",
            List(
              SubExprNode(
                VariableNode(
                  "Equal",
                  List(
                    SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(65))))),
                    SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(0)))))
                  )
                )
              ),
              SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(1))))),
              SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(0)))))
            )
          )
        ),
        (
          Let(LiteralIdentifierBind("5"), Num(5), Bool(false)),
          VariableNode(
            "Let",
            List(
              LiteralNode(LiteralIdentifierBind("5")),
              SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(5))))),
              SubExprNode(VariableNode("Bool", List(LiteralNode(LiteralBool(false)))))
            )
          )
        )
      )

      forAll(cases) { (expr, node) =>
        VariableNode.fromExpr(expr) shouldBe node
      }
    }

    "correctly convert from an expression (with types)" in {
      val cases = Table(
        ("expr", "node"),
        (
          Lambda("x", IntType(), Var("x")),
          VariableNode(
            "Lambda",
            List(
              LiteralNode(LiteralIdentifierBind("x")),
              SubTypeNode(TypeNode.fromType(IntType())),
              SubExprNode(VariableNode("Var", List(LiteralNode(LiteralIdentifierLookup("x")))))
            )
          )
        ),
        (
          Lambda("int", Func(IntType(), IntType()), Apply(Var("int"), Num(5))),
          VariableNode(
            "Lambda",
            List(
              LiteralNode(LiteralIdentifierBind("int")),
              SubTypeNode(TypeNode.fromType(Func(IntType(), IntType()))),
              SubExprNode(
                VariableNode(
                  "Apply",
                  List(
                    SubExprNode(VariableNode("Var", List(LiteralNode(LiteralIdentifierLookup("int"))))),
                    SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(5)))))
                  )
                )
              )
            )
          )
        ),
        (
          Rec("rec", "x", Func(IntType(), IntType()), IntType(), Var("x")),
          VariableNode(
            "Rec",
            List(
              LiteralNode(LiteralIdentifierBind("rec")),
              LiteralNode(LiteralIdentifierBind("x")),
              SubTypeNode(TypeNode.fromType(Func(IntType(), IntType()))),
              SubTypeNode(TypeNode.fromType(IntType())),
              SubExprNode(VariableNode("Var", List(LiteralNode(LiteralIdentifierLookup("x")))))
            )
          )
        )
      )

      forAll(cases) { (expr, node) =>
        VariableNode.fromExpr(expr) shouldBe node
      }
    }

    "correctly return its expression" in {
      val cases = Table(
        ("node", "expr"),
        (VariableNode("Num", List(LiteralNode(LiteralInt(5)))), Num(5)),
        (VariableNode("Var", List(LiteralNode(LiteralIdentifierLookup("x")))), Var("x")),
        (
          VariableNode(
            "Plus",
            List(
              SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(5))))),
              SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(6)))))
            )
          ),
          Plus(Num(5), Num(6))
        ),
        (
          VariableNode(
            "Plus",
            List(
              SubExprNode(
                VariableNode(
                  "Times",
                  List(
                    SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(1))))),
                    SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(2)))))
                  )
                )
              ),
              SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(53)))))
            )
          ),
          Plus(Times(Num(1), Num(2)), Num(53))
        ),
        (
          VariableNode(
            "Lambda",
            List(
              LiteralNode(LiteralIdentifierBind("z")),
              SubTypeNode(TypeNode.fromType(IntType())),
              SubExprNode(VariableNode("Var", List(LiteralNode(LiteralIdentifierLookup("z")))))
            )
          ),
          Lambda("z", IntType(), Var("z"))
        ),
        (
          VariableNode(
            "IfThenElse",
            List(
              SubExprNode(ExprChoiceNode()),
              SubExprNode(VariableNode("Var", List(LiteralNode(LiteralIdentifierLookup("bar"))))),
              SubExprNode(VariableNode("Bool", List(LiteralNode(LiteralBool(true)))))
            )
          ),
          IfThenElse(BlankExprDropDown(), Var("bar"), Bool(true))
        )
      )

      forAll(cases) { (node, expr) =>
        node.getExpr shouldBe expr
      }
    }
  }

  "SubExprNode" should {
    "correctly return its parent" in {
      val node = VariableNode(
        "Plus",
        List(SubExprNode(ExprChoiceNode()), SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(5))))))
      )
      node.args.head.getParent shouldBe Some(node)
      node.args(1).getParent shouldBe Some(node)
    }

    "not be able to have a TypeNode as a parent" in {
      a[NodeParentWrongTypeException] should be thrownBy
        SubExprNode(ExprChoiceNode()).setParent(Some(TypeNode.fromType(IntType())))

      a[NodeParentWrongTypeException] should be thrownBy
        TypeNode("Func", List(SubTypeNode(TypeNode("Int", Nil)), SubExprNode(ExprChoiceNode())))
    }

    "cannot be a root node" in {
      a[InnerNodeCannotBeRootException] should be thrownBy SubExprNode(ExprChoiceNode()).setParent(None)
    }
  }

  "TypeNode" should {
    "convert to HTML without error with multiple levels" in {
      forAll(Table("mode", DisplayMode.values: _*)) { mode =>
        val node = TypeNode.fromType(Func(Func(Func(IntType(), IntType()), Func(IntType(), BoolType())), BoolType()))
        noException should be thrownBy HTMLConvertor(LRec, mode).convert(node)
      }
    }

    "be able to parse types from strings" in {
      val types =
        Table("type", IntType(), BoolType(), Func(IntType(), IntType()), Func(Func(IntType(), IntType()), BoolType()))

      forAll(types) { t =>
        TypeNode.fromType(t).getType shouldBe t
        readType(t.toString) shouldBe Some(t)
      }
    }

    "correctly convert from a type" in {
      val cases = Table(
        ("type", "node"),
        (IntType(), TypeNode("IntType", List())),
        (BoolType(), TypeNode("BoolType", List())),
        (
          Func(IntType(), BoolType()),
          TypeNode("Func", List(SubTypeNode(TypeNode("IntType", List())), SubTypeNode(TypeNode("BoolType", List()))))
        ),
        (
          Func(Func(IntType(), IntType()), BoolType()),
          TypeNode(
            "Func",
            List(
              SubTypeNode(
                TypeNode(
                  "Func",
                  List(SubTypeNode(TypeNode("IntType", List())), SubTypeNode(TypeNode("IntType", List())))
                )
              ),
              SubTypeNode(TypeNode("BoolType", List()))
            )
          )
        )
      )

      forAll(cases) { (t, node) =>
        TypeNode.fromType(t) shouldBe node
      }
    }

    "correctly convert to and from a string" in {
      val cases = Table(
        "node",
        TypeNode.fromType(IntType()),
        TypeNode.fromType(BoolType()),
        TypeNode.fromType(Func(IntType(), BoolType())),
        TypeNode.fromType(Func(Func(IntType(), IntType()), BoolType()))
      )

      forAll(cases) { node =>
        Node.read(node.toString) shouldBe Some(node)
      }
    }
  }

  "LiteralNode" should {
    def testLiteralNodeStringConversion(literalStrings: List[Literal]): Unit = {
      def checkMatch(literal: Literal): Unit = {
        val outerVersion = VariableNode("Num", List(LiteralNode(literal)))
        Node.read(outerVersion.toString) shouldBe Some(outerVersion)
      }

      forAll(Table("literalString", literalStrings: _*)) { literal =>
        checkMatch(literal)
      }
    }

    "correctly convert to and from a string without escapes" in {
      val literals = List("", "foo", "bar", "861", "-65", "1.56")

      testLiteralNodeStringConversion(literals.map(Literal.fromString))
    }

    "correctly convert to and from a string with escapes" in {
      val literals = List(
        "\\",
        "\\\\",
        "\"",
        "\\\"",
        "\\\\\"",
        "\"test\"",
        "\\\"test\\\"",
        "\"Hello\"!, \"World\"!",
        "1\\\"'.--';\\\"\\\\\\4 \\t \\\\",
        "1\\\"'.--';\\\"\\\\\\4 \\t \\\\"
      )

      testLiteralNodeStringConversion(literals.map(Literal.fromString))
    }
  }

  "Tree paths" should {
    "return the correct child" in {
      val node =
        VariableNode.fromExpr(
          Apply(Lambda("x", IntType(), IfThenElse(Equal(Var("x"), Num(0)), Num(1), Num(0))), Num(5))
        )
      node.findChild(List()) shouldBe Some(node)
      node.findChild(List(0)) shouldBe Some(
        VariableNode.fromExpr(Lambda("x", IntType(), IfThenElse(Equal(Var("x"), Num(0)), Num(1), Num(0))))
      )
      node.findChild(List(1)) shouldBe Some(VariableNode.fromExpr(Num(5)))
      node.findChild(List(0, 0)) shouldBe Some(LiteralNode(LiteralIdentifierBind("x")))
      node.findChild(List(0, 1)) shouldBe Some(TypeNode.fromType(IntType()))
      node.findChild(List(0, 2)) shouldBe Some(
        VariableNode.fromExpr(IfThenElse(Equal(Var("x"), Num(0)), Num(1), Num(0)))
      )
      node.findChild(List(0, 2, 0)) shouldBe Some(VariableNode.fromExpr(Equal(Var("x"), Num(0))))
      node.findChild(List(0, 2, 1)) shouldBe Some(VariableNode.fromExpr(Num(1)))
      node.findChild(List(0, 2, 2)) shouldBe Some(VariableNode.fromExpr(Num(0)))
    }

    "error on invalid paths" in {
      val node =
        VariableNode.fromExpr(
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
