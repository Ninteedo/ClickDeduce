package app

import actions.exceptions.ActionInvocationException
import convertors.{DisplayMode, HTMLConvertor, LaTeXConvertor}
import languages.*
import languages.terms.blanks.BlankExprDropDown
import nodes.*
import nodes.exceptions.NodeStringParseException
import scalatags.Text.TypedTag
import scalatags.Text.all.*

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.JSExportTopLevel

/** Contains the methods for Scala.js to export */
object ScalaJsEntry {
  private val knownLanguages: List[(String, ClickDeduceLanguage)] = List(
    "LArith" -> LArith(),
    "LIf"    -> LIf(),
    "LLet"   -> LLet(),
    "LLam"   -> LLam(),
    "LRec"   -> LRec(),
    "LData"  -> LData(),
    "LPoly"  -> LPoly(),
    "LList"  -> LList(),
    "LWhile" -> LWhile(),
  )

  private def getLanguage(langName: String): ClickDeduceLanguage = knownLanguages.find(_._1 == langName) match {
    case Some((_, lang)) => lang
    case None            => throw new IllegalArgumentException(s"Unknown language: $langName")
  }

  /** Start a new node with a blank tree
    * @param langName
    *   The name of the language to start with
    * @return
    *   A tuple of the node string and the HTML representation of the tree
    */
  @JSExportTopLevel("startNodeBlank")
  def startNodeBlank(langName: String): js.Tuple2[String, String] = {
    val lang = getLanguage(langName)
    val convertor = HTMLConvertor(lang, DisplayMode.Edit)
    val tree = ExprChoiceNode(lang)
    (tree.toString, convertor.convert(tree))
  }

  /** Get a list of all known languages in the form of a select element
    * @return
    *   The HTML representation of the select element
    */
  @JSExportTopLevel("getLangSelector")
  def getLangSelector(): String = {
    val langSelector: TypedTag[String] = select(
      id := "lang-selector",
      name := "lang-name",
      knownLanguages.map(_._1).map(langName => option(value := langName, langName))
    )
    langSelector.toString
  }

  /** Process a given action beginning with a given node
    * @param langName
    *   The name of the language to use
    * @param modeName
    *   The name of the display mode to use
    * @param actionName
    *   The name of the action to perform
    * @param nodeString
    *   The string representation of the root node
    * @param treePath
    *   The path to the node to perform the action on
    * @param extraArgs
    *   Any extra arguments to pass to the action (must match expected arguments)
    * @return
    *   A tuple of the updated tree's node string and HTML representation
    */
  @JSExportTopLevel("processAction")
  def processAction(
    langName: String,
    modeName: String,
    actionName: String,
    nodeString: String,
    treePath: String,
    extraArgs: js.Array[String]
  ): js.Tuple2[String, String] = {
    val originalLang = getLanguage(langName)
    val displayMode = DisplayMode.fromString(modeName)
    val convertor = HTMLConvertor(originalLang, displayMode)
    val action = originalLang.createAction(actionName, nodeString, treePath, extraArgs.toList, modeName)
    val updatedTree = action.newTree
    (updatedTree.toString, convertor.convert(updatedTree))
  }

  /**
   * Convert a node string to LaTeX.
   * @param langName The name of the language to use
   * @param modeName The name of the display mode to use
   * @param nodeString The string representation of the root node
   * @return The LaTeX representation of the node
   * @throws ClickDeduceException If the node string cannot be parsed
   * @see LaTeXConvertor
   */
  @JSExportTopLevel("convertToLaTeX")
  def convertToLaTeX(langName: String, modeName: String, nodeString: String): String = {
    val originalLang = getLanguage(langName)
    val displayMode = DisplayMode.fromString(modeName)
    val convertor = LaTeXConvertor(originalLang, displayMode)
    Node.read(originalLang, nodeString) match {
      case Some(tree: OuterNode) => convertor.convert(tree)
      case _ => throw new ClickDeduceException(s"Failed to parse node string: $nodeString")
    }
  }

  @JSExportTopLevel("getTasks")
  def getTasks(langName: String): js.Array[js.Tuple3[String, String, Int]] = {
    val lang = getLanguage(langName)
    lang.getTasks
      .values
      .toList
      .map(task => js.Tuple3(task.name, task.description, task.difficulty))
      .toJSArray
  }

  @JSExportTopLevel("checkTask")
  def checkTask(langName: String, taskName: String, nodeString: String): Boolean = {
    val lang = getLanguage(langName)
    val node = Node.read(lang, nodeString) match {
      case Some(n: OuterNode) => n
      case Some(n) => throw new ActionInvocationException(s"Expected OuterNode, got $n")
      case _ => throw NodeStringParseException(nodeString)
    }
    val expr = node match {
      case n: ExprNode => n.getExpr
      case _ => BlankExprDropDown(lang)
    }
    lang.getTasks.get(taskName) match {
      case Some(task) => task.checkFulfilled(expr)
      case None => throw new IllegalArgumentException(s"Unknown task: $taskName")
    }
  }

  @JSExportTopLevel("getExprRulePreview")
  def getExprRulePreview(langName: String, exprName: String): String = {
    val lang = getLanguage(langName)
    val preview = lang.getExprRulePreview(exprName)
    if (preview.isEmpty) ""
    else preview.get.toHtml.toString
  }

  @JSExportTopLevel("parseExpr")
  def parseExpr(langName: String, exprText: String): String = {
    getLanguage(langName).parseExpr(exprText) match {
      case Right(expr) => expr.toString
      case _ => ""
    }
  }

  /**
   * @return tuple of (error column (or -1), error message on failure or HTML preview on success)
   */
  @JSExportTopLevel("exprParsePreviewHtml")
  def exprParsePreviewHtml(
    langName: String,
    exprText: String,
    modeName: String,
    nodeString: String,
    treePathString: String
  ): js.Tuple2[Int, String] = {
    val lang = getLanguage(langName)
    val default = ""
    lang.parseExpr(exprText) match {
      case Left(msg, col) => (col, msg)
      case Right(expr) =>
        Node.read(lang, nodeString) match {
          case Some(node: ExprNodeParent) =>
            val treePath = Node.readPathString(treePathString)
            node.findChild(treePath) match {
              case Some(child: ExprNodeParent) =>
                val mode = DisplayMode.fromString(modeName)
                val newChild = ExprNode.fromExpr(lang, expr)

                DisplayMode.values.foreach(mode => {
                  newChild.overrideEnv(child.getEnv(mode), mode)
                })

                (-1, HTMLConvertor(lang, mode).convert(newChild))
              case _ => (-1, default)
            }
          case _ => (-1, default)
        }
    }
  }

  @JSExportTopLevel("exprText")
  def exprText(langName: String, nodeString: String, treePathString: String): String = {
    val lang = getLanguage(langName)
    Node.read(lang, nodeString) match {
      case Some(node: ExprNodeParent) =>
        val treePath = Node.readPathString(treePathString)
        node.findChild(treePath) match {
          case Some(child: ExprNodeParent) =>
            val expr = child.getExpr
            val exprText = expr.toText
            exprText.asPlainText
          case _ => ""
        }
      case _ => ""
    }
  }
}
