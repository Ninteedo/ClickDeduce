package languages.terms.exprs

import convertors.{ConvertableText, TextElement}

/** An expression that should not be used.
 *
 * This can be used for development purposes but is not expected to be used in practice.
 */
case class MissingExpr() extends NotImplementedExpr {
  override def toText: ConvertableText = TextElement("Missing")
}
