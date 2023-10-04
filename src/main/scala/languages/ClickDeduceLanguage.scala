package languages

/**
 * Parent trait for all languages designed to be loaded in ClickDeduce.
 */
trait ClickDeduceLanguage {
  /**
   * A variable name.
   *
   * Case sensitive.
   */
  type Variable = String

  /**
   * The evaluation environment at a particular point.
   *
   * Contains variables with bound values.
   */
  type Env = Map[Variable, Value]

  /**
   * The type environment at a particular point.
   *
   * Contains variables with bound types.
   */
  type TypeEnv = Map[Variable, Type]

  /**
   * An unevaluated expression.
   */
  abstract class Expr

  /**
   * A value resulting from an expression being evaluated.
   */
  abstract class Value

  /**
   * An error resulting from an expression being evaluated.
   */
  abstract class EvalError extends Value

  /**
   * The type of a value.
   */
  abstract class Type

  /**
   * An error resulting from an expression being type checked.
   */
  abstract class TypeError extends Type

  /**
   * Function which evaluates an `Expr` to a `Value`, given an environment.
   *
   * @param e   The `Expr` to evaluate.
   * @param env The environment to evaluate the `Expr` in.
   * @return The `Value` resulting from evaluating the `Expr`.
   */
  def eval(e: Expr, env: Env): Value

  /**
   * Function which evaluates an `Expr` to a `Value`, given an empty environment.
   *
   * Equivalent to calling <code>eval(e, Map()).</code>
   *
   * @param e The `Expr` to evaluate.
   * @return The `Value` resulting from evaluating the `Expr`.
   */
  def eval(e: Expr): Value = {
    eval(e, Map())
  }

  /**
   * Function to perform type checking on an `Expr` in the given type environment.
   *
   * @param e    The `Expr` on which type checking needs to be performed.
   * @param tenv The type environment in which type checking is done.
   * @return The `Type` of the expression after type checking.
   */
  def typeOf(e: Expr, tenv: TypeEnv): Type

  /**
   * Overloaded type checking function that performs type checking on an `Expr` in an empty type environment.
   *
   * Equivalent to calling <code>typeCheck(e, Map()).</code>
   *
   * @param e The `Expr` on which type checking needs to be performed.
   * @return The `Type` of the expression after type checking.
   */
  def typeOf(e: Expr): Type = {
    typeOf(e, Map())
  }

  /**
   * Function to get the children of an `Expr` in the given environment.
   *
   * @param e   The `Expr` whose children are to be returned.
   * @param env The environment in which the `Expr` is to be evaluated.
   * @return The `Expr`s which are the children of `e`.
   */
  def childrenOf(e: Expr, env: Env): List[Expr]

  /**
   * Overloaded function to get the children of an `Expr` in an empty environment.
   *
   * @param e The `Expr` whose children are to be returned.
   * @return The `Expr`s which are the children of `e`.
   */
  def childrenOf(e: Expr): List[Expr] = {
    childrenOf(e, Map())
  }

  /**
   * Function to create a human-readable string representation of an `Expr`.
   *
   * @param e The `Expr` to be pretty printed.
   * @return A `String` representing the pretty printed expression.
   */
  def prettyPrint(e: Expr): String

  /**
   * Function to create a human-readable string representation of a `Type`.
   *
   * @param t The `Type` to be pretty printed.
   * @return A `String` representing the pretty printed type.
   */
  def prettyPrint(t: Type): String

  /**
   * Function to create a human-readable string representation of a `Value`.
   *
   * @param v The `Value` to be pretty printed.
   * @return A `String` representing the pretty printed value.
   */
  def prettyPrint(v: Value): String

  /**
   * Function to load an `Expr` from a string.
   * Input must be in the format produced by `Expr.toString`
   * @param s The string to be parsed.
   * @return The `Expr` represented by the string, if successful.
   */
  def readExpr(s: String): Option[Expr] = {
    var parseIndex = 0

    /**
     * Create an `Expr` given its name and a list of arguments.
     * @param name The name of the `Expr` to be created. Must match the name of a class extending `Expr` in the language.
     * @param args The arguments to be passed to the constructor of the `Expr`.
     * @return The `Expr` created, if successful.
     */
    def makeExpr(name: String, args: List[Any]): Option[Expr] = {
      val exprClass = getClass.getClasses.find(c => classOf[Expr].isAssignableFrom(c) && c.getSimpleName == name)
      exprClass match
        case Some(value) => {
          val constructor = value.getConstructors()(0)
          val arguments = List(LArith) ++ args
          Some(constructor.newInstance(arguments: _*).asInstanceOf[Expr])
        }
        case None => None
    }

    /**
     * Read the name of an `Expr` from the string.
     * @return The name of the `Expr` read.
     */
    def readExprName(): String = {
      val exprName = new StringBuilder()
      while (s(parseIndex) != '(') {
        exprName += s(parseIndex)
        parseIndex += 1
      }
      parseIndex += 1
      exprName.toString
    }

    /**
     * Read the next `Expr` from the string.
     * If an argument which is a sub-expression fails to be read, the whole expression fails to be read.
     * @return The `Expr` read, if successful.
     */
    def readSubExpr(): Option[Expr] = {
      val name = readExprName()
      val children = new scala.collection.mutable.ListBuffer[Any]()
      while (s(parseIndex) != ')') {
        if (s(parseIndex) == ',' || s(parseIndex) == ' ') {
          parseIndex += 1
        }
        readArgument() match
          case Some(value) => children += value
          case None => return None
      }
      parseIndex += 1
      makeExpr(name, children.toList)
    }

    /**
     * Read the next argument from the string.
     * Can be either a literal or another `Expr`.
     * @return The argument read, if successful.
     */
    def readArgument(): Option[Any] = {
      if (s(parseIndex) == '\"') {
        Some(readStringLiteral())
      } else if (s(parseIndex).isDigit) {
        Some(readNumberLiteral())
      } else if (checkArgumentMatchLiteral("true")) {
        Some(true)
      } else if (checkArgumentMatchLiteral("false")) {
        Some(false)
      } else {
        readSubExpr()
      }
    }

    /**
     * Read a string literal from the string.
     * Respects escape characters.
     * @return The string literal read.
     */
    def readStringLiteral(): String = {
      parseIndex += 1
      val literal = new StringBuilder()
      var escaped: Boolean = false
      while (s(parseIndex) != '\"' || escaped) {
        if (s(parseIndex) == '\\' && !escaped) {
          escaped = true
        } else {
          escaped = false
          literal += s(parseIndex)
        }
        parseIndex += 1
      }
      parseIndex += 1
      literal.toString
    }

    /**
     * Read a number literal from the string.
     * @return The number literal read.
     */
    def readNumberLiteral(): BigInt = {
      val literal = new StringBuilder()
      while (s(parseIndex).isDigit) {
        literal += s(parseIndex)
        parseIndex += 1
      }
      BigInt(literal.toString)
    }

    /**
     * Check if the next argument exactly matches a given literal.
     * If it does, advance the parse index past the literal.
     * Used for parsing booleans.
     * @param literal The literal to check against.
     * @return True if the next argument matches the literal, false otherwise.
     */
    def checkArgumentMatchLiteral(literal: String): Boolean = {
      if (s.length >= parseIndex + literal.length && s.startsWith(literal, parseIndex)) {
        val result = s(parseIndex + literal.length) != '('
        if (result) {
          parseIndex += literal.length
        }
        result
      } else {
        false
      }
    }

    readSubExpr()
  }
}
