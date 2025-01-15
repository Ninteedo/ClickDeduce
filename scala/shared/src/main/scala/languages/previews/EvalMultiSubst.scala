package languages.previews

import convertors.text.*

case class EvalMultiSubst(subIn: ConvertableText, pairs: EvalSubst*) extends InferenceRulePart {
  override def toText: ConvertableText = ListElement(
    pairs.map(p => MultiElement(p.l, Symbols.forwardSlash, p.r))
  )
}
