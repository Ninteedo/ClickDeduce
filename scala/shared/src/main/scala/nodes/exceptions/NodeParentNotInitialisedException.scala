package nodes.exceptions

import app.ClickDeduceException

case class NodeParentNotInitialisedException() extends ClickDeduceException("Node parent not initialised")
