package convertors

import languages.{AbstractNodeLanguage, ClickDeduceLanguage}

class LaTeXConvertor(override val lang: ClickDeduceLanguage, mode: DisplayMode) extends IConvertor(lang, mode) {
  private type LaTeX = String
  private type ExprNode = lang.ExprNode
  private type TypeNode = lang.TypeNodeParent

  override def convert[T <: AbstractNodeLanguage#OuterNode](node: T): Output = {
    outerNodeToLaTeX(node.asInstanceOf[lang.OuterNode])
  }

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

  private def createTree(below: LaTeX, ruleLabel: LaTeX, children: List[LaTeX]): LaTeX = {
    s"""\\prftree[r]{\\scriptsize $ruleLabel}
       |{${children.mkString("\n")}}
       |{$below}""".stripMargin
  }

  def fullExprBottomDiv(node: ExprNode): LaTeX =
    s"${envDiv(node.getEnv(mode))} ${exprDiv(node)} ${resultDiv(node)}".strip()

  def envDiv(env: lang.ValueEnv | lang.TypeEnv): LaTeX = {
    val variablesHtml: Option[LaTeX] =
      if (env.isEmpty) None else Some(env.map((k, v) => MultiElement(TextElement(k), TextElement(" \\rightarrow "), v.toText)).mkString("[", ", ", "]"))
    val delimiter =
      if (mode == DisplayMode.TypeCheck) Some(typeCheckTurnstileSpan) else if (env.nonEmpty) Some(",") else None

    var result: String = ""
    if (variablesHtml.isDefined) result += variablesHtml.get
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

  def typeDiv(node: TypeNode): LaTeX = node.toText(mode).asLaTeX

  def typeResultDiv(node: TypeNode): LaTeX = s"$typeCheckColon, ${node.getTypeCheckResult(mode).toText.asLaTeX}"

  private val typeCheckTurnstileSpan: LaTeX = "\\vdash"

  private val typeCheckColon: LaTeX = ":"

  private def typeCheckResultDiv(node: ExprNode): LaTeX = node.getType.toText.asLaTeX

  private val evalArrowSpan: LaTeX = "\\Downarrow"

  private def evalResultDiv(node: ExprNode): LaTeX = node.getValue.toText.asLaTeX

  private def editEvalResultDiv(node: ExprNode): LaTeX = node.getEditValueResult.toText.asLaTeX
}
