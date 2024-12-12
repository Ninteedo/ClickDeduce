package nodes

import app.{ClickDeduceException, UtilityFunctions}
import convertors.{ConvertableText, DisplayMode}
import languages.AbstractNodeLanguage
import languages.terms.blanks.BlankTypeDropDown
import languages.terms.literals.Literal
import languages.terms.types.{Type, TypePlaceholder}
import scalatags.Text.all.s

/** Implementation of a type node.
  *
  * Analogous to [[ExprNode]], but for types.
  *
  * @param typeName
  *   The name of the type.
  * @param args
  *   The arguments of the type.
  */
case class TypeNode(lang: AbstractNodeLanguage, typeName: String, args: List[InnerNode]) extends TypeNodeParent {
  override lazy val getType: Type = {
    val arguments = args.map {
      case tn: SubTypeNode => tn.node.getType
      case ln: LiteralNode => ln.getLiteral
    }
    lang.buildType(typeName, arguments)
  }
  override val name: String = "TypeNode"
  override val children: List[OuterNode] = args.filter(_.isInstanceOf[SubTypeNode]).flatMap(_.children)

  override def toText(mode: DisplayMode): ConvertableText = {
    val arguments = args.map {
      case n: LiteralNode => n.getPlaceholder(mode, false)
      case other          => other.getPlaceholder(mode)
    }
    lang.buildType(typeName, arguments).toText
  }

  override def toTextReadOnly(mode: DisplayMode): ConvertableText = {
    val arguments = args.map {
      case n: LiteralNode => Literal.placeholderOf(n.literal, n.toHtmlLineReadOnly(mode).toString)
      case n: SubTypeNode => TypePlaceholder(n.node.toTextReadOnly(mode), n.node.getType.needsBrackets)
    }
    lang.buildType(typeName, arguments).toText
  }

  override def toString: String = s"TypeNode(${UtilityFunctions.quote(typeName)}, $args)"

  children.foreach(_.setParent(Some(this)))
  args.foreach(_.setParent(Some(this)))
}

/** Companion object for [[TypeNode]].
 */
object TypeNode {

  /** Create a new [[TypeNode]] from a type name.
   *
   * @param typeName
   * The type name.
   * @return
   * The new [[TypeNode]].
   * @throws ClickDeduceException
   * If the type name is not recognised in this language, or if there is no default type for the given name.
   */
  def fromTypeName(lang: AbstractNodeLanguage, typeName: String): Option[TypeNode] = lang.getTypeBuilder(typeName) match {
    case Some(builder) =>
      val arguments = builder(Nil) match {
        case Some(e: Product) =>
          e.productIterator.toList.collect({
            case c: Literal => LiteralNode(c)
            case c: Type => SubTypeNode(TypeNode.fromType(lang, c))
            case c => throw new ClickDeduceException(s"Unexpected parameter type in createFromTypeName: $c")
          }
          )
        case _ => throw new ClickDeduceException(s"No default type for $typeName")
      }
      Some(TypeNode(lang, typeName, arguments))
    case None => None
  }

  /** Create a new [[TypeNode]] from a type.
   *
   * @param typ
   * The type.
   * @return
   * The new [[TypeNode]], matching the structure of the given type.
   */
  def fromType(lang: AbstractNodeLanguage, typ: Type): TypeNodeParent = typ match {
    case blank: BlankTypeDropDown => TypeChoiceNode(lang)
    case _ =>
      val innerNodes = typ match {
        case e0: Product =>
          e0.productIterator.toList.collect({
            case c: Literal => LiteralNode(c)
            case c: Type => SubTypeNode(TypeNode.fromType(lang, c))
          }
          )
      }
      val result = TypeNode(lang, typ.name, innerNodes)
      innerNodes.foreach(_.setParent(Some(result)))
      result
  }
}
