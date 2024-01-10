export const PLUS_EMPTY: string =
    `VariableNode("Plus", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))`;
export const PLUS_LEFT_NUM_RIGHT_EMPTY: string =
    `VariableNode("Plus", List(SubExprNode(VariableNode("Num", List(LiteralNode("")))), SubExprNode(ExprChoiceNode())))`;
export const PLUS_LEFT_FILLED_NUM_RIGHT_EMPTY: string =
    `VariableNode("Plus", List(SubExprNode(VariableNode("Num", List(LiteralNode("4")))), SubExprNode(ExprChoiceNode())))`;


export const TIMES_EMPTY: string =
    `VariableNode("Times", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))`;
export const TIMES_LEFT_NUM_RIGHT_EMPTY: string =
    `VariableNode("Times", List(SubExprNode(VariableNode("Num", List(LiteralNode("")))), SubExprNode(ExprChoiceNode())))`;
export const TIMES_LEFT_FILLED_NUM_RIGHT_EMPTY: string =
    `VariableNode("Times", List(SubExprNode(VariableNode("Num", List(LiteralNode("4")))), SubExprNode(ExprChoiceNode())))`;
