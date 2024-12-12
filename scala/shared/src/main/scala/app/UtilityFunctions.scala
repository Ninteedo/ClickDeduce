package app

object UtilityFunctions {
  // String escaping
  // https://stackoverflow.com/a/40073137

  def quote(s: String): String = "\"" + escape(s) + "\""
  private def escape(s: String): String = s.flatMap(escapedChar)

  private def escapedChar(ch: Char): String = ch match {
    case '\b' => "\\b"
    case '\t' => "\\t"
    case '\n' => "\\n"
    case '\f' => "\\f"
    case '\r' => "\\r"
    case '"'  => "\\\""
    case '\'' => "\\\'"
    case '\\' => "\\\\"
    case _ =>
      if (ch.isControl) "\\0" + Integer.toOctalString(ch.toInt)
      else String.valueOf(ch)
  }

  def unquote(s: String): String = if (s.length > 1 && s.startsWith("\"") && s.endsWith("\"")) {
    unescape(s.substring(1, s.length - 1))
  } else {
    throw new Exception("Invalid string literal")
  }

  private def unescape(s: String): String = {
    val res = new StringBuilder
    var i = 0
    var escaped = false

    s.foreach(c => {
      if (escaped) {
        c match {
          case 'b'  => res += '\b'
          case 't'  => res += '\t'
          case 'n'  => res += '\n'
          case 'f'  => res += '\f'
          case 'r'  => res += '\r'
          case '"'  => res += '"'
          case '\'' => res += '\''
          case '\\' => res += '\\'
          case _    => throw new Exception("Invalid escape sequence")
        }
        escaped = false
      } else {
        if (c == '\\') {
          escaped = true
        } else {
          res += c
        }
      }
    })

    if (escaped) {
      throw new Exception("Invalid escape sequence")
    }

    res.toString
  }


  def cacheQuery[A, B](cache: collection.mutable.Map[A, B], key: A, value: => B): B = cache.get(key) match {
    case Some(value) => value
    case None =>
      val result = value
      cache += (key -> result)
      result
  }
}
