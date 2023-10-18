package app

import languages.LArith.*
import languages.{ClickDeduceLanguage, LArith} // TODO: this should import from ClickDeduceLanguage


class ExpressionEvalTree(val language: ClickDeduceLanguage, val expr: language.Expr, val value: Option[language.Value], val env: Option[language.Env], val children: List[ExpressionEvalTree]) {

  private val XMLNS = "http://www.w3.org/2000/svg"
  private val style = "line: {stroke: black; stroke-width: 2;}, text: {font-family: sans-serif; font-size: 12px;}"

  /**
   * Convert this expression tree to a full SVG.
   *
   * @return the SVG string
   */
  def toSvg: String = {
    val svg = new StringBuilder()
    svg.append(s"""<svg xmlns="$XMLNS" width="${size._1}" height="${size._2}">""")
    svg.append(s"""<style type="text/css">$style</style>""")
    svg.append(toSvgGroup)
    svg.append("</svg>")
    svg.toString
  }

  /**
   * Convert this expression tree to an SVG group.
   */
  def toSvgGroup: String = {
    def createGroup(content: String, translateAmount: (Float, Float) = (0, 0)) = {
      s"""<g transform="translate$translateAmount">$content</g>"""
    }

    val totalWidth = size._1
    val halfWidth = totalWidth / 2

    val turnstile = "&#x22a2;"
    val arrow = "&DoubleDownArrow;"

    val line = s"""<line x1="-$halfWidth" x2="$halfWidth" y1="0" y2="0" />"""
    val exprText = new StringBuilder()
    if (env.isDefined) {
      val envText = env.get.map({ case (name, value): (Variable, Value) => s"$name := ${prettyPrint(value)}" }).mkString(", ")
      exprText.append(s"[$envText], ")
    }
    exprText.append(language.prettyPrint(expr))
    if (value.isDefined) {
      exprText.append(s" $arrow ${language.prettyPrint(value.get)}")
    }
    val textBlock = s"""<text>${exprText.toString()}</text>"""

    val thisGroup = createGroup(line + textBlock)
    val svg = new StringBuilder()
    svg.append(thisGroup)
    if (children.nonEmpty) {
      val childGroups = for {i <- children.indices} yield {
        val child = children(i)
        val childSvg = child.toSvgGroup
        createGroup(childSvg, (totalWidth * (i - children.length / 2), 20))
      }
      val childGroup = createGroup(childGroups.mkString(""), (0, -20))
      svg.append(childGroup)
    }
    svg.toString()
  }

  /**
   * Calculate the total size of the SVG for this expression tree.
   *
   * @return the size of the SVG in pixels, (width, height)
   */
  def size: (Float, Float) = {
    // TODO: calculate SVG size using font metrics
    val width = 100
    val height = 100
    (width, height)
  }
}


object ExpressionEvalTree {
  def exprToTree(e0: Expr): ExpressionEvalTree = {
//    def getExprFields(e: Expr): List[Expr] = {
//      val mirror = ru.runtimeMirror(e.getClass.getClassLoader)
//      val instanceMirror = mirror.reflect(e)
//      val fields = instanceMirror.symbol.typeSignature.members.filter(_.isTerm).toList
//      val exprFields = fields.map(field => {
//        val fieldMirror = instanceMirror.reflectField(field.asTerm)
//        fieldMirror.get match {
//          case e: Expr => Some(e)
//          case _ => None
//        }
//      })
//      exprFields.flatten
//    }

    val childExprs = Nil // getExprFields(e0)
    val childTrees = childExprs.map(exprToTree)
    ExpressionEvalTree(LArith, e0, Some(eval(e0)), None, childTrees)
  }
}
