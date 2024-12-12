package actions.exceptions

import app.ClickDeduceException
import nodes.Node

class InvalidEditTargetException(found: Option[Node])
    extends ClickDeduceException(s"Invalid literal edit target: $found")
