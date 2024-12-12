package languages.terms.types

import convertors.{ConvertableText, TextElement}
import languages.terms.builders.{BuilderArgs, TypeCompanion}

case class UnknownType() extends Type {
  override val needsBrackets: Boolean = false

  override def toText: ConvertableText = TextElement("Unknown")
}

object UnknownType extends TypeCompanion {
  override val isHidden: Boolean = true

  override def create(args: BuilderArgs): Option[Type] = args match {
    case Nil => Some(UnknownType())
    case _   => None
  }
}
