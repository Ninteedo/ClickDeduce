package app

import java.awt.{Font, FontMetrics, Toolkit}

object FontWidthCalculator {
  val defaultFont = "Courier New"

  def calculateWidth(text: String, font: Font): Float = {
    val toolkit = Toolkit.getDefaultToolkit
    val metrics = toolkit.getFontMetrics(font)
    metrics.stringWidth(text)
  }
}
