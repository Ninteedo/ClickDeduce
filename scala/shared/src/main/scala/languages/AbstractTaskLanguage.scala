package languages

/**
 * Adds the ability to define tasks for the user to solve.
 */
trait AbstractTaskLanguage extends AbstractActionLanguage {
  trait Task {
    val name: String

    val description: String

    val difficulty: Int

    def checkFulfilled(expr: Expr): Boolean

    final def register(): Unit = tasks += (name -> this)
  }

  private var tasks: Map[String, Task] = Map()

  def getTasks: Map[String, Task] = tasks

  protected def clearTasks(): Unit = tasks = Map()

  protected def checkCondition(expr: Expr, cond: Expr => Boolean): Boolean = expr match {
    case e if cond(e) => true
    case e => e.getExprFields.exists(checkCondition(_, cond))
  }

  protected def checkHasOp(expr: Expr, op: Class[_ <: Expr]): Boolean = checkCondition(expr, _.getClass == op)
}
