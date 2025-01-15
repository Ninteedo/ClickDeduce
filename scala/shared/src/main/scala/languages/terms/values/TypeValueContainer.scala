package languages.terms.values

import convertors.text.ConvertableText
import languages.terms.types.Type

case class TypeValueContainer(typ: Type) extends Value {
  override val showInValueLookupList: Boolean = false

  override def valueTextShowType: Boolean = false

  override def toText: ConvertableText = typ.toText
}
