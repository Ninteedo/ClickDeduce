export const PLUS_EMPTY: string =
    `VariableNode("Plus", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))`;
export const PLUS_LEFT_NUM_RIGHT_EMPTY: string =
    `VariableNode("Plus", List(SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(0))))), SubExprNode(ExprChoiceNode())))`;
export const PLUS_LEFT_FILLED_NUM_RIGHT_EMPTY: string =
    `VariableNode("Plus", List(SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(4))))), SubExprNode(ExprChoiceNode())))`;


export const TIMES_EMPTY: string =
    `VariableNode("Times", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))`;
export const TIMES_LEFT_NUM_RIGHT_EMPTY: string =
    `VariableNode("Times", List(SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(0))))), SubExprNode(ExprChoiceNode())))`;
export const TIMES_LEFT_FILLED_NUM_RIGHT_EMPTY: string =
    `VariableNode("Times", List(SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(4))))), SubExprNode(ExprChoiceNode())))`;

export const TABBING_EXAMPLE: string =
    `VariableNode("Times", List(SubExprNode(VariableNode("Times", List(SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(1))))), SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(2)))))))), SubExprNode(VariableNode("Plus", List(SubExprNode(VariableNode("Times", List(SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(3))))), SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(4)))))))), SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(5))))))))))`;

export const PHANTOM_EXAMPLE: string =
    `VariableNode("Apply", List(SubExprNode(VariableNode("Lambda", List(LiteralNode(LiteralIdentifierBind("x")), SubTypeNode(TypeNode("IntType", List())), SubExprNode(VariableNode("Plus", List(SubExprNode(VariableNode("Var", List(LiteralNode(LiteralIdentifierLookup("x"))))), SubExprNode(ExprChoiceNode()))))))), SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(1)))))))`

export const NODE_STRING_PATH_TEST_EXAMPLE: string =
    `VariableNode("Plus", List(SubExprNode(VariableNode("Times", List(SubExprNode(VariableNode("Bool", List(LiteralNode(LiteralBool(true))))), SubExprNode(VariableNode("Num", List(LiteralNode(LiteralInt(0)))))))), SubExprNode(VariableNode("IfThenElse", List(SubExprNode(VariableNode("Bool", List(LiteralNode(LiteralBool(false))))), SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode()))))))`
