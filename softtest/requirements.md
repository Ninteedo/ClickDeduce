# Requirements

## Interface

1. The system will allow the user to intuitively view an evaluation tree
   1. The user will be able to click and drag the tree to pan around it
   2. The user will be able to scroll to zoom in and out
   3. The subtree that the user is hovering over will be highlighted
   4. The evaluation tree will be displayed as according to the [tree HTML](#tree-html) format
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
      2. Once the user has finished typing, the evaluation tree should be updated
         1. The user has finished typing when they click outside the text box, or press enter, escape, or tab
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
         1. The server will check that the tree does not contain any evaluation rules or expressions which do not
            exist in the new language
      2. The user will then be able to use evaluation rules from the new language
      3. The language will be switched whenever the user changes the language dropdown value

## Server API

1. The server will return appropriate files when requested
   1. The server will return the index.html file when the user requests the root path, or specifically requests
      index.html
   2. The server will return files from the `images` subfolder when the user requests them
   3. The server will return files from the `dist` subfolder when the user requests them
   4. The server will not return files from any other folders, and will return a 404 error
2. The server will expose the `get-lang-selector` endpoint
   1. It will respond to GET requests
   2. It will return a JSON object with the following property:
      1. `langSelectorHtml` - A string containing the HTML for the language selector dropdown menu
3. The server will expose the `start-node-blank` endpoint
   1. It will respond to POST requests
   2. The request body will be a JSON object with the following properties:
      1. `langName` - A string containing the name of the language to use
         1. This must match the name of a language provided in the values of the HTML returned by
            the `get-lang-selector` endpoint
         2. Invalid values should respond with a 400 error
      2. Other properties may be included, but will be ignored
   3. It will return a JSON object with the following properties:
      1. `nodeString` - A string of the node tree, in the format described in the [Node Tree](#node-tree) section
      2. `html` - A string containing the HTML for the node tree
   4. The represented node tree will be a single root node, with an expression select dropdown menu
4. The server will expose the `process-action` endpoint
   1. It will respond to POST requests
   2. The request body will be a JSON object with the following properties:
      1. `langName` - A string containing the name of the language to use
         1. This must match the name of a language provided in the values of the HTML returned by
            the `get-lang-selector` endpoint
      2. `modeName` - A string which matches an available processing mode
         1. Valid options are:
            1. `edit`
            2. `type-check`
            3. `eval`
      3. `actionName` - A string which matches a possible [action](#actions)
      4. `nodeString` - A string of the node tree, in the format described in the [Node Tree](#node-tree) section
      5. `treePath` - A string follows the format for [tree paths](#tree-paths)
      6. `extraArgs` - A list of strings, containing any extra arguments for the action
         1. The list may be empty
         2. Additional arguments result in an error
   3. If any part of the request body is invalid, the server will respond with a 400 error
   4. It will return a JSON object with the following properties:
      1. `nodeString` - A string of the node tree, in the format described in the [Node Tree](#node-tree) section
      2. `html` - A string containing the HTML for the node tree
5. If a valid request is made, but an error occurs while processing it, the server will respond with a
   context-appropriate error code and message

## Webapp

1. The webapp will be a single page application
   1. This means that the page will not be reloaded when the user interacts with the interface and there will be only
      one HTML page
2. The main page will be served from the root path
3. The webapp will be served from a Scala server
   1. The server will be written using the Akka HTTP library
4. The code for the webapp will be written in TypeScript
   1. The scripts will be bundled using Webpack and Babel during the server startup process
   2. The original TypeScript files will not be served to the client
5. The styling for the webapp will be written in CSS
   1. The styles will be bundled using Webpack during the server startup process
   2. The original CSS files will not be served to the client

## Languages

The following languages, following the grammars defined in the example languages of the Elements of Programming
Languages course, will be implemented:

1. [Arithmetic](#arithmetic-larith)
2. [Boolean](#boolean-lif)
3. [Variable](#variable-llet)
4. [Lambda Functions](#lambda-functions-llam)
5. [Recursive Functions](#recursive-functions-lrec)

They should each be available separately for the user to select from the language dropdown menu.

### Arithmetic (LArith)

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
   1. The literal `x` in `Num(x)` contains any characters other than digits and optionally a minus sign at the
      beginning
   2. The type of the `x` and `y` values in `Plus(x, y)` are not `Int`
   3. The type of the `x` and `y` values in `Times(x, y)` are not `Int`
   4. Any attempt at evaluation or type-checking that does not match a defined rule

### Boolean (LIf)

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

### Variable (LLet)

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

### Lambda Functions (LLam)

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

### Recursive Functions (LRec)

1. The following requirements are in addition to the requirements for the Lambda Functions language
2. The language will support the following expressions:
   1. `Rec(f, v, t, x)`, where `f` and `v` are literals, `t` is a type, and `x` is an expression
3. The language will have the following values:
   1. `RecV(f, v, t, x, env)`, where `f` and `v` are literals, `t` is a type, `x` is an expression, and `env` is an
      environment
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

## Node Tree

The node tree is a less mathematically rigorous representation of the evaluation tree, which allows for more flexibility
in the interface.
Without this representation, it would be difficult for users to construct a tree, especially one that is not
well-formed.

1. Any node tree can be converted to a string which can be converted back into the same node tree
2. Any expression can be represented as a node tree

### Tree Paths

A tree path is a string which represents a path through a node tree.

1. A tree path is a list of non-negative integers, separated by hyphens
2. It must be possible to find any node in a node tree using an appropriate tree path
   1. Phantom nodes ignore this requirement, as they are not a proper part of a node tree

## Tree HTML

Node trees are represented in the interface as HTML.

1. Any node tree can be converted to HTML
   1. This does not need to be reversible
2. A particular format for the HTML is required to allow for styling and interactivity
   1. Each subtree is represented as a `<div>` element with the class `subtree`
   2. Each subtree has a lower section displaying the expression of that subtree in a `<div>` element with the `expr`
      class
      1. If the expression contains any literals, they should be represented as text boxes inside the `expr` div with
         the `literal` class
         1. The width of the text box should match the width of the literal, and should be updated when typing in the
            text box
            1. The width of the text box should be at least 2 characters wide
         2. When appearing inside a parent expression, the text box should be read-only
      2. The expression text should display the whole expression, including any subexpressions
         1. For clarity, literals from subexpressions should be displayed as read-only text boxes
         2. Select dropdown menus should be displayed but disabled
      3. The evaluation or type-checking result should appear on the right-hand side of the expression
         1. The value or type should be pretty printed
            1. The exact format of the pretty printed value or type is not specified, but should generally be
               human-readable and not too long
         2. If there was an error, the result should be a question mark displayed in red
            1. Hovering over the error should display a tooltip with the error message
            2. The message should be specific to the error that occurred
         3. Hovering over the result should display the internal representation of the result, instead of the pretty
            printed version
         4. For evaluation, the result should be in a `<div>` element with the `eval-result` class
         5. For type-checking, the result should be in a `<div>` element with the `type-check-result` class
   3. A subtree without any children is an axiom, and its `subtree` div should also have the `axiom` class
      1. A line should appear above the lower section of an axiom subtree
      2. The name of the rule that the axiom represents should appear above the line
   4. Each non-axiom subtree has an upper section displaying the subexpressions of that subtree in a `<div>` element
      with the `args` class
      1. There should be a horizontal line between the upper and lower sections of each subtree
      2. The `args` div should contain a `subtree` div for each subexpression
      3. It should also contain an annotation for the evaluation rule used, on the right-hand side of the
         subexpressions
   5. Each subtree should have a `data-tree-path` attribute, containing the [tree path](#tree-paths) of that subtree
   6. Each subtree should have a `data-node-string` attribute, containing the [node string](#node-tree) of that subtree
   7. Any tooltips in the tree should have the `tooltip` class and be hidden by default
