package languages.previews

import convertors.ClassDict
import scalatags.Text.TypedTag
import scalatags.Text.all.*

abstract class InferenceRulePreview {
  val assumptions: Seq[InferenceRulePart]
  val conclusion: InferenceRulePart

  protected val divClass: String

  def toHtml: TypedTag[String] = div(
    cls := divClass,
    div(cls := ClassDict.RULE_PREVIEW_ASSUMPTIONS, assumptions.map(a => a.toText.asHtml)),
    div(cls := ClassDict.RULE_PREVIEW_CONCLUSION, conclusion.toText.asHtml)
  )
}
