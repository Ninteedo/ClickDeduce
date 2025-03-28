package nodes

import app.UtilityFunctions.cacheQuery
import convertors.DisplayMode
import languages.AbstractNodeLanguage
import languages.env.{Env, TypeEnv, ValueEnv}
import languages.terms.Term
import languages.terms.errors.StackOverflowErrorValue
import languages.terms.exprs.Expr
import languages.terms.types.Type
import languages.terms.values.Value
import nodes.exceptions.{DepthLimitExceededException, NodeParentWrongTypeException}

/** Parent class for nodes that represent an expression.
  *
  * Has a depth limit to prevent infinite recursion.
  */
abstract class ExprNodeParent(lang: AbstractNodeLanguage) extends OuterNode {
  private var editValueResultCache: Option[Value] = None
  def getEditValueResult: Value = {
    if (editValueResultCache.isEmpty) {
      editValueResultCache = Some(if (willDepthLimitBeExceeded()) StackOverflowErrorValue() else getExpr.eval(getEditEnv))
    }
    editValueResultCache.get
  }
  private var valueCache: Option[Value] = None
  /** Evaluation result of the expression represented by this node.
   */
  def getValue: Value = {
    if (valueCache.isEmpty) {
      valueCache = Some(getExpr.eval(getEvalEnv))
    }
    valueCache.get
  }
  private var typeCache: Option[Type] = None
  /** Type-checking result of the expression represented by this node.
   */
  def getType: Type = {
    if (typeCache.isEmpty) {
      typeCache = Some(getExpr.typeCheck(getTypeEnv))
    }
    typeCache.get
  }
  private var editEnvCache: Option[ValueEnv] = None
  def getEditEnv: ValueEnv = {
    if (editEnvCache.isEmpty) {
      editEnvCache = Some(editEnvOverride.getOrElse(getCorrectEnv(_.getChildrenBase, _.getEditEnv)))
    }
    editEnvCache.get
  }
  private var evalEnvCache: Option[ValueEnv] = None
  def getEvalEnv: ValueEnv = {
    if (evalEnvCache.isEmpty) {
      evalEnvCache = Some(evalEnvOverride.getOrElse(getCorrectEnv(_.getChildrenEval, _.getEvalEnv)))
    }
    evalEnvCache.get
  }
  private var typeEnvCache: Option[TypeEnv] = None
  def getTypeEnv: TypeEnv = {
    if (typeEnvCache.isEmpty) {
      typeEnvCache = Some(typeEnvOverride.getOrElse(getCorrectEnv(_.getChildrenTypeCheck, _.getTypeEnv)))
    }
    typeEnvCache.get
  }
  /** The name of the expression represented by this node.
   */
  val exprName: String
  private val visibleChildrenCache = collection.mutable.Map[DisplayMode, List[OuterNode]]()
  private var isPhantomStore = false
  private var phantomDepth: Option[Int] = None

  private val depthLimit: Int = 100

  def depth: Int = getPhantomDepth

  /** Check if the depth limit will be exceeded by evaluating this node, throwing an exception if it will.
   *
   * Similar to [[willDepthLimitBeExceeded]], but throws an exception if the depth limit will be exceeded.
   *
   * @param currDepth
   * The current depth, default 0.
   * @throws DepthLimitExceededException
   * if the depth limit will be exceeded.
   */
  def checkDepthLimitWillBeExceeded(currDepth: Int = 0): Unit = {
    if (currDepth + 1 >= depthLimit) throw DepthLimitExceededException(depthLimit)

    getVisibleChildren(DisplayMode.Evaluation).reverse.foreach({
      case n: ExprNodeParent => n.checkDepthLimitWillBeExceeded(currDepth + 1)
      case _ =>
    })
  }

  /** Whether the depth limit will be exceeded by evaluating this node.
   *
   * @param currDepth
   * The current depth, default 0.
   * @return
   * Whether the depth limit will be exceeded.
   */
  def willDepthLimitBeExceeded(currDepth: Int = 0): Boolean = {
    (currDepth + 1 >= depthLimit) || getVisibleChildren(DisplayMode.Evaluation).reverse.exists({
      case n: ExprNodeParent => n.willDepthLimitBeExceeded(currDepth + 1)
      case _ => false
    }
    )
  }

  /** The expression represented by this node.
   *
   * @return
   * Represented expression.
   */
  def getExpr: Expr

  private var hasUpdatedEnvCache: Map[DisplayMode, Boolean] = Map()

