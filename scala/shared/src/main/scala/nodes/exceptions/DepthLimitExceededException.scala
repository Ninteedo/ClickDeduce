package nodes.exceptions

import app.ClickDeduceException

case class DepthLimitExceededException(depthLimit: Int) extends ClickDeduceException(s"Depth limit ($depthLimit) exceeded")
