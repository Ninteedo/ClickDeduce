package languages

import convertors.{DisplayMode, LaTeXConvertor}
import languages.LPoly.*
import nodes.*
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor2}
import org.scalatest.propspec.AnyPropSpec

class LaTeXTest extends AnyPropSpec with TableDrivenPropertyChecks {
  private val docStart: String =
    "\\documentclass{article}\n\\usepackage{bussproofs}\n\\usepackage[margin=1cm]{geometry}\n\\begin{document}\n"
  private val docEnd: String = "\n\\end{document}"

  private def latexOutputTest(displayMode: DisplayMode): Unit = {
    val l = LPoly
    val convertor: LaTeXConvertor = new LaTeXConvertor(l, displayMode)
    val nodes: TableFor2[String, OuterNode] = Table(
      ("name", "node"),
      ("Empty node", ExprChoiceNode(l)),
      ("Num node", ExprNode.fromExpr(l, l.Num(8165))),
      ("Arithmetic tree", ExprNode.fromExpr(l, l.Times(l.Plus(l.Num(45), l.Num(7)), l.Num(-41658)))),
      (
        "Boolean tree",
        ExprNode.fromExpr(
          l,
          l.Plus(
            l.IfThenElse(l.Equal(l.Num(56), l.Times(l.Num(2), l.Num(38))), l.Plus(l.Num(1), l.Num(2)), l.Num(6)),
            l.Num(87)
          )
        )
      ),
      (
        "Let tree",
        ExprNode.fromExpr(
          l,
          l.Let("x", l.Bool(true), l.IfThenElse(l.Var("x"), l.Plus(l.Num(1), l.Num(2)), l.Num(6)))
        )
      ),
      (
        "Lambda tree",
        ExprNode.fromExpr(l, l.Apply(l.Lambda("x", l.IntType(), l.Plus(l.Var("x"), l.Num(1))), l.Num(42)))
      ),
      (
        "Factorial tree",
        ExprNode.fromExpr(
          l,
          l.Apply(
            l.Rec(
              "factorial",
              "n",
              l.IntType(),
              l.IntType(),
              l.IfThenElse(
                l.Equal(l.Var("n"), l.Num(0)),
                l.Num(1),
                l.Times(l.Var("n"), l.Apply(l.Var("factorial"), l.Plus(l.Var("n"), l.Num(-1))))
              )
            ),
            l.Num(2)
          )
        )
      ),
      (
        "Pair tree",
        ExprNode.fromExpr(
          l,
          l.LetPair("x", "y", l.Pair(l.Num(1), l.Bool(false)), l.IfThenElse(l.Var("y"), l.Var("x"), l.Num(0)))
        )
      ),
      (
        "Union type tree",
        ExprNode.fromExpr(
          l,
          l.CaseSwitch(
            l.Left(l.Num(1), l.BoolType()),
            "x",
            "y",
            l.Plus(l.Var("x"), l.Num(1)),
            l.Equal(l.Var("y"), l.Bool(false))
          )
        )
      )
    )
    val treesLatex: String = nodes
      .map { case (name, node) =>
        s"$name:\n${convertor.convert(node)}"
      }
      .mkString("\n")
    val latex: String = docStart + treesLatex + docEnd
    println(latex)
  }

  property("edit mode output test") {
    latexOutputTest(DisplayMode.Edit)
  }

  property("evaluation output test") {
    latexOutputTest(DisplayMode.Evaluation)
  }

  property("type-checking output test") {
    latexOutputTest(DisplayMode.TypeCheck)
  }
}
