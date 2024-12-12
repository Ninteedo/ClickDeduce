package actions.exceptions

import app.ClickDeduceException
import nodes.Node

class InvalidDeleteTargetException(found: Option[Node]) extends ClickDeduceException(s"Invalid delete target: $found")
