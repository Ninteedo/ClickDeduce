package convertors

import convertors.text.Symbols
import languages.ClickDeduceLanguage
import languages.env.{Env, TypeEnv, ValueEnv}
import languages.terms.Term
import languages.terms.types.Type
import languages.terms.values.Value
import nodes.*
import scalatags.Text.TypedTag
import scalatags.Text.all.*

class HTMLConvertor(lang: ClickDeduceLanguage, mode: DisplayMode) extends IConvertor(lang, mode) {
  private type HTML = TypedTag[String]

  def convert(node: OuterNode): Output = {
    if (mode == DisplayMode.Evaluation) {
      node match {
        case n: ExprNode => n.checkDepthLimitWillBeExceeded()
        case _           =>
      }
    }
    val startingEnvIndex = node match {
      case n: ExprNodeParent => if n.getEnv(mode).isEmpty then -1 else 0
      case _ => -1
    }
    envCounter = startingEnvIndex
    outerNodeToHTML(node, startingEnvIndex).toString
  }

  private def outerNodeToHTML(node: OuterNode, envIndex: Int): HTML = node match {
    case n: ExprNodeParent => exprNode(n, envIndex)
    case n: TypeNodeParent => typeNode(n, envIndex)
  }

  private def exprNode(node: ExprNodeParent, envIndex: Int): HTML = {
    val isAxiom = node.getVisibleChildren(mode).isEmpty

    val newEnvIndex = if (node.hasUpdatedEnv(mode)) {
      nextEnvIndex
    } else {
      envIndex
    }

    div(
      cls := f"${ClassDict.SUBTREE} ${if (isAxiom) ClassDict.AXIOM else ""} ${phantomClassName(node)}".strip,
      data("tree-path") := node.treePathString,
      data("node-string") := node.toString,
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

  private def fullExprBottomDiv(node: ExprNodeParent, envIndex: Int): HTML =
    div(cls := ClassDict.NODE, envDiv(node, envIndex, typeMode = false), exprDiv(node), resultDiv(node))

  private var envCounter: Int = 0

  private def nextEnvIndex: Int = {
    val res = envCounter
    envCounter += 1
    res
  }

  private def envDiv(node: OuterNode, envIndex: Int, typeMode: Boolean): HTML = {
    def parseEnv(
      env: Env[Term],
      valueTooltips: Boolean
    ): Iterable[(String, TypedTag[String])] =
      env.map((k, v) =>
        v match {
          case value: Value => k -> (if valueTooltips then value.toHtml else value.valueText)
          case typ: Type    => k -> (if valueTooltips then typ.toHtml else typ.valueText)
        }
      )

    val envMode = if (typeMode) DisplayMode.TypeCheck else mode

    def getNodeEnv(node: OuterNode): ValueEnv | TypeEnv = node match {
      case n: ExprNodeParent => n.getEnv(envMode)
      case n: TypeNodeParent => n.getEnv(envMode)
    }

    val parentNode = node.getParent
    val parentEnv = {
      val res = parentNode.map(getNodeEnv)
      if (typeMode && parentNode.exists(_.isInstanceOf[ExprNode])) res.map(TypeEnv.typeVariableEnv) else res
    }
    val filteredEnv: Env[Term] = getNodeEnv(node)
      .filterToEnv((k, v) => {
        val parentValue = parentEnv.flatMap(_.get(k))
        !parentValue.contains(v)
      }).asInstanceOf[Env[Term]]
    val parsedEnv = parseEnv(filteredEnv, valueTooltips = true)
    val delimiter = if (envMode == DisplayMode.TypeCheck) raw(" &#x22a2;&nbsp;") else raw(",&nbsp;")

    if (parentEnv.isDefined && parentEnv.get.nonEmpty) {
      val parsedParentEnv = parseEnv(parentEnv.get.asInstanceOf[Env[Term]], valueTooltips = false)
      val envChar = if (mode == DisplayMode.TypeCheck) Symbols.gamma.asPlainText else Symbols.sigma.asPlainText
      val envLabel = span(envChar, sub(envIndex), if (typeMode) sup(raw("τ")) else raw(""))
      val miniParent = span(cls := ClassDict.TOOLTIP, envLabel, div(cls := ClassDict.TOOLTIP_TEXT, formatEnv(parsedParentEnv, breaks = true)))

      if (parsedEnv.nonEmpty) span(miniParent, " + ", formatEnv(parsedEnv), delimiter) else span(miniParent, delimiter)
    } else if (parsedEnv.nonEmpty) span(formatEnv(parsedEnv), delimiter)
    else span()
  }

  private def formatEnv(env: Iterable[(String, TypedTag[String])], breaks: Boolean = false): TypedTag[String] = {
    val variablesHtml: Option[HTML] =
      if (env.isEmpty) None
      else Some(div(raw(env.map((k, v) => s"$k = $v").mkString("[", if breaks then ",<br/>" else ", ", "]"))))
    div(cls := ClassDict.SCOPED_VARIABLES, variablesHtml)
  }

  private def exprDiv(node: ExprNodeParent): HTML = div(if (node.isPhantom) {
    node.toTextReadOnly(mode).asHtml
  } else {
    node.toText(mode).asHtml
  })(cls := ClassDict.EXPR)

  private def resultDiv(node: ExprNodeParent): Seq[HTML] = mode match {
    case DisplayMode.Edit =>
      val typeCheckResult = node.getType
      if (typeCheckResult.isError) List(typeCheckTurnstileSpan, typeCheckResultDiv(node))
      else {
        val evalResult: Value = node.getEditValueResult
        if (!evalResult.isError && !evalResult.isPlaceholder) List(evalArrowSpan, editEvalResultDiv(node))
        else List(typeCheckTurnstileSpan, typeCheckResultDiv(node))
      }
    case DisplayMode.TypeCheck  => List(typeCheckTurnstileSpan, typeCheckResultDiv(node))
    case DisplayMode.Evaluation => List(evalArrowSpan, evalResultDiv(node))
  }

  private def typeNode(node: TypeNodeParent, envIndex: Int): HTML = {
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

  private def fullTypeBottomDiv(node: TypeNodeParent, envIndex: Int): HTML =
    div(cls := ClassDict.NODE, envDiv(node, envIndex, typeMode = true), typeDiv(node), typeResultDiv(node))

  private def typeDiv(node: TypeNodeParent): HTML = node.toText(mode).asHtml(cls := ClassDict.TYPE)

  private def typeResultDiv(node: TypeNodeParent): HTML =
    div(cls := ClassDict.TYPE_CHECK_RESULT, typeCheckTurnstileSpan, node.getTypeCheckResult(mode).toHtml)

  private val typeCheckTurnstileSpan: HTML = span(paddingLeft := "0.5ch", paddingRight := "0.5ch", raw(":"))
  private def typeCheckResultDiv(node: ExprNodeParent): HTML =
    div(cls := ClassDict.TYPE_CHECK_RESULT, node.getType.toHtml)
  private val evalArrowSpan: HTML = span(paddingLeft := "1ch", paddingRight := "1ch", raw("&DoubleDownArrow;"))
  private def evalResultDiv(node: ExprNodeParent): HTML = div(cls := ClassDict.EVAL_RESULT, node.getValue.toHtml)
  private def editEvalResultDiv(node: ExprNodeParent): HTML =
    div(cls := ClassDict.EVAL_RESULT, node.getEditValueResult.toHtml)

  private def phantomClassName(node: OuterNode): String = if (node.isPhantom) ClassDict.PHANTOM else ""
}
