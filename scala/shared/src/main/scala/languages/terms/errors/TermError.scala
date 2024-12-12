package languages.terms.errors

import languages.terms.Term

trait TermError extends Term {
  val message: String = "Error"
}
