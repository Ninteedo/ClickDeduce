package nodes

import convertors.DisplayMode
import languages.env.{Env, TypeEnv}
import languages.terms.types.Type

/** Parent class for nodes that represent a type.
  *
  * Analogous to [[ExprNodeParent]], but for types.
  */
abstract class TypeNodeParent extends OuterNode {

  /** The type represented by this node.
    */
  lazy val getType: Type

  /** The name of the type represented by this node.
    */
  lazy val getTypeName: String = getType.name

  override def getParent: Option[OuterNode] = {
    if (!isParentInitialised) markRoot()
    super.getParent match {
      case Some(n) => Some(n)
      case None    => None
    }
  }

  /** Get the type variable environment for the given mode.
    * @param mode
    *   The display mode.
    * @return
    *   The type variable environment.
    */
  def getEnv(mode: DisplayMode): TypeEnv = getParent match {
    case Some(n: ExprNodeParent)       => TypeEnv.typeVariableEnv(n.getEnv(mode))
    case Some(n: TypeNodeParent) => n.getEnv(mode)
    case _                       => Env()
  }

  /** The result of type-checking this node in the given mode.
    * @param mode
    *   The display mode.
    * @return
    *   The type-checking result.
    */
  def getTypeCheckResult(mode: DisplayMode): Type = getType.typeCheck(getEnv(mode))
}
