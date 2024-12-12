package languages.terms.types

import convertors.{ConvertableText, TextElement}
import languages.env.TypeEnv
import languages.terms.builders.{BuilderArgs, TypeCompanion}

case class TypeContainer(typ: Type) extends Type {
  override def typeCheck(tEnv: TypeEnv): Type = typ

  override def toText: ConvertableText = typ.toText
}

object TypeContainer extends TypeCompanion {
  override val isHidden: Boolean = true

  override def create(args: BuilderArgs): Option[Type] = args match {
    case List(t: Type) => Some(TypeContainer(t))
    case Nil => Some(TypeContainer(TypePlaceholder(TextElement(""))))
    case _ => None
  }
}
