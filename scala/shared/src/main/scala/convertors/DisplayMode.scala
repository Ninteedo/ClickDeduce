package convertors

enum DisplayMode:
  case Edit, Evaluation, TypeCheck

object DisplayMode {
  def fromString(s: String): DisplayMode = s match {
    case "edit" => Edit
    case "eval" => Evaluation
    case "type-check" => TypeCheck
    case _ => throw new IllegalArgumentException(s"Unknown display mode: $s")
  }
}
