package languages

/** Adds the ability to define tasks for the user to solve.
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

  protected final def checkCondition(expr: Expr, cond: Expr => Boolean, env: ValueEnv = ValueEnv.empty): Boolean =
    checkCondition(expr, (e, _) => cond(e), env)

  protected final def checkCondition(expr: Expr, cond: (Expr, ValueEnv) => Boolean, env: ValueEnv): Boolean =
    expr match {
      case e if cond(e, env) => true
      case e =>
        e.getChildrenBase(env)
          .exists({
            case (expr: Expr, newEnv: ValueEnv) => checkCondition(expr, cond, newEnv)
            case _                              => false
          })
    }

  protected def checkHasOp(expr: Expr, op: Class[_ <: Expr]): Boolean = checkCondition(expr, cond = _.getClass == op)
}
