package convertors

import languages.{AbstractNodeLanguage, ClickDeduceLanguage}
import scalatags.Text.TypedTag
import scalatags.Text.all.*

class HTMLConvertor(override val lang: ClickDeduceLanguage, mode: DisplayMode) extends IConvertor(lang, mode) {
  type OuterNode = lang.OuterNode
  private type HTML = TypedTag[String]
  private type ExprNode = lang.ExprNode
  private type TypeNode = lang.TypeNodeParent

  def convert[T <: AbstractNodeLanguage#OuterNode](node: T): Output = {
    val fixedNode = node.asInstanceOf[OuterNode]
    outerNodeToHTML(fixedNode).toString
  }

//  def fixOuterNodeType[T <: AbstractNodeLanguage#OuterNode](n: T): OuterNode = n.asInstanceOf[OuterNode]

  def outerNodeToHTML(node: OuterNode): HTML = node match {
    case n: ExprNode => exprNode(n)
    case n: TypeNode => typeNode(n)
  }

  def exprNode(node: ExprNode): HTML = {
    val isAxiom = node.getVisibleChildren(mode).isEmpty

    if (!isAxiom && mode == DisplayMode.Evaluation && node.getParent.isEmpty) {
      node.checkDepthLimitWillBeExceeded()
    }

    div(
      cls := f"subtree ${if (isAxiom) "axiom" else ""} ${phantomClassName(node)}".strip,
      data("tree-path") := node.treePathString,
      data("node-string") := node.toString,
      fullExprBottomDiv(node),
      if (isAxiom)
        div(cls := "annotation-axiom", node.exprName)
      else
        div(
          cls := "args",
          node.getVisibleChildren(mode).map(outerNodeToHTML),
          div(cls := "annotation-new", node.exprName)
        )
    )
  }

  def fullExprBottomDiv(node: ExprNode): HTML = div(cls := "node", envDiv(node), exprDiv(node), resultDiv(node))

  def envDiv(node: ExprNode): HTML = {
    val env = mode match {
      case DisplayMode.Edit       => node.getEditEnv
      case DisplayMode.Evaluation => node.getEvalEnv
      case DisplayMode.TypeCheck  => node.getTypeEnv
    }
    val variablesHtml: Option[HTML] =
      if (env.isEmpty) None else Some(div(raw(env.map((k, v) => s"$k &rarr; ${v.toHtml}").mkString("[", ", ", "]"))))
    val delimiter =
      if (mode == DisplayMode.TypeCheck) Some(raw(" &#x22a2;")) else if (env.nonEmpty) Some(raw(",")) else None

    div(
      cls := "scoped-variables",
      variablesHtml,
      delimiter,
      paddingRight := (if (env.isEmpty && mode != DisplayMode.TypeCheck) "0ch" else "0.5ch")
    )
  }

  def exprDiv(node: ExprNode): HTML = div(if (node.isPhantom) {
    node.toHtmlLineReadOnly(mode)(display := "inline")
  } else {
    node.toHtmlLine(mode)
  })(cls := "expr")

  def resultDiv(node: ExprNode): Seq[HTML] = mode match {
    case DisplayMode.Edit =>
      val typeCheckResult = node.getType
      if (typeCheckResult.isError) List(typeCheckTurnstileSpan, typeCheckResultDiv(node))
      else {
        val evalResult = node.getEditValueResult
        if (!evalResult.isError && !evalResult.isPlaceholder) List(evalArrowSpan, editEvalResultDiv(node))
        else List(typeCheckTurnstileSpan, typeCheckResultDiv(node))
      }
    case DisplayMode.TypeCheck  => List(typeCheckTurnstileSpan, typeCheckResultDiv(node))
    case DisplayMode.Evaluation => List(evalArrowSpan, evalResultDiv(node))
  }

  def typeNode(node: TypeNode): HTML = {
    val isAxiom = node.getVisibleChildren(mode).isEmpty
    div(
      cls := f"subtree ${if (isAxiom) "axiom" else ""} type-tree ${phantomClassName(node)}".strip,
      data("tree-path") := node.treePathString,
      data("node-string") := node.toString,
      fullTypeBottomDiv(node),
      if (isAxiom)
        div(cls := "annotation-axiom", node.getTypeName)
      else
        div(
          cls := "args",
          node.getVisibleChildren(mode).map(outerNodeToHTML),
          div(cls := "annotation-new", node.getTypeName)
        )
    )
  }

  def fullTypeBottomDiv(node: TypeNode): HTML = div(cls := "node", typeDiv(node))

  def typeDiv(node: TypeNode): HTML = node.toHtmlLine(mode)(cls := "type")

  private val typeCheckTurnstileSpan: HTML = span(paddingLeft := "0.5ch", paddingRight := "0.5ch", raw(":"))
  private def typeCheckResultDiv(node: ExprNode): HTML =
    div(cls := "type-check-result", display := "inline", node.getType.toHtml)
  private val evalArrowSpan: HTML = span(paddingLeft := "1ch", paddingRight := "1ch", raw("&DoubleDownArrow;"))
  private def evalResultDiv(node: ExprNode): HTML = div(cls := "eval-result", display := "inline", node.getValue.toHtml)
  private def editEvalResultDiv(node: ExprNode): HTML =
    div(cls := "eval-result", display := "inline", node.getEditValueResult.toHtml)

  def phantomClassName(node: OuterNode): String = if (node.isPhantom) " phantom" else ""
}
