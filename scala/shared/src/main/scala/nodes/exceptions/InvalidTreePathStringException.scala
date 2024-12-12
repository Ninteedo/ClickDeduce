package nodes.exceptions

import app.ClickDeduceException

case class InvalidTreePathStringException(s: String) extends ClickDeduceException(s"Invalid tree path string: $s")
