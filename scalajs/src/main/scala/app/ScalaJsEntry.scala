package app

import convertors.{DisplayMode, HTMLConvertor}
import languages.*
import scalatags.Text.TypedTag
import scalatags.Text.all.*

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

/**
 * Contains the methods for Scala.js to export
 */
object ScalaJsEntry {
  private val knownLanguages: List[(String, ClickDeduceLanguage)] = List(
    "LArith" -> LArith(),
    "LIf" -> LIf(),
    "LLet" -> LLet(),
    "LLam" -> LLam(),
    "LRec" -> LRec(),
    "LData" -> LData(),
    "LPoly" -> LPoly()
  )

  private def getLanguage(langName: String): ClickDeduceLanguage = knownLanguages.find(_._1 == langName) match {
    case Some((_, lang)) => lang
    case None            => throw new IllegalArgumentException(s"Unknown language: $langName")
  }

  /**
   * Start a new node with a blank tree
   * @param langName The name of the language to start with
   * @return A tuple of the node string and the HTML representation of the tree
   */
  @JSExportTopLevel("startNodeBlank")
  def startNodeBlank(langName: String): js.Tuple2[String, String] = {
    val lang = getLanguage(langName)
    val convertor = HTMLConvertor(lang, DisplayMode.Edit)
    val tree = convertor.lang.ExprChoiceNode()
    (tree.toString, convertor.convert(tree))
  }

  /**
   * Get a list of all known languages in the form of a select element
   * @return The HTML representation of the select element
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

  /**
   * Process a given action beginning with a given node
   * @param langName The name of the language to use
   * @param modeName The name of the display mode to use
   * @param actionName The name of the action to perform
   * @param nodeString The string representation of the root node
   * @param treePath The path to the node to perform the action on
   * @param extraArgs Any extra arguments to pass to the action (must match expected arguments)
   * @return A tuple of the updated tree's node string and HTML representation
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
    val displayMode: DisplayMode = DisplayMode.fromString(modeName)
    val convertor = HTMLConvertor(originalLang, displayMode)
    val action = convertor.lang.createAction(
      actionName,
      nodeString,
      treePath,
      extraArgs.toList,
      modeName
    )
    val updatedTree = action.newTree
    (updatedTree.toString, convertor.convert(updatedTree))
  }
}
