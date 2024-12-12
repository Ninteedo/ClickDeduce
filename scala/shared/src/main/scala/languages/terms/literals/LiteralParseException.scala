package languages.terms.literals

import app.ClickDeduceException

case class LiteralParseException(message: String) extends ClickDeduceException(message)
