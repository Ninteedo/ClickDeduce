package languages

import actions.*
import actions.exceptions.ActionInvocationException
import nodes.*
import nodes.exceptions.NodeStringParseException

trait AbstractActionLanguage extends AbstractNodeLanguage {
  lang =>

  private def actionArgCount(actionName: String): Int = actionName match {
    case "SelectExprAction"  => 1
    case "SelectTypeAction"  => 1
    case "EditLiteralAction" => 1
    case "DeleteAction"      => 0
    case "MoveAction"        => 1
    case "PasteAction"       => 1
    case "ParseExprAction"   => 1
    case "IdentityAction"    => 0
    case _                   => throw new ActionInvocationException(s"Unknown action name: $actionName")
  }

  private def instantiateAction(
    actionName: String,
    node: OuterNode,
    treePath: List[Int],
    extraArgs: List[String]
  ): Action = {
    if (actionArgCount(actionName) != extraArgs.length) {
      throw new ActionInvocationException(s"Expected ${actionArgCount(actionName)} extra args, got ${extraArgs.length}")
    }

    actionName match {
      case "SelectExprAction"  => SelectExprAction(node, treePath, lang, extraArgs.head)
      case "SelectTypeAction"  => SelectTypeAction(node, treePath, lang, extraArgs.head)
      case "EditLiteralAction" => EditLiteralAction(node, treePath, lang, extraArgs.head)
      case "DeleteAction"      => DeleteAction(node, treePath, lang)
      case "MoveAction"        => MoveAction(node, treePath, lang, Node.readPathString(extraArgs.head))
      case "PasteAction"       => PasteAction(node, treePath, lang, extraArgs.head)
      case "ParseExprAction"   => ParseExprAction(node, treePath, lang, extraArgs.head)
      case "IdentityAction"    => IdentityAction(node, treePath, lang)
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
    val node = Node.read(lang, nodeString) match {
      case Some(n: OuterNode) => n
      case Some(n)            => throw new ActionInvocationException(s"Expected OuterNode, got $n")
      case _                  => throw NodeStringParseException(nodeString)
    }
    val treePath = Node.readPathString(treePathString)
    instantiateAction(actionName, node, treePath, extraArgs)
  }

}
