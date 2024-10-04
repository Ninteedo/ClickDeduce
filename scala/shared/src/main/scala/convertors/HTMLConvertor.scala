package convertors

import languages.{AbstractNodeLanguage, ClickDeduceLanguage}
import scalatags.Text.TypedTag
import scalatags.Text.all.*

class HTMLConvertor(lang: ClickDeduceLanguage, mode: DisplayMode) extends IConvertor(lang, mode) {
  private type OuterNode = lang.OuterNode
  private type HTML = TypedTag[String]
  private type ExprNode = lang.ExprNode
  private type TypeNode = lang.TypeNodeParent

  def convert(node: AbstractNodeLanguage#OuterNode): Output = {
    val fixedNode = node.asInstanceOf[OuterNode]
    if (mode == DisplayMode.Evaluation) {
      fixedNode match {
        case n: ExprNode => n.checkDepthLimitWillBeExceeded()
        case _           =>
      }
    }
    envCounter = -1
    outerNodeToHTML(fixedNode, 0).toString
  }

  private def outerNodeToHTML(node: OuterNode, envIndex: Int): HTML = node match {
    case n: ExprNode => exprNode(n, envIndex)
    case n: TypeNode => typeNode(n, envIndex)
  }

  private def exprNode(node: ExprNode, envIndex: Int): HTML = {
    val isAxiom = node.getVisibleChildren(mode).isEmpty

    val newEnvIndex = if (node.hasUpdatedEnv(mode)) {
      envCounter += 1
      envCounter - 1
    } else {
      envIndex
    }

    div(
      cls := f"${ClassDict.SUBTREE} ${if (isAxiom) ClassDict.AXIOM else ""} ${phantomClassName(node)}".strip,
      data("tree-path") := node.treePathString,
      fullExprBottomDiv(node, envIndex),
      if (isAxiom)
        div(cls := ClassDict.ANNOTATION_AXIOM, node.exprName)
      else
        div(
          cls := ClassDict.ARGS,
          node.getVisibleChildren(mode).map(outerNodeToHTML(_, newEnvIndex)),
          div(cls := ClassDict.ANNOTATION, node.exprName)
        )
    )
  }

  private def fullExprBottomDiv(node: ExprNode, envIndex: Int): HTML =
    div(cls := ClassDict.NODE, envDiv(node, envIndex, typeMode = false), exprDiv(node), resultDiv(node))

  private var envCounter: Int = 0

  private def envDiv(node: OuterNode, envIndex: Int, typeMode: Boolean): HTML = {
    def parseEnv(
      env: Iterable[(lang.Variable, lang.Term)],
      valueTooltips: Boolean
    ): Iterable[(lang.Variable, TypedTag[String])] =
      env.map((k, v) =>
        v match {
          case value: lang.Value => k -> (if valueTooltips then value.toHtml else value.valueText)
          case typ: lang.Type    => k -> (if valueTooltips then typ.toHtml else typ.valueText)
        }
      )

    val envMode = if (typeMode) DisplayMode.TypeCheck else mode

    def getNodeEnv(node: OuterNode): lang.ValueEnv | lang.TypeEnv = node match {
      case n: ExprNode => n.getEnv(envMode)
      case n: TypeNode => n.getEnv(envMode)
    }

    val parentEnv = {
      val res = node.getParent.map(getNodeEnv)
      if (typeMode) res.map(lang.typeVariableEnv) else res
    }
    val filteredEnv = getNodeEnv(node)
      .map((k, v) => {
        val parentValue = parentEnv.flatMap(_.get(k))
        if parentValue.contains(v) then None else Some(k -> v)
      })
      .filter(_.isDefined)
      .map(_.get)
    val parsedEnv = parseEnv(filteredEnv, valueTooltips = true)
    val delimiter = if (envMode == DisplayMode.TypeCheck) raw(" &#x22a2;&nbsp;") else raw(",&nbsp;")

    if (parentEnv.isDefined && parentEnv.get.nonEmpty) {
      val parsedParentEnv = parseEnv(parentEnv.get.env, valueTooltips = false)
      val miniParent = span(cls := ClassDict.TOOLTIP, s"σ$envIndex", div(cls := ClassDict.TOOLTIP_TEXT, formatEnv(parsedParentEnv)))

      if (parsedEnv.nonEmpty) span(miniParent, " + ", formatEnv(parsedEnv), delimiter) else span(miniParent, delimiter)
    } else if (parsedEnv.nonEmpty) span(formatEnv(parsedEnv), delimiter)
    else span()
  }

  private def formatEnv(env: Iterable[(lang.Variable, TypedTag[String])]): TypedTag[String] = {
    val variablesHtml: Option[HTML] =
      if (env.isEmpty) None
      else Some(div(raw(env.map((k, v) => s"$k &rarr; $v").mkString("[", ", ", "]"))))
    div(cls := ClassDict.SCOPED_VARIABLES, variablesHtml)
  }

  private def exprDiv(node: ExprNode): HTML = div(if (node.isPhantom) {
    node.toTextReadOnly(mode).asHtml
  } else {
    node.toText(mode).asHtml
  })(cls := ClassDict.EXPR)

  private def resultDiv(node: ExprNode): Seq[HTML] = mode match {
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

  private def typeNode(node: TypeNode, envIndex: Int): HTML = {
    val isAxiom = node.getVisibleChildren(mode).isEmpty
    div(
      cls := List(
        ClassDict.SUBTREE,
        { if (isAxiom) ClassDict.AXIOM else "" },
        ClassDict.TYPE_TREE,
        phantomClassName(node)
      ).mkString(" "),
      data("tree-path") := node.treePathString,
      fullTypeBottomDiv(node, envIndex),
      if (isAxiom)
        div(cls := ClassDict.ANNOTATION_AXIOM, node.getTypeName)
      else
        div(
          cls := ClassDict.ARGS,
          node.getVisibleChildren(mode).map(outerNodeToHTML(_, envIndex)),
          div(cls := ClassDict.ANNOTATION, node.getTypeName)
        )
    )
  }

  private def fullTypeBottomDiv(node: TypeNode, envIndex: Int): HTML =
    div(cls := ClassDict.NODE, envDiv(node, envIndex, typeMode = true), typeDiv(node), typeResultDiv(node))

  private def typeDiv(node: TypeNode): HTML = node.toText(mode).asHtml(cls := ClassDict.TYPE)

  private def typeResultDiv(node: TypeNode): HTML =
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
