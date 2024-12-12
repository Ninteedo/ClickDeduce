package nodes.exceptions

import app.ClickDeduceException

case class InnerNodeCannotBeRootException() extends ClickDeduceException("Inner node cannot be root")
