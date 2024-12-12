package languages.terms.types

import app.HTMLHelper
import convertors.ClassDict
import languages.env.TypeEnv
import languages.terms.Term
import scalatags.Text.TypedTag
import scalatags.Text.all.*

/** The type of a value. Can also appear in expressions.
 */
abstract class Type extends Term {
  override lazy val toHtml: TypedTag[String] = HTMLHelper.tooltip(valueText, span(tooltipText))

  lazy val tooltipText: String = toString

  lazy val valueText: TypedTag[String] = div(toText.asHtml, cls := ClassDict.VALUE_TYPE)

  val isError: Boolean = false

  def typeCheck(tEnv: TypeEnv): Type = this

  private def getTypeFields: List[Type] = this match {
    case t0: Product => t0.productIterator.toList.collect({ case t: Type => t })
    case _           => Nil
  }

  private def defaultChildren(env: TypeEnv): List[(Term, TypeEnv)] = getTypeFields.map(t => (t, env))

  override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = defaultChildren(tEnv)
}
