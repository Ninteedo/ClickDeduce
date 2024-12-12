package languages.terms.blanks

import languages.terms.Term
import scalatags.Text.TypedTag
import scalatags.Text.all.*

trait BlankSpace extends Term {
  override lazy val toHtml: TypedTag[String] = {
    input(`type` := "text", placeholder := "Term")
  }
}
