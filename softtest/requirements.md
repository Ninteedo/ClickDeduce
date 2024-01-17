# Requirements

## Interface

1. The system will allow the user to intuitively view an evaluation tree
   1. The user will be able to click and drag the tree to pan around it
   2. The user will be able to scroll to zoom in and out
   3. The subtree that the user is hovering over will be highlighted
   4. The evaluation tree will be displayed as...
2. The system will allow the user to construct an evaluation tree from scratch
   1. The user will be able to create a new evaluation tree with only a root node
   2. The user will be able to select an evaluation rule available in the language
      1. Each empty node will have a dropdown menu of available evaluation rules
   3. Evaluation rules can have any number of subexpressions
      1. Evaluation rules without any subexpressions are axioms, and the end of the subtree
      2. Each subexpression has its own subtree
   4. The user will be able to select a subexpression of the current node to be the next node
   5. The user will be able to enter text where literals occur
      1. While literals may have specific syntax rules, the user will be able to enter any text
         1. Invalid literals will result in an evaluation error being displayed in the tree value
3. The system will allow the user to edit an existing evaluation tree
   1. The user will be able to right-click on a node to open a context menu, focused on that node
      1. The highlighted subtree will be the one focused on when the context menu is opened
      2. The focused subtree will remain highlighted as long as the context menu is open
      3. Operations from the context menu will target the focused subtree
   2. The user will be able to delete a node, resetting that subtree to an empty state
   3. The user will be able to copy a node, and paste it somewhere else in the tree
      1. It is only possible to paste a subtree after copying a subtree
      2. Any subtree can be copied, including the root node
   4. The user will be able to reset the entire tree to an empty state
4. The user will be able to use one of multiple evaluation languages
   1. The user will be able to select a language from a dropdown menu
      1. The server will provide a list of available languages
      2. The server will provide a list of available evaluation rules for each language
   2. The user can switch which language is being used at any time, as long as it is valid
      1. The server will validate the tree when the user switches languages
         1. The server will check that the tree does not contain any evaluation rules or expressions which do not exist in the new language
      2. The user will then be able to use evaluation rules from the new language
      3. The language will be switched whenever the user changes the language dropdown value

# Languages

The following languages, following the grammars defined in the example languages of the Elements of Programming Languages course, will be implemented:

## Arithmetic (LArith)

1. The language will support the following expressions:
   1. `Num(x)`, where `x` is a literal
   2. `Plus(x, y)`, where `x` and `y` are expressions
   3. `Times(x, y)`, where `x` and `y` are expressions
2. The language will have the following types:
   1. `Int`
3. The language will have the following values:
   1. `NumV(x)`, where `x` is a number
4. The language will have the following evaluation rules:
   1. `Num(x) ⇓ NumV(x)`
      1. The literal `x` must only contain digits and optionally a minus sign at the beginning
   2. `Plus(NumV(x), NumV(y)) ⇓ NumV(x + y)`
   3. `Times(NumV(x), NumV(y)) ⇓ NumV(x * y)`
5. The language will have the following type-checking rules:
   1. `Num(x) : Int`
      1. The literal `x` must only contain digits and optionally a minus sign at the beginning 
   2. `Plus(x: Int, y: Int) : Int`
   3. `Times(x: Int, y: Int) : Int`
6. The following scenarios will result in an error:
   1. The literal `x` in `Num(x)` contains any characters other than digits and optionally a minus sign at the beginning
   2. The type of the `x` and `y` values in `Plus(x, y)` are not `Int`
   3. The type of the `x` and `y` values in `Times(x, y)` are not `Int`
   4. Any attempt at evaluation or type-checking that does not match a defined rule

## Boolean (LIf)

1. The following requirements are in addition to the requirements for the Arithmetic language
2. The language will support the following expressions:
   1. `Bool(x)`, where `x` is a literal
   2. `Eq(x, y)`, where `x` and `y` are expressions
   3. `IfThenElse(cond, then, else)`, where `cond`, `then`, and `else` are expressions
3. The language will have the following types:
   1. `Bool`
4. The language will have the following values:
   1. `BoolV(x)`, where `x` is either `true` or `false`
5. The language will have the following evaluation rules:
   1. `Bool(x) ⇓ BoolV(x)`
      1. The literal `x` must exactly match either `true` or `false`
   2. `Eq(x, y) ⇓ BoolV(x == y)`
   3. `IfThenElse(BoolV(true), then, else) -> then`
   4. `IfThenElse(BoolV(false), then, else) -> else`
6. The language will have the following type-checking rules:
   1. `Bool(x) : Bool`
      1. The literal `x` must exactly match either `true` or `false`
   2. `Eq(x: T, y: T) : Bool`, where `T` is any type
      1. The type of the `x` and `y` values must be the same 
   3. `IfThenElse(cond: Bool, then: T, else: T) : T`, where `T` is any type
      1. The type of the `then` and `else` values must be the same
