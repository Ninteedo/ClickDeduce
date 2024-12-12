package actions.exceptions

import app.ClickDeduceException
import nodes.Node

class InvalidPasteTargetException(found: Option[Node]) extends ClickDeduceException(s"Invalid paste target: $found")