  def hasUpdatedEnv(mode: DisplayMode): Boolean = {
    if (!hasUpdatedEnvCache.contains(mode)) {
      val env = getEnv(mode)
      val parentEnv = getParent.map(_.getEnv(mode))
      hasUpdatedEnvCache += mode -> (!parentEnv.contains(env) || (parentEnv.isEmpty && env.nonEmpty))
    }
    hasUpdatedEnvCache(mode)
  }

  override def getParent: Option[ExprNodeParent] = {
    if (!isParentInitialised) markRoot()
    super.getParent match {
      case Some(n: ExprNodeParent) => Some(n)
      case None => None
      case Some(n) => throw NodeParentWrongTypeException("ExprNode", n.name)
    }
  }

  override def setParent(parentNode: Option[OuterNode]): Unit = parentNode match {
    case Some(n: ExprNodeParent) =>
      if (n.depth >= depthLimit) throw DepthLimitExceededException(depthLimit)
      super.setParent(Some(n))
    case None => super.setParent(None)
    case Some(n) => throw NodeParentWrongTypeException("ExprNode", n.name)
  }

  /** Get the environment for the given mode.
   *
   * @param mode
   * The display mode.
   * @return
   * The environment for the given mode, either a [[ValueEnv]] or a [[TypeEnv]].
   */
  def getEnv(mode: DisplayMode): ValueEnv | TypeEnv = mode match {
    case DisplayMode.Edit       => getEditEnv
    case DisplayMode.TypeCheck  => getTypeEnv
    case DisplayMode.Evaluation => getEvalEnv
  }

  override def getVisibleChildren(mode: DisplayMode): List[OuterNode] = cacheQuery(
    visibleChildrenCache,
    mode,
    mode match {
      case DisplayMode.Edit => children
      case DisplayMode.TypeCheck => children
      case DisplayMode.Evaluation => visibleEvaluationChildren
    }
  )

  override def isPhantom: Boolean = isPhantomStore

  private var editEnvOverride: Option[ValueEnv] = None
  private var evalEnvOverride: Option[ValueEnv] = None
  private var typeEnvOverride: Option[TypeEnv] = None

  def overrideEnv(env: ValueEnv | TypeEnv, mode: DisplayMode): Unit = {
    resetCaches()
    mode match {
      case DisplayMode.Edit => editEnvOverride = Some(env.asInstanceOf[ValueEnv])
      case DisplayMode.Evaluation => evalEnvOverride = Some(env.asInstanceOf[ValueEnv])
      case DisplayMode.TypeCheck => typeEnvOverride = Some(env.asInstanceOf[TypeEnv])
    }
  }

  private def getCorrectEnv[T](
    childrenFunction: Expr => Env[T] => List[(Term, Env[T])],
    parentEnvFunction: ExprNodeParent => Env[T]
  ): Env[T] = getParent match {
    case Some(parent) =>
      val parentEnv = parentEnvFunction(parent)
      val parentExpr = parent.getExpr
      val myExpr = getExpr
      val parentChildren = childrenFunction(parentExpr)(parentEnv)
      parentChildren.find(_._1 eq myExpr).map(_._2).getOrElse(parentEnv)
    case None => Env()
  }

  private def visibleEvaluationChildren: List[OuterNode] = {
    val childExprList = getExpr.getChildrenEval(getEvalEnv).map(_._1)
    var unconsumedChildren = children

    childExprList.flatMap({ case expr: Expr =>
      val matchingChild = unconsumedChildren.collectFirst {
        case c: ExprNodeParent if c.getExpr eq expr                 => c
        case c: ExprChoiceNode if c.getExpr == expr && !c.isPhantom => c
      }

      matchingChild match {
        case Some(childNode) =>
          unconsumedChildren = unconsumedChildren.filter(_ ne childNode)
          Some(childNode)
        case None =>
          val newNode = ExprNode.fromExpr(lang, expr)
          newNode.setParent(Some(this))
          newNode.markPhantom(getPhantomDepth + 1)
          Some(newNode)
      }
    })
  }

  private def markPhantom(depth: Int): Unit = {
    isPhantomStore = true
    phantomDepth = Some(depth)

    if (depth >= depthLimit) throw DepthLimitExceededException(depthLimit)
  }

  private def getPhantomDepth: Int = phantomDepth.getOrElse(0)

  private def resetCaches(): Unit = {
    editValueResultCache = None
    valueCache = None
    typeCache = None
    editEnvCache = None
    evalEnvCache = None
    typeEnvCache = None
    hasUpdatedEnvCache = Map()
    visibleChildrenCache.clear()
  }
}
