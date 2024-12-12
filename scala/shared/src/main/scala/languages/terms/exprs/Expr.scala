package languages.terms.exprs

import convertors.DisplayMode
import languages.env.{Env, TypeEnv, ValueEnv}
import languages.terms.Term
import languages.terms.errors.{EvalException, TypeException}
import languages.terms.types.Type
import languages.terms.values.Value


/** An unevaluated expression.
 *
 * [[evalInner]] and [[typeCheckInner]] methods need to be implemented. The [[eval]] and [[typeCheck]] methods are
 * provided as wrappers around these methods, which should be used for evaluation and type-checking.
 *
 * The methods for getting children are provided as default implementations, which can be overridden if the
 * expression has a different structure, e.g. if some children have updated environments.
 *
 * The builder for the expression needs to be registered using either the [[ExprCompanion]] trait, or by calling
 * [[addExprBuilder]].
 */
abstract class Expr extends Term {
  def getExprFields: List[Expr] = this match {
    case e0: Product => e0.productIterator.toList.collect({ case e: Expr => e })
    case _           => Nil
  }

  private def defaultChildren[EnvContents](env: Env[EnvContents]): List[(Term, Env[EnvContents])] =
    getExprFields.map(e => (e, env))

  /** Children of this expression in [[DisplayMode.Edit]] mode.
   * @param env
   *   The current environment.
   * @return
   *   A list of pairs of children and their environments.
   */
  override def getChildrenBase(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] = defaultChildren(env)

  /** Children of this expression in [[DisplayMode.Evaluation]] mode.
   * @param env
   *   The current environment.
   * @return
   *   A list of pairs of children and their environments.
   */
  override def getChildrenEval(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] = defaultChildren(env)

  /** Children of this expression in [[DisplayMode.TypeCheck]] mode.
   * @param tEnv
   *   The current type environment.
   * @return
   *   A list of pairs of children and their environments.
   */
  override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = defaultChildren(tEnv)

  /** Perform evaluation using an empty environment. */
  final def eval(): Value = eval(ValueEnv.empty)

  /** Function which evaluates this `Expr` to a `Value`, given an environment.
   *
   * @param env
   *   The environment to evaluate in.
   * @return
   *   The `Value` resulting from evaluating this.
   */
  final def eval(env: ValueEnv): Value = try {
    evalInner(env)
  } catch {
    case e: StackOverflowError => EvalException.stackOverflow
  }

  /** Function to perform evaluation on this `Expr` in the given environment.
   *
   * This function must be implemented by subclasses.
   * @param env
   *   The environment to evaluate in.
   * @return
   *   The `Value` resulting from evaluating this.
   */
  protected def evalInner(env: ValueEnv): Value

  /** Perform type-checking using an empty environment. */
  final def typeCheck(): Type = typeCheck(TypeEnv.empty)

  /** Type-check this `Expr` in the given type environment.
   *
   * @param tEnv
   *   The type environment in which type-checking is done.
   * @return
   *   The `Type` of this expression after type-checking.
   */
  final def typeCheck(tEnv: TypeEnv): Type = try {
    typeCheckInner(tEnv)
  } catch {
    case e: StackOverflowError => TypeException.stackOverflow
  }

  /** Function to perform type checking on this `Expr` in the given type environment.
   *
   * This function must be implemented by subclasses.
   * @param tEnv
   *   The type environment in which type checking is done.
   * @return
   *   The `Type` of this expression after type checking.
   */
  protected def typeCheckInner(tEnv: TypeEnv): Type
}
