package languages.previews

import convertors.text.*

case class EvalMultiSubst(subIn: ConvertableText, pairs: EvalSubst*) extends RulePart {
  override def toText: ConvertableText = MultiElement(
    subIn,
    ListElement(pairs.map(p => MultiElement(p.l, Symbols.forwardSlash, p.r)))
  )
}
