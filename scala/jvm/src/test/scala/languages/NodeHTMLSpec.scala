package languages

import convertors.{DisplayMode, HTMLConvertor}
import languages.LLam.*
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import net.ruippeixotog.scalascraper.model.*
import nodes.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatest.wordspec.AnyWordSpec

class NodeHTMLSpec extends AnyWordSpec with Matchers with TableDrivenPropertyChecks {
  val modes: TableFor1[DisplayMode] = TableFor1("mode", DisplayMode.values: _*)

  def checkHtmlDoc(tree: OuterNode)(mode: DisplayMode)(test: Document => Unit): Unit = {
//    val convertor = HTMLConvertor(LLam, mode)
//    val convertorTree = convertor.fixOuterNodeType(tree)
    test(JsoupBrowser().parseString(HTMLConvertor(LLam, mode).convert(tree)))
  }

  def checkHtmlDocAllModes(tree: OuterNode)(test: Document => Unit): Unit = {
    forAll(modes) { mode =>
      checkHtmlDoc(tree)(mode)(test)
    }
  }

  def getRootElement(doc: Document): Element = doc.body.children.head

  def classesOf(el: Element): Set[String] = el.attr("class").split(" ").toSet

  "be correct with single node tree" should {
    val tree = ExprNode.fromExpr(LLam, Num(5))

    "root should have subtree and axiom classes" in {
      checkHtmlDocAllModes(tree) { doc =>
        doc >> elementList("div.subtree") should have size 1
        doc >> elementList("div.subtree.axiom") should have size 1
        (doc >> elementList("div.subtree")).head shouldBe getRootElement(doc)
      }
    }

    "root should have an empty 'data-tree-path' attribute" in {
      checkHtmlDocAllModes(tree) { doc =>
        getRootElement(doc).attrs.contains("data-tree-path") shouldBe true
        getRootElement(doc).attr("data-tree-path") shouldBe ""
      }
    }

    "have a child '.expr' div" should {
      "should exist" in {
        checkHtmlDocAllModes(tree) { doc =>
          doc >> elementList("div.subtree > div.node > div.expr") should have size 1
        }
      }

      "contain a div which contains an input" should {
        val selector = "div.subtree > div.node > div.expr input.literal"

        "should exist" in {
          checkHtmlDocAllModes(tree) { doc =>
            doc >> elementList(selector) should have size 1
          }
        }

        "should have 'number' input type" in {
          checkHtmlDocAllModes(tree) { doc =>
            (doc >> elementList(selector)).head.attr("type") shouldBe "number"
          }
        }

        "should have the correct 'data-tree-path'" in {
          checkHtmlDocAllModes(tree) { doc =>
            (doc >> elementList(selector)).head.attr("data-tree-path") shouldBe "0"
          }
        }

        "should have the correct value" in {
          checkHtmlDocAllModes(tree) { doc =>
            val literalValue = tree.findChild(List(0)).get.asInstanceOf[LiteralNode].literalText
            (doc >> elementList(selector)).head.attr("value") shouldBe literalValue
          }
        }
      }

      "contain a result div" should {
        "have eval results in edit and eval modes" should {
          val selector = "div.subtree > div.node > div.eval-result"
          val modes = TableFor1("mode", DisplayMode.Edit, DisplayMode.Evaluation)

          "should exist" in {
            forAll(modes) { mode =>
              checkHtmlDoc(tree)(mode) { doc =>
                doc >> elementList(selector) should have size 1
              }
            }
          }
        }

        "have type-check results in type-check mode" should {
          val selector = "div.subtree > div.node > div.type-check-result"

          "should exist" in {
            checkHtmlDoc(tree)(DisplayMode.TypeCheck) { doc =>
              doc >> elementList(selector) should have size 1
            }
          }
        }
      }
    }

    "have a child '.annotation-axiom' div" should {
      "should exist" in {
        checkHtmlDocAllModes(tree) { doc =>
          doc >> elementList("div.subtree > div.annotation-axiom") should have size 1
        }
      }

      "should have no children" in {
        checkHtmlDocAllModes(tree) { doc =>
          doc >> elementList("div.subtree > div.annotation-axiom > *") shouldBe empty
        }
      }

      "should have the correct text" in {
        checkHtmlDocAllModes(tree) { doc =>
          (doc >> elementList("div.subtree > div.annotation-axiom")).head.text shouldBe tree.exprName
        }
      }
    }
  }

