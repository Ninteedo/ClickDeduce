package convertors

import languages.{AbstractNodeLanguage, ClickDeduceLanguage}

trait IConvertor(val lang: ClickDeduceLanguage, mode: DisplayMode) {
  protected type Output = String

  protected type Mode = DisplayMode

  def convert(node: AbstractNodeLanguage#OuterNode): Output
}
