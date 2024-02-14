package convertors

import languages.{AbstractNodeLanguage, ClickDeduceLanguage}

class LaTeXConvertor(override val lang: ClickDeduceLanguage, mode: DisplayMode) extends IConvertor(lang, mode) {
  private type LaTeX = String
  private type ExprNode = lang.ExprNode
  private type TypeNode = lang.TypeNodeParent

  override def convert[T <: AbstractNodeLanguage#OuterNode](node: T): Output = {
    asProofTree(removeBlankLines(outerNodeToLaTeX(node.asInstanceOf[lang.OuterNode])))
  }

  private def removeBlankLines(s: String): String = s.split("\n").filter(_.nonEmpty).mkString("\n")

  private def asProofTree(s: String): String = s"\\begin{prooftree}\n$s\n\\end{prooftree}"

  def outerNodeToLaTeX(node: lang.OuterNode): LaTeX = node match {
    case node: ExprNode => exprNodeToLaTeX(node)
    case node: TypeNode => typeNodeToLaTeX(node)
  }

  private def exprNodeToLaTeX(node: ExprNode): LaTeX = {
    createTree(fullExprBottomDiv(node), node.exprName, node.getVisibleChildren(mode).map(outerNodeToLaTeX))
  }

  private def typeNodeToLaTeX(node: TypeNode): LaTeX = {
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

  def fullExprBottomDiv(node: ExprNode): LaTeX =
    s"${envDiv(node.getEnv(mode))} ${exprDiv(node)} ${resultDiv(node)}".strip()

  def envDiv(env: lang.ValueEnv | lang.TypeEnv): LaTeX = {
    val variables: Option[LaTeX] =
      if (env.isEmpty) None
      else
        Some(
          env
            .map((k, v) =>
              MultiElement(ItalicsElement(TextElement(k)), SurroundSpaces(SingleRightArrow()), v.toText).asLaTeX
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

  def exprDiv(node: ExprNode): LaTeX = node.getExpr.toText.asLaTeX

  def resultDiv(node: ExprNode): LaTeX = mode match {
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

  def fullTypeBottomDiv(node: TypeNode): LaTeX = s"${envDiv(node.getEnv(mode))} ${typeDiv(node)} ${typeResultDiv(node)}"

  def typeDiv(node: TypeNode): LaTeX = node.getType.toText.asLaTeX

  def typeResultDiv(node: TypeNode): LaTeX = s"$typeCheckColon ${node.getTypeCheckResult(mode).toText.asLaTeX}"

  private val typeCheckTurnstileSpan: LaTeX = "\\vdash"

  private val typeCheckColon: LaTeX = ":"

  private def typeCheckResultDiv(node: ExprNode): LaTeX = node.getType.toText.asLaTeX

  private val evalArrowSpan: LaTeX = "\\Downarrow"

  private def evalResultDiv(node: ExprNode): LaTeX = node.getValue.toText.asLaTeX

  private def editEvalResultDiv(node: ExprNode): LaTeX = node.getEditValueResult.toText.asLaTeX
}
