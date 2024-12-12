package languages.terms.types

import convertors.ConvertableText

case class TypePlaceholder(content: ConvertableText, override val needsBrackets: Boolean = true) extends Type {
  override def toText: ConvertableText = content
}

object TypePlaceholder {
  def apply(typ: Type): TypePlaceholder = TypePlaceholder(typ.toText, typ.needsBrackets)
}