7. The following scenarios will result in an error:
   1. The literal `x` in `Bool(x)` does not exactly match either `true` or `false`
   2. The type of the `x` and `y` values in `Eq(x, y)` are not the same
   3. The type of the `then` and `else` values are not the same
   4. The type of the `cond` value is not `Bool`

## Variable (LLet)

1. The following requirements are in addition to the requirements for the Boolean language
2. The language will support the following expressions:
   1. `Var(v)`, where `v` is a literal
   2. `Let(v, x, y)`, where `v` is a literal, `x` is an expression, and `y` is an expression
3. The language will not add any new types
4. The language will not add any new values
5. The language will use an environment when evaluating or type-checking expressions
   1. The environment is a mapping between variable names and values/types
   2. In the evaluation/type-checking rules, the environment will be represented as `σ`
   3. Looking up a variable in the environment will be represented as `σ(v)`
   4. Binding a variable to a value/type in the environment will be represented as `σ[v -> x]`
6. Valid identifiers will:
   1. Start with a letter, `$`, or `_`
   2. Only contain letters, digits, `$`, or `_`
   3. Have a length of at least 1
7. The language will have the following evaluation rules:
   1. `σ, Var(v) ⇓ σ(v)`
      1. The literal `v` must be a valid identifier 
   2. `σ, Let(v, x, y) ⇓ σ[v -> x], y`
      1. The literal `v` must be a valid identifier 
      2. The bound value of `x` is the result of evaluating `x` in the environment `σ`
8. The language will have the following type-checking rules:
   1. `σ, Var(v) : σ(v)`
      1. The literal `v` must be a valid identifier 
   2. `σ, Let(v, x, y) : σ[v -> x], y`
      1. The literal `v` must be a valid identifier
      2. The bound type of `x` is the result of type-checking `x` in the environment `σ`
9. The following scenarios will result in an error:
   1. The variable being looked up does not exist in the environment
   2. A variable identifier is invalid

## Lambda Functions (LLam)

1. The following requirements are in addition to the requirements for the Variable language
2. The language will support the following expressions:
   1. `Lambda(v, t, x)`, where `v` is a literal, `t` is a type, and `x` is an expression
   2. `Apply(x, y)`, where `x` and `y` are expressions
3. The language will support the following types:
   1. `Func(t1, t2)`, where `t1` and `t2` are types
4. The language will have the following values:
   1. `LambdaV(v, t, x, env)`, where `v` is a literal, `t` is a type, `x` is an expression, and `env` is an environment
5. The language will have the following evaluation rules:
   1. `σ, Lambda(v, t, x) ⇓ LambdaV(v, t, x, σ)`
      1. The literal `v` must be a valid identifier
   2. `σ, Apply(LambdaV(v, t, x, env), y) ⇓ (env[v -> y], x)`
      1. The literal `v` must be a valid identifier
      2. The type of `y` must be `t`
6. The language will have the following type-checking rules:
   1. `σ, Lambda(v, t, x) : Func(t, t2)`
      1. The literal `v` must be a valid identifier
      2. `t2` is the type of `σ[v -> t], x`
   2. `σ, Apply(x: Func(t1, t2), y: t1) : t2`
7. The following scenarios will result in an error:
   1. The literal `v` in `Lambda(v, t, x)` is not a valid identifier
   2. When evaluating `Apply(LambdaV(v, t, x, env), y)`, the type of `y` is not equal to `t`

## Recursive Functions (LRec)

1. The following requirements are in addition to the requirements for the Lambda Functions language
2. The language will support the following expressions:
   1. `Rec(f, v, t, x)`, where `f` and `v` are literals, `t` is a type, and `x` is an expression
3. The language will have the following values:
   1. `RecV(f, v, t, x, env)`, where `f` and `v` are literals, `t` is a type, `x` is an expression, and `env` is an environment
4. The language will have the following evaluation rules:
   1. `σ, Rec(f, v, t, x) ⇓ RecV(f, v, t, x, σ)`
      1. The literal `f` must be a valid identifier
      2. The literal `v` must be a valid identifier
   2. `σ, Apply(RecV(f, v, t, x, env), y) ⇓ (env[f -> RecV(f, v, t, x, env), v -> y], x)`
      1. The literal `f` must be a valid identifier
      2. The literal `v` must be a valid identifier
      3. The type of `y` must be `t`
5. The language will have the following type-checking rules:
   1. `σ, Rec(f, v, t, x) : Func(t, t2)`
      1. The literal `f` must be a valid identifier
      2. The literal `v` must be a valid identifier
      3. `t2` is the type of `σ[f -> t, v -> t], x`
6. The following scenarios will result in an error:
   1. The literal `f` in `Rec(f, v, t, x)` is not a valid identifier
   2. The literal `v` in `Rec(f, v, t, x)` is not a valid identifier
   3. When evaluating `Apply(RecV(f, v, t, x, env), y)`, the type of `y` is not equal to `t`
   4. When evaluating the application of a recursive function, an infinite loop occurs
      1. This is not relevant to type-checking, as the recursive function is not explored
      2. This should trigger if the depth of an evaluation exceeds 100
