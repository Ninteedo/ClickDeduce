package actions.exceptions

import app.ClickDeduceException
import nodes.Node

class InvalidSelectTargetException(found: Option[Node]) extends ClickDeduceException(s"Invalid select target: $found")
