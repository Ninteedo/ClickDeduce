package nodes

import convertors.{ConvertableText, DisplayMode, HtmlElement}
import languages.env.{Env, TypeEnv, ValueEnv}
import languages.terms.literals.Literal
import scalatags.Text.TypedTag

/** An inner node that represents a literal field.
  *
  * @param literal
  *   The current literal.
  */
case class LiteralNode(literal: Literal) extends InnerNode {
  lazy val literalText: String = literal.getValue
  lazy val getLiteral: Literal = literal
  override val name: String = "LiteralNode"
  override val children: List[OuterNode] = Nil

  override def toText(mode: DisplayMode): ConvertableText = HtmlElement(toHtmlLine(mode), getLiteral.toText)

  override def toTextReadOnly(mode: DisplayMode): ConvertableText =
    HtmlElement(toHtmlLineReadOnly(mode), getLiteral.toText)

  override def getPlaceholder(mode: DisplayMode, readOnly: Boolean = true): Literal =
    Literal.placeholderOf(literal, (if (readOnly) toHtmlLineReadOnly(mode) else toHtmlLine(mode)).toString)

  def toHtmlLine(mode: DisplayMode): TypedTag[String] = literal.toHtmlInput(treePathString, getEnv(mode))

  def getEnv(mode: DisplayMode): ValueEnv | TypeEnv = getParent match
    case Some(exprNode: ExprNodeParent)       => exprNode.getEnv(mode)
    case Some(typeNode: TypeNodeParent) => typeNode.getEnv(mode)
    case None                           => Env()

  def toHtmlLineReadOnly(mode: DisplayMode): TypedTag[String] = literal.toHtmlInputReadOnly(treePathString)
}
