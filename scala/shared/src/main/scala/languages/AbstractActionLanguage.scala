package languages

import app.ClickDeduceException

trait AbstractActionLanguage extends AbstractNodeLanguage {
  private def actionArgCount(actionName: String): Int = actionName match {
    case "SelectExprAction"  => 1
    case "SelectTypeAction"  => 1
    case "EditLiteralAction" => 1
    case "DeleteAction"      => 0
    case "PasteAction"       => 1
    case "IdentityAction"    => 0
    case _                   => throw new ActionInvocationException(s"Unknown action name: $actionName")
  }

  private def instantiateAction(
    actionName: String,
    node: OuterNode,
    treePath: List[Int],
    extraArgs: List[String]
  ): Action = {
    if (actionArgCount(actionName) != extraArgs.length)
      throw new ActionInvocationException(s"Expected ${actionArgCount(actionName)} extra args, got ${extraArgs.length}")

    actionName match {
      case "SelectExprAction"  => SelectExprAction(node, treePath, extraArgs.head)
      case "SelectTypeAction"  => SelectTypeAction(node, treePath, extraArgs.head)
      case "EditLiteralAction" => EditLiteralAction(node, treePath, extraArgs.head)
      case "DeleteAction"      => DeleteAction(node, treePath)
      case "PasteAction"       => PasteAction(node, treePath, extraArgs.head)
      case "IdentityAction"    => IdentityAction(node, treePath)
      case _                   => throw new ActionInvocationException(s"Unknown action name: $actionName")
    }
  }

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
    instantiateAction(actionName, node, treePath, extraArgs)
  }

  abstract class Action(val originalTree: OuterNode, val treePath: List[Int]) {
    lazy val newTree: OuterNode
  }

  case class SelectExprAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    exprChoiceName: String
  ) extends Action(originalTree, treePath) {
    override lazy val newTree: OuterNode = {
      if (getExprBuilder(exprChoiceName).isEmpty) throw new InvalidSelectValueNameException(exprChoiceName)

      val exprNode = VariableNode.createFromExprName(exprChoiceName)
      if (exprNode.isEmpty) throw new InvalidSelectValueNameException(exprChoiceName)
      originalTree.findChild(treePath) match {
        case Some(exprChoiceNode: ExprChoiceNode) =>
          originalTree.replace(treePath, exprNode.get)
        case other => throw new InvalidSelectTargetException(other)
      }
    }
  }

  case class SelectTypeAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    typeChoiceName: String
  ) extends Action(originalTree, treePath) {
    override lazy val newTree: OuterNode = {
      val typeNode = TypeNode.fromTypeName(typeChoiceName)
      if (typeNode.isEmpty) throw new InvalidSelectValueNameException(typeChoiceName)
      originalTree.findChild(treePath) match {
        case Some(typeChoiceNode: TypeChoiceNode) =>
          originalTree.replace(treePath, typeNode.get)
        case other => throw new InvalidSelectTargetException(other)
      }
    }
  }

  case class EditLiteralAction(
    override val originalTree: OuterNode,
    override val treePath: List[Int],
    newLiteralText: String
  ) extends Action(originalTree, treePath) {
    override lazy val newTree: OuterNode = originalTree.findChild(treePath) match {
      case Some(literalNode: LiteralNode) => originalTree.replace(treePath, LiteralNode(Literal.fromStringOfType(newLiteralText, literalNode.literal.getClass)))
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

  class ActionInvocationException(message: String) extends ClickDeduceException(message)

  class InvalidSelectTargetException(found: Option[Node]) extends ClickDeduceException(s"Invalid select target: $found")

  class InvalidSelectValueNameException(valueName: String)
      extends ClickDeduceException(s"Invalid select value name: $valueName")

  class InvalidEditTargetException(found: Option[Node])
      extends ClickDeduceException(s"Invalid literal edit target: $found")

  class InvalidDeleteTargetException(found: Option[Node]) extends ClickDeduceException(s"Invalid delete target: $found")

  class InvalidPasteTargetException(found: Option[Node]) extends ClickDeduceException(s"Invalid paste target: $found")
}
