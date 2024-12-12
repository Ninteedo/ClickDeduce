package languages.terms.builders

import app.ClickDeduceException

private case class UnknownTermBuilder(name: String) extends ClickDeduceException(s"Unknown term builder: $name")
