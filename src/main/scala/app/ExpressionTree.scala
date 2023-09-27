package app

import languages.LArith.{Expr, Type, Value, prettyPrint}  // TODO: this should import from ClickDeduceLanguage

class ExpressionTree(val expr: Expr, val value: Option[Value], val typ: Option[Type], val children: List[ExpressionTree]) {


  /**
   * Convert this expression tree to an SVG object.
   *
   * Does not include the `svg` or `style` tags.
   */
  def toSvg: String = {
    def createGroup(content: String, translateAmount: (Int, Int) = (0, 0)) = {
      s"""<g transform="translate$translateAmount">$content</g>"""
    }

    val totalWidth = 100
    val halfWidth = totalWidth / 2

    val turnstile = "&#x22a2;"
    val arrow = "&DoubleDownArrow;"

    val line = s"""<line x1="-$halfWidth" x2="$halfWidth" y1="0" y2="0" />"""
    val exprText = new StringBuilder()
    exprText.append(prettyPrint(expr))
    if (value.isDefined) {
      exprText.append(s" $arrow ${prettyPrint(value.get)}")
    }
    if (typ.isDefined) {
      exprText.append(s" $turnstile ${prettyPrint(typ.get)}")
    }
    val textBlock = s"""<text>${exprText.toString()}</text>"""

    val thisGroup = createGroup(line + textBlock)
    val svg = new StringBuilder()
    svg.append(thisGroup)
    if (children.nonEmpty) {
      val childGroups = for {i <- children.indices} yield {
        val child = children(i)
        val childSvg = child.toSvg
        createGroup(childSvg, (totalWidth * (i - children.length / 2), 20))
      }
      val childGroup = createGroup(childGroups.mkString(""), (0, -20))
      svg.append(childGroup)
    }
    svg.toString()
  }
}
