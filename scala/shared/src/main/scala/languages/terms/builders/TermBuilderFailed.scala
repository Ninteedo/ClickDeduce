package languages.terms.builders

import app.ClickDeduceException

private case class TermBuilderFailed(name: String, args: BuilderArgs)
  extends ClickDeduceException(s"Failed to build $name with args $args")

