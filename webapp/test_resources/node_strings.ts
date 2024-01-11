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

export const TABBING_EXAMPLE: string =
    `VariableNode(&quot;Times&quot;, List(SubExprNode(VariableNode(&quot;Times&quot;, List(SubExprNode(VariableNode(&quot;Num&quot;, List(LiteralNode(&quot;1&quot;)))), SubExprNode(VariableNode(&quot;Num&quot;, List(LiteralNode(&quot;2&quot;))))))), SubExprNode(VariableNode(&quot;Plus&quot;, List(SubExprNode(VariableNode(&quot;Times&quot;, List(SubExprNode(VariableNode(&quot;Num&quot;, List(LiteralNode(&quot;3&quot;)))), SubExprNode(VariableNode(&quot;Num&quot;, List(LiteralNode(&quot;4&quot;))))))), SubExprNode(VariableNode(&quot;Num&quot;, List(LiteralNode(&quot;5&quot;)))))))))`;
