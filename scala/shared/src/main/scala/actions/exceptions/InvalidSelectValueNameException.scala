package actions.exceptions

import app.ClickDeduceException

class InvalidSelectValueNameException(valueName: String)
    extends ClickDeduceException(s"Invalid select value name: $valueName")
