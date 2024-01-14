package app

import java.awt.{Font, FontMetrics, Toolkit}

object FontWidthCalculator {
  val defaultFont = "Courier New"

  def calculateWidth(text: String, font: Font): Float = {
    val toolkit = Toolkit.getDefaultToolkit
    val metrics = toolkit.getFontMetrics(font)
    metrics.stringWidth(replaceHtmlCharacterCodes(text))
  }

  def replaceHtmlCharacterCodes(text: String): String = {
    val mapping = Map("&DoubleDownArrow;" -> "⇓", "&#x22a2;" -> "⊢")
    mapping.foldLeft(text) { case (s, (code, replacement)) =>
      s.replace(code, replacement)
    }
  }
}
