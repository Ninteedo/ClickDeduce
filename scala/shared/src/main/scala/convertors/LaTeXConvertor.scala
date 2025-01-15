package convertors

import convertors.text.*
import languages.ClickDeduceLanguage
import languages.env.{TypeEnv, ValueEnv}
import nodes.*

class LaTeXConvertor(lang: ClickDeduceLanguage, mode: DisplayMode) extends IConvertor(lang, mode) {
  private type LaTeX = String

  override def convert(node: OuterNode): Output = {
    asProofTree(removeBlankLines(outerNodeToLaTeX(node.asInstanceOf[OuterNode])))
  }

  private def removeBlankLines(s: String): String = s.split("\n").filter(_.nonEmpty).mkString("\n")

  private def asProofTree(s: String): String = s"\\begin{prooftree}\n$s\n\\end{prooftree}"

  private def outerNodeToLaTeX(node: OuterNode): LaTeX = node match {
    case node: ExprNodeParent => exprNodeToLaTeX(node)
    case node: TypeNodeParent => typeNodeToLaTeX(node)
  }

  private def exprNodeToLaTeX(node: ExprNodeParent): LaTeX = {
    createTree(fullExprBottomDiv(node), node.exprName, node.getVisibleChildren(mode).map(outerNodeToLaTeX))
  }

  private def typeNodeToLaTeX(node: TypeNodeParent): LaTeX = {
    createTree(fullTypeBottomDiv(node), node.getTypeName, node.getVisibleChildren(mode).map(outerNodeToLaTeX))
  }

//  private def createTree(below: LaTeX, ruleLabel: LaTeX, children: List[LaTeX]): LaTeX = {
//    s"""\\prftree[r]{\\scriptsize $ruleLabel}
//       |{${children.mkString("\n")}}
//       |{$below}""".stripMargin
//  }

  private def createTree(below: LaTeX, ruleLabel: LaTeX, children: List[LaTeX]): LaTeX = {
    val ruleKind = children.size match {
      case 0 => "AxiomC{}\n\\UnaryInfC"
      case 1 => "UnaryInfC"
      case 2 => "BinaryInfC"
      case 3 => "TrinaryInfC"
      case 4 => "QuaternaryInfC"
      case 5 => "QuinaryInfC"
      case n => throw new IllegalArgumentException(s"Too many children ($n) for LaTeX tree")
    }
    val dollar = "$"
    s"""${children.mkString("\n")}\n\\$ruleKind{$dollar$below$dollar}""".stripMargin
  }

  private def fullExprBottomDiv(node: ExprNodeParent): LaTeX =
    s"${envDiv(node.getEnv(mode))} ${exprDiv(node)} ${resultDiv(node)}".strip()

  private def envDiv(env: ValueEnv | TypeEnv): LaTeX = {
    val variables: Option[LaTeX] =
      if (env.isEmpty) None
      else
        Some(
          env
            .map((k, v) =>
              MultiElement(ItalicsElement(TextElement(k)), SurroundSpaces(Symbols.singleRightArrow), v.toText).asLaTeX
            )
            .mkString("[", ", ", "]")
        )
    val delimiter =
      if (mode == DisplayMode.TypeCheck) Some(typeCheckTurnstileSpan) else if (env.nonEmpty) Some(",") else None

    var result: String = ""
    if (variables.isDefined) result += variables.get
    if (delimiter.isDefined) result += delimiter.get
    result
  }

  private def exprDiv(node: ExprNodeParent): LaTeX = node.getExpr.toText.asLaTeX

  private def resultDiv(node: ExprNodeParent): LaTeX = mode match {
    case DisplayMode.Edit =>
      val typeCheckResult = node.getType
      if (typeCheckResult.isError) s"$typeCheckColon ${typeCheckResultDiv(node)}"
      else {
        val evalResult = node.getEditValueResult
        if (!evalResult.isError && !evalResult.isPlaceholder) s"$evalArrowSpan ${editEvalResultDiv(node)}"
        else s"$typeCheckColon ${typeCheckResultDiv(node)}"
      }
    case DisplayMode.TypeCheck  => s"$typeCheckColon ${typeCheckResultDiv(node)}"
    case DisplayMode.Evaluation => s"$evalArrowSpan ${evalResultDiv(node)}"
  }

  private def fullTypeBottomDiv(node: TypeNodeParent): LaTeX = s"${envDiv(node.getEnv(mode))} ${typeDiv(node)} ${typeResultDiv(node)}"

  private def typeDiv(node: TypeNodeParent): LaTeX = node.getType.toText.asLaTeX

  private def typeResultDiv(node: TypeNodeParent): LaTeX = s"$typeCheckColon ${node.getTypeCheckResult(mode).toText.asLaTeX}"

  private val typeCheckTurnstileSpan: LaTeX = "\\vdash"

  private val typeCheckColon: LaTeX = ":"

  private def typeCheckResultDiv(node: ExprNodeParent): LaTeX = node.getType.toText.asLaTeX

  private val evalArrowSpan: LaTeX = "\\Downarrow"

  private def evalResultDiv(node: ExprNodeParent): LaTeX = node.getValue.toText.asLaTeX

  private def editEvalResultDiv(node: ExprNodeParent): LaTeX = node.getEditValueResult.toText.asLaTeX
}
