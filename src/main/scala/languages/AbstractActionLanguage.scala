package languages

trait AbstractActionLanguage extends AbstractNodeLanguage {
  private def getActionClass(actionName: String): Class[Action] = (actionName match {
    case "SelectExprAction"  => classOf[SelectExprAction]
    case "SelectTypeAction"  => classOf[SelectTypeAction]
    case "EditLiteralAction" => classOf[EditLiteralAction]
    case "DeleteAction"      => classOf[DeleteAction]
    case "PasteAction"       => classOf[PasteAction]
    case "IdentityAction"    => classOf[IdentityAction]
    case _                   => throw new ActionInvocationException(s"Unknown action name: $actionName")
  }).asInstanceOf[Class[Action]]

  def createAction(
    actionName: String,
    nodeString: String,
    treePathString: String,
    extraArgs: List[String],
    modeName: String = "edit"
  ): Action = {
    val node = Node.read(nodeString) match {
      case Some(n: OuterNode) => n
      case Some(n)            => throw new ActionInvocationException(s"Expected OuterNode, got $n")
      case _                  => throw new NodeStringParseException(nodeString)
    }
    val treePath = Node.readPathString(treePathString)
    val actionClass = getActionClass(actionName)
    val constructor = actionClass.getConstructors.headOption match {
      case Some(c) => c
      case None    => throw new ActionInvocationException(s"No constructor found for $actionClass")
    }
    var remainingExtraArgs = extraArgs
    val arguments = constructor.getParameterTypes.map {
      case c if classOf[AbstractActionLanguage] isAssignableFrom c => this
      case c if classOf[Node] isAssignableFrom c                   => node
      case c if classOf[List[Int]] isAssignableFrom c              => treePath
      case c if classOf[String] isAssignableFrom c =>
        if (remainingExtraArgs.isEmpty) {
          throw new ActionInvocationException(s"Missing parameter for $actionClass")
        }
        val arg = remainingExtraArgs.head
        remainingExtraArgs = remainingExtraArgs.tail
        arg
      case c => throw new ActionInvocationException(s"Unexpected parameter type in createAction: $c")
    }
    if (remainingExtraArgs.nonEmpty) {
      throw new ActionInvocationException(s"Too many parameters for $actionClass")
    }
    try {
      val result = constructor.newInstance(arguments: _*)
      result.asInstanceOf[Action]
    } catch {
      case e: Exception => throw new ActionInvocationException(s"Error invoking constructor for $actionClass: $e")
    }
  }

  abstract class Action(val originalTree: OuterNode, val treePath: List[Int]) {
    lazy val newTree: OuterNode
  }

  case class SelectExprAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    exprChoiceName: String
  ) extends Action(originalTree, treePath) {
    override lazy val newTree: OuterNode = originalTree.findChild(treePath) match {
      case Some(exprChoiceNode: ExprChoiceNode) =>
        originalTree.replace(treePath, VariableNode.createFromExprName(exprChoiceName))
      case other => throw new InvalidSelectTargetException(other)
    }
  }

  case class SelectTypeAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    typeChoiceName: String
  ) extends Action(originalTree, treePath) {
    override lazy val newTree: OuterNode = originalTree.findChild(treePath) match {
      case Some(typeChoiceNode: TypeChoiceNode) =>
        originalTree.replace(treePath, TypeNode.fromTypeName(typeChoiceName))
      case other => throw new InvalidSelectTargetException(other)
    }
  }

  case class EditLiteralAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    newLiteralText: String
  ) extends Action(originalTree, treePath) {
    override lazy val newTree: OuterNode = originalTree.findChild(treePath) match {
      case Some(literalNode: LiteralNode) => originalTree.replace(treePath, LiteralNode(newLiteralText))
      case other                          => throw new InvalidEditTargetException(other)
    }
  }

  case class DeleteAction(override val originalTree: OuterNode, override val treePath: List[Int])
      extends Action(originalTree, treePath) {
    override lazy val newTree: OuterNode = originalTree.findChild(treePath) match {
      case Some(_: ExprNode)       => originalTree.replace(treePath, ExprChoiceNode())
      case Some(_: TypeNodeParent) => originalTree.replace(treePath, TypeChoiceNode())
      case other                   => throw new InvalidDeleteTargetException(other)
    }
  }

  case class PasteAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    pasteNodeString: String
  ) extends Action(originalTree, treePath) {
    private val pasteNode: Node = Node.read(pasteNodeString).get

    override lazy val newTree: OuterNode = originalTree.findChild(treePath) match {
      case Some(_: ExprNode) =>
        pasteNode match {
          case _: ExprNode => originalTree.replace(treePath, pasteNode)
          case _           => throw new InvalidPasteTargetException(Some(pasteNode))
        }
      case Some(_: TypeNodeParent) =>
        pasteNode match {
          case _: TypeNodeParent => originalTree.replace(treePath, pasteNode)
          case _                 => throw new InvalidPasteTargetException(Some(pasteNode))
        }
      case other => throw new InvalidPasteTargetException(other)
    }
  }

  case class IdentityAction(override val originalTree: OuterNode, override val treePath: List[Int])
      extends Action(originalTree, treePath) {
    override lazy val newTree: OuterNode = originalTree
  }

  class ActionInvocationException(message: String) extends Exception(message)

  class InvalidSelectTargetException(found: Option[Node]) extends Exception(s"Invalid select target: $found")

  class InvalidEditTargetException(found: Option[Node]) extends Exception(s"Invalid literal edit target: $found")

  class InvalidDeleteTargetException(found: Option[Node]) extends Exception(s"Invalid delete target: $found")

  class InvalidPasteTargetException(found: Option[Node]) extends Exception(s"Invalid paste target: $found")
}
