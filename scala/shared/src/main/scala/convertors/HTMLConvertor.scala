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
    if (mode == DisplayMode.Evaluation) {
      fixedNode match {
        case n: ExprNode => n.checkDepthLimitWillBeExceeded()
        case _           =>
      }
    }
    outerNodeToHTML(fixedNode).toString
  }

  private def outerNodeToHTML(node: OuterNode): HTML = node match {
    case n: ExprNode => exprNode(n)
    case n: TypeNode => typeNode(n)
  }

  def exprNode(node: ExprNode): HTML = {
    val isAxiom = node.getVisibleChildren(mode).isEmpty

    div(
      cls := f"${ClassDict.SUBTREE} ${if (isAxiom) ClassDict.AXIOM else ""} ${phantomClassName(node)}".strip,
      data("tree-path") := node.treePathString,
      fullExprBottomDiv(node),
      if (isAxiom)
        div(cls := ClassDict.ANNOTATION_AXIOM, node.exprName)
      else
        div(
          cls := ClassDict.ARGS,
          node.getVisibleChildren(mode).map(outerNodeToHTML),
          div(cls := ClassDict.ANNOTATION, node.exprName)
        )
    )
  }

  def fullExprBottomDiv(node: ExprNode): HTML =
    div(cls := ClassDict.NODE, envDiv(node), exprDiv(node), resultDiv(node))

  def envDiv(node: OuterNode): HTML = {
    val env = node match
      case n: ExprNode => n.getEnv(mode)
      case n: TypeNode => n.getEnv(mode)
    val parentEnv = node.getParent.map {
      case n1: ExprNode => n1.getEnv(mode)
      case n1: TypeNode => n1.getEnv(mode)
    }
    val filteredEnv = env
      .map((k, v) => {
        val parentValue = parentEnv.flatMap(_.get(k))
        if parentValue.contains(v) then None else Some(k -> v)
      })
      .filter(_.isDefined)
      .map(_.get)
    val parsedEnv = filteredEnv.map((k, v) =>
      v match {
        case value: lang.Value => k -> value.toHtml
        case typ: lang.Type    => k -> typ.toHtml
      }
    )
    val emptyEnv = parsedEnv.isEmpty
    val variablesHtml: Option[HTML] =
      if (emptyEnv) None else Some(div(raw(parsedEnv.map((k, v) => s"$k &rarr; $v").mkString("[", ", ", "]"))))
    val delimiter =
      if (mode == DisplayMode.TypeCheck) Some(raw(" &#x22a2;"))
      else if (!emptyEnv) Some(raw(","))
      else None

    div(
      cls := ClassDict.SCOPED_VARIABLES,
      variablesHtml,
      delimiter,
      paddingRight := (if (emptyEnv && mode != DisplayMode.TypeCheck) "0ch" else "0.5ch")
    )
  }

  def exprDiv(node: ExprNode): HTML = div(if (node.isPhantom) {
    node.toTextReadOnly(mode).asHtml
  } else {
    node.toText(mode).asHtml
  })(cls := ClassDict.EXPR)

  def resultDiv(node: ExprNode): Seq[HTML] = mode match {
    case DisplayMode.Edit =>
      val typeCheckResult = node.getType
      if (typeCheckResult.isError) List(typeCheckTurnstileSpan, typeCheckResultDiv(node))
      else {
        val evalResult: lang.Value = node.getEditValueResult
        if (!evalResult.isError && !evalResult.isPlaceholder) List(evalArrowSpan, editEvalResultDiv(node))
        else List(typeCheckTurnstileSpan, typeCheckResultDiv(node))
      }
    case DisplayMode.TypeCheck  => List(typeCheckTurnstileSpan, typeCheckResultDiv(node))
    case DisplayMode.Evaluation => List(evalArrowSpan, evalResultDiv(node))
  }

  def typeNode(node: TypeNode): HTML = {
    val isAxiom = node.getVisibleChildren(mode).isEmpty
    div(
      cls := List(
        ClassDict.SUBTREE,
        { if (isAxiom) ClassDict.AXIOM else "" },
        ClassDict.TYPE_TREE,
        phantomClassName(node)
      ).mkString(" "),
      data("tree-path") := node.treePathString,
      fullTypeBottomDiv(node),
      if (isAxiom)
        div(cls := ClassDict.ANNOTATION_AXIOM, node.getTypeName)
      else
        div(
          cls := ClassDict.ARGS,
          node.getVisibleChildren(mode).map(outerNodeToHTML),
          div(cls := ClassDict.ANNOTATION, node.getTypeName)
        )
    )
  }

  def fullTypeBottomDiv(node: TypeNode): HTML =
    div(cls := ClassDict.NODE, envDiv(node), typeDiv(node), typeResultDiv(node))

  def typeDiv(node: TypeNode): HTML = node.toText(mode).asHtml(cls := ClassDict.TYPE)

  def typeResultDiv(node: TypeNode): HTML =
    div(cls := ClassDict.TYPE_CHECK_RESULT, typeCheckTurnstileSpan, node.getTypeCheckResult(mode).toHtml)

  private val typeCheckTurnstileSpan: HTML = span(paddingLeft := "0.5ch", paddingRight := "0.5ch", raw(":"))
  private def typeCheckResultDiv(node: ExprNode): HTML =
    div(cls := ClassDict.TYPE_CHECK_RESULT, node.getType.toHtml)
  private val evalArrowSpan: HTML = span(paddingLeft := "1ch", paddingRight := "1ch", raw("&DoubleDownArrow;"))
  private def evalResultDiv(node: ExprNode): HTML = div(cls := ClassDict.EVAL_RESULT, node.getValue.toHtml)
  private def editEvalResultDiv(node: ExprNode): HTML =
    div(cls := ClassDict.EVAL_RESULT, node.getEditValueResult.toHtml)

  private def phantomClassName(node: OuterNode): String = if (node.isPhantom) ClassDict.PHANTOM else ""
}
