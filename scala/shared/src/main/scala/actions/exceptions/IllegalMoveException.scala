package actions.exceptions

import app.ClickDeduceException

case class IllegalMoveException(message: String) extends ClickDeduceException(s"Illegal move: $message")
