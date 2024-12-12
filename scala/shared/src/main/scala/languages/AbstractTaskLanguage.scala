package languages

import languages.env.{TypeEnv, ValueEnv}
import languages.terms.exprs.Expr
import languages.terms.types.Type

/** Adds the ability to define tasks for the user to solve.
  */
trait AbstractTaskLanguage extends AbstractActionLanguage {
  trait Task {
    val name: String

    val description: String

    val difficulty: Int

    def checkFulfilled(expr: Expr): Boolean
  }

  private var tasks: Map[String, Task] = Map()

  final def getTasks: Map[String, Task] = tasks

  protected final def clearTasks(): Unit = tasks = Map()

  protected final def setTasks(newTasks: Task*): Unit = tasks = newTasks.map(t => (t.name, t)).toMap

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
    
  protected final def checkCondition(typ: Type, cond: (Type, TypeEnv) => Boolean, env: TypeEnv): Boolean =
    typ match {
      case t if cond(t, env) => true
      case t =>
        t.getChildrenTypeCheck(env)
          .exists({
            case (typ: Type, newEnv: TypeEnv) => checkCondition(typ, cond, newEnv)
            case _                             => false
          })
    }

  protected final def checkHasOp(expr: Expr, op: Class[_ <: Expr]): Boolean = checkCondition(expr, cond = _.getClass == op)
}
