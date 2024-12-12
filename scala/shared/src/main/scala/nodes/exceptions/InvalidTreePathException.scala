package nodes.exceptions

import app.ClickDeduceException

case class InvalidTreePathException(treePath: List[Int]) extends ClickDeduceException(s"Invalid tree path: $treePath")
