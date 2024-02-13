package convertors

import languages.{AbstractNodeLanguage, ClickDeduceLanguage}

trait IConvertor(val lang: ClickDeduceLanguage, mode: DisplayMode) {
  protected type Output = String

  protected type Mode = DisplayMode

//  type OuterNode = lang.OuterNode

  def convert[T <: AbstractNodeLanguage#OuterNode](node: T): Output
}
