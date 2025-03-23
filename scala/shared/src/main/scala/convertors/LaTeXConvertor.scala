package convertors

import languages.ClickDeduceLanguage
import nodes.*

class LaTeXConvertor(lang: ClickDeduceLanguage, mode: DisplayMode) extends IConvertor(lang, mode) {
  private type LaTeX = String

  private var envCounter: Int = 0

  private def nextEnvIndex(): Int = {
    val res = envCounter
    envCounter += 1
    res
  }

  override def convert(node: OuterNode): Output = {
    val startingEnvIndex = node match {
      case n: ExprNodeParent => if n.getEnv(mode).isEmpty then -1 else 0
      case _ => -1
    }
    envCounter = startingEnvIndex

    val latexTree = outerNodeToLaTeX(node, startingEnvIndex)
    asProofTree(removeBlankLines(latexTree))
  }

  private def removeBlankLines(s: String): String = s.split("\n").filter(_.trim.nonEmpty).mkString("\n")

  private def asProofTree(s: String): String = s"\\begin{prooftree}\n$s\n\\end{prooftree}"

  private def outerNodeToLaTeX(node: OuterNode, envIndex: Int): LaTeX =
    node match {
      case e: ExprNodeParent => exprNodeToLaTeX(e, envIndex)
      case t: TypeNodeParent => typeNodeToLaTeX(t, envIndex)
    }

  // expressions

  private def exprNodeToLaTeX(node: ExprNodeParent, envIndex: Int): LaTeX = {
    val newEnvIndex =
      if node.hasUpdatedEnv(mode) then nextEnvIndex()
      else envIndex

    val childLatex = node
      .getVisibleChildren(mode)
      .map(ch => outerNodeToLaTeX(ch, newEnvIndex))

    val below = fullExprBottomDiv(node, newEnvIndex)

    createTree(below, node.exprName, childLatex)
  }

  private def fullExprBottomDiv(node: ExprNodeParent, envIndex: Int): LaTeX = {
    val envPart   = envDiv(node, envIndex, isTypeMode = false)
    val exprPart  = exprDiv(node)
    val resultPart= resultDiv(node)
    s"$envPart $exprPart $resultPart".trim
  }

  private def exprDiv(node: ExprNodeParent): LaTeX = node.getExpr.toText.asLaTeX

  private def resultDiv(node: ExprNodeParent): LaTeX =
    mode match {
      case DisplayMode.Edit =>
        val typeCheckResult = node.getType
        if (typeCheckResult.isError) s"$typeCheckColon ${typeCheckResultDiv(node)}"
        else {
          val evalResult = node.getEditValueResult
          if (!evalResult.isError && !evalResult.isPlaceholder)
            s"$evalArrowSpan ${editEvalResultDiv(node)}"
          else s"$typeCheckColon ${typeCheckResultDiv(node)}"
        }
      case DisplayMode.TypeCheck =>
        s"$typeCheckColon ${typeCheckResultDiv(node)}"
      case DisplayMode.Evaluation =>
        s"$evalArrowSpan ${evalResultDiv(node)}"
    }

  private def typeCheckResultDiv(node: ExprNodeParent): LaTeX =
    node.getType.toText.asLaTeX

  private def editEvalResultDiv(node: ExprNodeParent): LaTeX =
    node.getEditValueResult.toText.asLaTeX

  private def evalResultDiv(node: ExprNodeParent): LaTeX =
    node.getValue.toText.asLaTeX

  // types

  private def typeNodeToLaTeX(node: TypeNodeParent, envIndex: Int): LaTeX = {
    val childLatex = node
      .getVisibleChildren(mode)
      .map(ch => outerNodeToLaTeX(ch, envIndex))

    val below = fullTypeBottomDiv(node, envIndex)
    createTree(below, node.getTypeName, childLatex)
  }

  private def fullTypeBottomDiv(node: TypeNodeParent, envIndex: Int): LaTeX = {
    val envPart  = envDiv(node, envIndex, isTypeMode = true)
    val typePart = node.getType.toText.asLaTeX
    val resPart  = s"$typeCheckColon ${node.getTypeCheckResult(mode).toText.asLaTeX}"
    s"$envPart, $typePart $resPart".trim
  }

  // environments

  private def envDiv(node: OuterNode, envIndex: Int, isTypeMode: Boolean): LaTeX = {
    if (envIndex < 0) {
      ""
    } else {
      if (isTypeMode || mode == DisplayMode.TypeCheck)
        s"\\Gamma_{$envIndex} $typeCheckTurnstileSpan"
      else
        s"\\sigma_{$envIndex}, "
    }
  }

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
    s"""${children.mkString("\n")}
       |\\$ruleKind{$dollar$below$dollar}""".stripMargin
  }

  private val typeCheckTurnstileSpan: LaTeX = "\\vdash"
  private val typeCheckColon: LaTeX        = ":"
  private val evalArrowSpan: LaTeX         = "\\Downarrow"
}
