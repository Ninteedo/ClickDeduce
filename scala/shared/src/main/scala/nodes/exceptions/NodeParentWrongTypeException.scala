package nodes.exceptions

import app.ClickDeduceException

case class NodeParentWrongTypeException(expected: String, actual: String)
  extends ClickDeduceException(s"Node parent has wrong type: expected $expected, got $actual")
