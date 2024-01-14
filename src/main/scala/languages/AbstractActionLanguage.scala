package languages

trait AbstractActionLanguage extends AbstractNodeLanguage {
  def getActionClass(actionName: String): Class[Action] = (actionName match {
    case "SelectExprAction"  => classOf[SelectExprAction]
    case "EditLiteralAction" => classOf[EditLiteralAction]
    case "DeleteAction"      => classOf[DeleteAction]
    case "InsertAction"      => classOf[InsertAction]
    case "PasteAction"       => classOf[PasteAction]
    case "IdentityAction"    => classOf[IdentityAction]
    case "SelectTypeAction"  => classOf[SelectTypeAction]
    case _                   => throw new IllegalArgumentException(s"Unknown action name: $actionName")
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
      case _                  => throw new IllegalArgumentException(s"Could not parse node string: $nodeString")
    }
    val treePath = Node.readPathString(treePathString)
    val actionClass = getActionClass(actionName)
    val constructor = actionClass.getConstructors()(0)
    var remainingExtraArgs = extraArgs
    val arguments = constructor.getParameterTypes.map {
      case c if classOf[AbstractActionLanguage] isAssignableFrom c => this
      case c if classOf[Node] isAssignableFrom c                   => node
      case c if classOf[List[Int]] isAssignableFrom c              => treePath
      case c if classOf[String] isAssignableFrom c => {
        val arg = remainingExtraArgs.head
        remainingExtraArgs = remainingExtraArgs.tail
        arg
      }
      case c => throw new Exception(s"Unexpected parameter type in createAction: $c")
    }
    val result = constructor.newInstance(arguments: _*)
    result.asInstanceOf[Action]
  }

  abstract class Action(val originalTree: OuterNode, val treePath: List[Int]) {
    val newTree: OuterNode
  }

  case class SelectExprAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    exprChoiceName: String
  ) extends Action(originalTree, treePath) {
    override val newTree: OuterNode = originalTree.replace(treePath, VariableNode.createFromExprName(exprChoiceName))
  }

  case class EditLiteralAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    newLiteralText: String
  ) extends Action(originalTree, treePath) {
    override val newTree: OuterNode = originalTree.replace(treePath, LiteralNode(newLiteralText))
  }

  case class SelectTypeAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    typeChoiceName: String
  ) extends Action(originalTree, treePath) {
    override val newTree: OuterNode = originalTree.replace(treePath, TypeNode.fromTypeName(typeChoiceName))
  }

  //  case class CompleteEvaluationAction(override val originalTree: OuterNode, override val treePath: List[Int])
  //    extends Action(originalTree, treePath) {
  //    override val newTree: OuterNode = {
  //      originalTree match {
  //        case ConcreteNode(exprString, args) => {
  //          val expr = readExpr(exprString).get
  //          ???
  //          ConcreteNode(exprString, newArgs)
  //        }
  //      }
  //    }
  //  }

  case class DeleteAction(override val originalTree: OuterNode, override val treePath: List[Int])
      extends Action(originalTree, treePath) {
    override val newTree: OuterNode = originalTree.replace(treePath, ExprChoiceNode())
  }

  case class InsertAction(override val originalTree: OuterNode, override val treePath: List[Int], insertTree: OuterNode)
      extends Action(originalTree, treePath) {
    override val newTree: OuterNode = originalTree.replace(treePath, insertTree)
  }

  case class PasteAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    pasteNodeString: String
  ) extends Action(originalTree, treePath) {
    private val pasteNode: Node = Node.read(pasteNodeString).get

    override val newTree: OuterNode = pasteNode match {
      case n: OuterNode => originalTree.replace(treePath, n)
      case n: InnerNode => originalTree.replace(treePath, n)
    }
  }

  case class IdentityAction(override val originalTree: OuterNode, override val treePath: List[Int])
      extends Action(originalTree, treePath) {
    override val newTree: OuterNode = originalTree
  }
}
