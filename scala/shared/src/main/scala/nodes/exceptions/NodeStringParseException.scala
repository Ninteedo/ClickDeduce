package nodes.exceptions

import app.ClickDeduceException

case class NodeStringParseException(nodeString: String)
    extends ClickDeduceException(s"Could not parse node string: $nodeString")
