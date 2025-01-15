package nodes

import app.{ClickDeduceException, UtilityFunctions}
import convertors.DisplayMode
import convertors.text.{ConvertableText, HtmlElement}
import languages.AbstractNodeLanguage
import languages.terms.Term
import languages.terms.blanks.BlankExprDropDown
import languages.terms.exprs.Expr
import languages.terms.literals.Literal
import languages.terms.types.Type
import scalatags.Text.TypedTag

/** Simple expression node implementation using an expression name and a list of arguments.
  *
  * Currently the only expression node implementation.
  */
case class ExprNode(lang: AbstractNodeLanguage, exprName: String, args: List[InnerNode] = Nil) extends ExprNodeParent(lang) {
  lazy val expr: Expr = {
    val arguments = args.map {
      case n: SubExprNode => n.node.getExpr
      case n: LiteralNode => n.getLiteral
      case n: SubTypeNode => n.node.getType
    }
    lang.buildExpr(exprName, arguments)
  }
  override val name: String = "VariableNode"
  override val children: List[OuterNode] = args.flatMap(_.children)
  private val htmlLineCache = collection.mutable.Map[DisplayMode, TypedTag[String]]()
  private val htmlLineReadOnlyCache = collection.mutable.Map[DisplayMode, TypedTag[String]]()
  private var exprOverride: Option[Expr] = None

  /** Set this node's expression to the given expression, ignoring the class parameters.
    *
    * This means that [[getExpr]] will return the given expression, rather than what it would normally return.
    *
    * @param e
    *   The expression to set this node's expression to.
    */
  def overrideExpr(e: Expr): Unit = {
    exprOverride = Some(e)
  }

  override def toText(mode: DisplayMode): ConvertableText =
    HtmlElement(getExprHtmlLine(mode).asHtml, getExpr.toText)

  override def getExpr: Expr = exprOverride.getOrElse(expr)

  private def getExprHtmlLine(mode: DisplayMode): ConvertableText = {
    val arguments: List[Term] = args.map {
      case n: SubExprNode => n.getPlaceholder(mode)
      case n: LiteralNode => n.getPlaceholder(mode, false)
      case n: SubTypeNode => n.getPlaceholder(mode)
    }
    lang.buildExpr(exprName, arguments).toText
  }

  override def toTextReadOnly(mode: DisplayMode): ConvertableText =
    HtmlElement(getExprHtmlLineReadOnly(mode).asHtmlReadOnly, getExpr.toText)

  private def getExprHtmlLineReadOnly(mode: DisplayMode): ConvertableText = {
    val arguments = args.map(_.getPlaceholder(mode))
    lang.buildExpr(exprName, arguments).toText
  }

  override def toString: String = s"VariableNode(${UtilityFunctions.quote(exprName)}, $args)"

  children.foreach(_.setParent(Some(this)))
  args.foreach(_.setParent(Some(this)))
}

/** Companion object for [[ExprNode]].
  */
object ExprNode {

  /** Create a new [[ExprNode]] from an expression name.
    *
    * @param exprName
    *   The expression name.
    * @return
    *   The new [[ExprNode]].
    * @throws ClickDeduceException
    *   If the expression name is not recognised in this language, or if there is no default expression for the given
    *   name.
    */
  def createFromExprName(lang: AbstractNodeLanguage, exprName: String): Option[ExprNode] = {
    val innerNodes: List[InnerNode] = lang.buildExpr(exprName, Nil) match {
      case e: Product =>
        e.productIterator.toList.collect({
          case c: Expr    => SubExprNode(ExprChoiceNode(lang))
          case c: Literal => LiteralNode(c)
          case c: Type    => SubTypeNode(TypeChoiceNode(lang))
          case c          => throw new ClickDeduceException(s"Unexpected parameter type in createFromExpr: $c")
        })
      case _ => throw new ClickDeduceException(s"No default expression for $exprName")
    }
    val result = ExprNode(lang, exprName, innerNodes)
    innerNodes.foreach(_.setParent(Some(result)))
    Some(result)
  }

  /** Create a new [[ExprNode]] from an expression.
    *
    * @param e
    *   The expression.
    * @return
    *   The new [[ExprNode]], matching the structure of the given expression.
    */
  def fromExpr(lang: AbstractNodeLanguage, e: Expr): ExprNodeParent = e match {
    case blank: BlankExprDropDown => ExprChoiceNode(lang)
    case e =>
      val innerNodes = e match {
        case e0: Product =>
          val values = e0.productIterator.toList
          values.collect({
            case c: Expr    => SubExprNode(ExprNode.fromExpr(lang, c))
            case c: Literal => LiteralNode(c)
            case c: Type    => SubTypeNode(TypeNode.fromType(lang, c))
          })
      }
      val result = ExprNode(lang, e.name, innerNodes)
      result.overrideExpr(e)
      innerNodes.foreach(_.setParent(Some(result)))
      result
  }
}
