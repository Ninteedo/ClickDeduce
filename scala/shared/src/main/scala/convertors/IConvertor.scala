package convertors

import languages.ClickDeduceLanguage
import nodes.OuterNode

trait IConvertor(lang: ClickDeduceLanguage, mode: DisplayMode) {
  protected type Output = String

  protected type Mode = DisplayMode

  def convert(node: OuterNode): Output
}