  "be correct with a complex node tree" should {
    val tree = ExprNode.fromExpr(LLam, Apply(Lambda("x", IntType(), Plus(Var("x"), Num(1))), Num(-4)))

    "root" should {
      "have '.subtree' class, but no '.axiom' class" in {
        checkHtmlDocAllModes(tree) { doc =>
          classesOf(getRootElement(doc)) should contain("subtree")
          classesOf(getRootElement(doc)) should not contain "axiom"
        }
      }

      "have the correct 'data-tree-path'" in {
        checkHtmlDocAllModes(tree) { doc =>
          getRootElement(doc).attr("data-tree-path") shouldBe ""
        }
      }

      "have a correct '.node' div" should {
        val selector = "body > div.subtree > div.node"

        "should exist" in {
          checkHtmlDocAllModes(tree) { doc =>
            doc >> elementList(selector) should have size 1
          }
        }

        "should contain a '.expr' div" in {
          checkHtmlDocAllModes(tree) { doc =>
            doc >> elementList(selector + " > div.expr") should have size 1
          }
        }

        "should contain a '.eval-result' div in edit/eval mode" in {
          forAll(Table("mode", DisplayMode.Edit, DisplayMode.Evaluation)) { mode =>
            checkHtmlDoc(tree)(mode) { doc =>
              doc >> elementList(selector + " > div.eval-result") should have size 1
            }
          }
        }

        "should contain a '.type-check-result' div in type-checking mode" in {
          checkHtmlDoc(tree)(DisplayMode.TypeCheck) { doc =>
            doc >> elementList(selector + " > div.type-check-result") should have size 1
          }
        }
      }
    }

    val rootArgsSelector = "body > div.subtree > div.args"

    "subtrees in edit/type-checking mode" should {
      val modes = Table("mode", DisplayMode.Edit, DisplayMode.TypeCheck)

      "root should have two subtrees" in {
        forAll(modes) { mode =>
          checkHtmlDoc(tree)(mode) { doc =>
            doc >> elementList(rootArgsSelector + " > div.subtree") should have size 2
          }
        }
      }

      "left subtree" should {
        val leftSubtreeSelector = rootArgsSelector + " > div.subtree:nth-child(1)"

        "not be an axiom" in {
          forAll(modes) { mode =>
            checkHtmlDoc(tree)(mode) { doc =>
              classesOf((doc >> elementList(leftSubtreeSelector)).head) should not contain "axiom"
            }
          }
        }

        "match the node" in {
          forAll(modes) { mode =>
            checkHtmlDoc(tree)(mode) { doc =>
              (doc >> elementList(leftSubtreeSelector)).head.attr("data-tree-path") shouldBe "0"
            }
          }
        }

        "have an '.expr' div" in {
          forAll(modes) { mode =>
            checkHtmlDoc(tree)(mode) { doc =>
              doc >> elementList(leftSubtreeSelector + " > div.node > div.expr") should have size 1
            }
          }
        }

        "have an input for the lambda variable name" in {
          val inputSelector = leftSubtreeSelector + " > div.node > div.expr input.literal:not([readonly])"
          forAll(modes) { mode =>
            checkHtmlDoc(tree)(mode) { doc =>
              doc >> elementList(inputSelector) should have size 1
              val element = (doc >> elementList(inputSelector)).head
              element.attr("type") shouldBe "text"
              element.attr("data-tree-path") shouldBe "0-0"
              element.attr("value") shouldBe "x"
            }
          }
        }

        "have correct subtrees" should {
          val depth2Selector = leftSubtreeSelector + " > div.args > div.subtree"

          "have two subtrees" in {
            forAll(modes) { mode =>
              checkHtmlDoc(tree)(mode) { doc =>
                doc >> elementList(depth2Selector) should have size 2
              }
            }
          }

          "left subtree (IntType)" should {
            def getElement = (doc: Document) => (doc >> elementList(depth2Selector)).head

            "be an axiom" in {
              forAll(modes) { mode =>
                checkHtmlDoc(tree)(mode) { doc =>
                  classesOf(getElement(doc)) should contain("axiom")
                }
              }
            }

            "match the node" in {
              forAll(modes) { mode =>
                checkHtmlDoc(tree)(mode) { doc =>
                  getElement(doc).attr("data-tree-path") shouldBe "0-1"
                }
              }
            }
          }

          "right subtree (Plus(Var(x), Num(1)))" should {
            def getElement = (doc: Document) => (doc >> elementList(depth2Selector))(1)

            "not be an axiom" in {
              forAll(modes) { mode =>
                checkHtmlDoc(tree)(mode) { doc =>
                  classesOf(getElement(doc)) should not contain "axiom"
                }
              }
            }

            "match the node" in {
              forAll(modes) { mode =>
                checkHtmlDoc(tree)(mode) { doc =>
                  getElement(doc).attr("data-tree-path") shouldBe "0-2"
                }
              }
            }
          }
        }
      }

      "right subtree" should {
        val rightSubtreeSelector = rootArgsSelector + " > div.subtree:nth-child(2)"

        "be an axiom" in {
          forAll(modes) { mode =>
            checkHtmlDoc(tree)(mode) { doc =>
              classesOf((doc >> elementList(rightSubtreeSelector)).head) should contain("axiom")
            }
          }
        }

        "have 'data-tree-path' equal to '1'" in {
          forAll(modes) { mode =>
            checkHtmlDoc(tree)(mode) { doc =>
              (doc >> elementList(rightSubtreeSelector)).head.attr("data-tree-path") shouldBe "1"
            }
          }
        }

        "have an '.expr' div" in {
          forAll(modes) { mode =>
            checkHtmlDoc(tree)(mode) { doc =>
              doc >> elementList(rightSubtreeSelector + " > div.node > div.expr") should have size 1
            }
          }
        }

        "have an '.annotation-axiom' div" in {
          forAll(modes) { mode =>
            checkHtmlDoc(tree)(mode) { doc =>
              doc >> elementList(rightSubtreeSelector + " > div.annotation-axiom") should have size 1
            }
          }
        }
      }
    }

    "subtrees in eval mode" should {
      val mode = DisplayMode.Evaluation

      "be three subtrees" in {
        checkHtmlDoc(tree)(mode) { doc =>
          doc >> elementList(rootArgsSelector + " > div.subtree") should have size 3
        }
      }

      "have correct left subtree (Lambda(x, IntType(), Plus(Var(x), Num(1)))" should {
        val selector = rootArgsSelector + " > div.subtree:nth-child(1)"

        "be an axiom" in {
          checkHtmlDoc(tree)(mode) { doc =>
            classesOf((doc >> elementList(selector)).head) should contain("axiom")
          }
        }

        "match the node" in {
          checkHtmlDoc(tree)(mode) { doc =>
            (doc >> elementList(selector)).head.attr("data-tree-path") shouldBe "0"
          }
        }

        "have an input for the lambda variable name" in {
          val inputSelector = selector + " > div.node > div.expr input.literal:not([readonly])"
          checkHtmlDoc(tree)(mode) { doc =>
            doc >> elementList(inputSelector) should have size 1
            val element = (doc >> elementList(inputSelector)).head
            element.attr("type") shouldBe "text"
            element.attr("data-tree-path") shouldBe "0-0"
            element.attr("value") shouldBe "x"
          }
        }
      }

      "have correct middle subtree (Num(-4))" should {
        val selector = rootArgsSelector + " > div.subtree:nth-child(2)"

        "be an axiom" in {
          checkHtmlDoc(tree)(mode) { doc =>
            classesOf((doc >> elementList(selector)).head) should contain("axiom")
          }
        }

        "match the node" in {
          checkHtmlDoc(tree)(mode) { doc =>
            (doc >> elementList(selector)).head.attr("data-tree-path") shouldBe "1"
          }
        }
      }

      "have correct right subtree (phantom apply)" should {
        val selector = rootArgsSelector + " > div.subtree:nth-child(3)"

        "not be an axiom" in {
          checkHtmlDoc(tree)(mode) { doc =>
            classesOf((doc >> elementList(selector)).head) should not contain "axiom"
          }
        }

        "be a phantom" in {
          checkHtmlDoc(tree)(mode) { doc =>
            classesOf((doc >> elementList(selector)).head) should contain("phantom")
          }
        }

        "have correct subtrees" in {
          checkHtmlDoc(tree)(mode) { doc =>
            val elements = doc >> elementList(selector + " > div.args > div.subtree")
            elements should have size 2
            forAll(Table(("element", "index"), elements.zipWithIndex: _*)) { (el, i) =>
              classesOf(el) should contain("axiom")
            }
          }
        }
      }
    }
  }
}
