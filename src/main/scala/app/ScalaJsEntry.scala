package app

import convertors.{DisplayMode, HTMLConvertor}
import languages.*
import scalatags.Text.TypedTag
import scalatags.Text.all.*

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

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

  @JSExportTopLevel("startNodeBlank")
  def startNodeBlank(langName: String): js.Tuple2[String, String] = {
    val lang = WebServer.getLanguage(langName)
    val convertor = HTMLConvertor(lang, DisplayMode.Edit)
    val tree = convertor.lang.ExprChoiceNode()
    (tree.toString, convertor.convert(tree))
  }

  @JSExportTopLevel("getLangSelector")
  def getLangSelector(): String = {
    val langSelector: TypedTag[String] = select(
      id := "lang-selector",
      name := "lang-name",
      WebServer.knownLanguages.map(_._1).map(langName => option(value := langName, langName))
    )
    langSelector.toString
  }

  @JSExportTopLevel("processAction")
  def processAction(
    langName: String,
    modeName: String,
    actionName: String,
    nodeString: String,
    treePath: String,
    extraArgs: js.Array[String]
  ): js.Tuple2[String, String] = {
    val originalLang = WebServer.getLanguage(langName)
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

  private def mapToJson(map: Map[String, String]): String = {
    val json = map.map { case (k, v) => s""""$k":"$v"""" }.mkString(",")
    s"{$json}"
  }
}
