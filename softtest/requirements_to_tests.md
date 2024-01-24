# Mapping of Requirements to Tests

This document matches the ordered list of requirements in the [requirements document](./requirements.md) to the tests
that cover them.

If a requirement does not need to be tested, it is marked as "N/A".

If a requirement that should be tested is not covered by any tests, it is marked as "Not tested".

## Server

### Process

1. N/A
   1. N/A
2. N/A
   1. ArgsHandlingSpec -> "Port number"
   2. ArgsHandlingSpec -> "Binding address"
   3. ArgsHandlingSpec -> "Skip bundle scripts option"
3. Not tested
4. RouteSpec -> "The process-action endpoint should handle POST requests" -> tests that mention errors
5. Not tested

### API

1. RouteSpec -> "The GET requests should return appropriate files"
   1. RouteSpec -> "The GET requests should return appropriate files" -> "return the index.html file for the '/' path"
   2. RouteSpec -> "The GET requests should return appropriate files" -> "return the 'images/zoom_to_fit.svg' file"
   3. RouteSpec -> "The GET requests should return appropriate files" -> "return the contents of 'dist/bundle.js'"
   4. RouteSpec -> "The GET requests should return appropriate files" -> "does not return contents from ..."
2. RouteSpec -> "The get-lang-selector endpoint should handle GET requests"
3. RouteSpec -> "The start-node-blank endpoint should handle POST requests"
4. RouteSpec -> "The process-action endpoint should handle POST requests"
5. RouteSpec -> "The process-action endpoint should handle POST requests" -> tests that mention errors

## Webapp

1. N/A
2. RouteSpec -> "The GET requests should return appropriate files" -> "return the index.html file for the '/' path"
3. Not tested
4. N/A
5. N/A

## Interface

1. Requires user testing
   1. Not tested - depends on PanZoom
   2. Not tested - depends on PanZoom
      1. Not tested - depends on PanZoom
   3. treeManipulation.mock.test -> "hovering over a node behaves correctly" -> "mousing over a node highlights it"
   4. NodeHTMLSpec
2. Requires user testing
   1. actions.mock.test -> "start new node button behaves correctly"
   2. actions.mock.test -> "selecting an option from the root expr dropdown behaves correctly" and "selecting an option
      from a non-root expr dropdown behaves correctly"
   3. Not tested
   4. Not tested
   5. actions.mock.test -> "entering text into a literal input behaves correctly"
3. N/A
   1. interface.mock.test -> "context menu behaves correctly"
   2. actions.mock.test -> "delete, copy, and paste buttons behave correctly" -> "pressing delete makes the correct
      request to the server"
   3. actions.mock.test -> "delete, copy, and paste buttons behave correctly" -> tests mentioning copy or paste
   4. Not tested
4. N/A
   1. serverIntegration.test -> "behaviour of changing the selected language is correct"
      1. serverIntegration.test -> "lang selector is correctly initialised on load"
      2. Not tested
   2. serverIntegration.test -> "behaviour of changing the selected language is correct"
      1. serverIntegration.test -> "behaviour of changing the selected language is correct" -> "can change to a parent
         language, as long as the current tree only uses expressions present in the parent language"
      2. serverIntegration.test -> "behaviour of changing the selected language is correct" -> "can change to a child
         language with more existing expressions"
      3. serverIntegration.test -> "behaviour of changing the selected language is correct"

## Languages

### Arithmetic (LArith)

1. N/A
2. N/A
3. N/A
4. N/A
   1. LArithTest -> "Num correctly evaluates to NumV"
      1. LArithTest -> "Num with non-integer literal inputs results in errors", "Num with integer literal inputs is
         correctly interpreted"
      2. LArithTest -> "Num can accept integers at least 100 digits long"
   2. LArithTest -> "Plus evaluates correctly" (from arithmeticOperationTests function)
   3. LArithTest -> "Times evaluates correctly" (from arithmeticOperationTests function)
5. N/A
   1. LArithTest -> "Num type-checks to IntType"
      1. LArithTest -> "Num with non-integer literal inputs results in errors", "Num with integer literal inputs is
         correctly interpreted"
   2. LArithTest -> "Plus type-checks correctly" (from arithmeticOperationTests function)
   3. LArithTest -> "Times type-checks correctly" (from arithmeticOperationTests function)
6. N/A
   1. LArithTest -> "Num with non-integer literal inputs results in errors"
   2. LArithTest -> "Plus type-checks correctly" (from arithmeticOperationTests function)
   3. LArithTest -> "Times type-checks correctly" (from arithmeticOperationTests function)
   4. N/A

### Boolean (LIf)

1. N/A
2. N/A
3. N/A
4. N/A
5. N/A
   1. LIfTest -> "Bool evaluates correctly"
      1. LIfTest -> "Bool returns an error when given an argument other than LiteralBool"
   2. LIfTest -> "Basic Eq expression evaluates correctly"
   3. LIfTest -> "Basic IfThenElse expressions evaluates correctly"
   4. LIfTest -> "Basic IfThenElse expressions evaluates correctly"
6. N/A
   1. LIfTest -> "BoolV's type is BoolType"
   2. LIfTest -> "Eq type-checks to BoolType when both sides have the same type"
      1. LIfTest -> "Eq type-checks to an error when the sides have different types"
   3. LIfTest -> "IfThenElse correctly type-checks when both branches have the same type"
      1. LIfTest -> "IfThenElse type-checks to an error when the branches have different types"
7. N/A
   1. LIfTest -> "Bool returns an error when given an argument other than LiteralBool"
   2. LIfTest -> "Eq type-checks to an error when the sides have different types"
   3. LIfTest -> "IfThenElse type-checks to an error when the branches have different types"
   4. LIfTest -> "IfThenElse type-checks to an error when the condition is not a BoolType"

### Variable (LLet)

1. N/A
2. N/A
3. N/A
4. N/A
5. N/A
6. LLetTest -> "Invalid variable names result in an error"
7. N/A
   1. LLetTest -> "Var correctly evaluates with simple environment", "Var correctly evaluates with big environment"
   2. LLetTest -> "Let with single Let in expression evaluates correctly", "Let with multiple Lets in expression
      evaluates correctly"
8. N/A
   1. LLetTest -> "Var correctly type-checks with simple environment", "Var correctly type-checks with big environment"
   2. LLetTest -> "Let with single Let in expression type-checks correctly", "Let with multiple Lets in expression
      type-checks correctly"
9. N/A
   1. LLetTest -> "Var results an error when variable not found"
   2. LLetTest -> "Invalid variable names result in an error"

### Lambda Functions (LLam)

1. N/A
2. N/A
3. N/A
4. N/A
5. N/A
   1. LLamTest -> "Lambda evaluates correctly", "Lambda correctly evaluates with existing environment"
   2. LLamTest -> "Apply correctly evaluates"
6. N/A
   1. LLamTest -> "Lambda type-checks correctly", "Lambda correctly type-checks with existing environment"
   2. LLamTest -> "Apply correctly type-checks"
7. N/A
   1. LLetTest -> "Invalid variable names result in an error"
   2. LLamTest -> "Apply results in error when the right side does not match the function input type"
   3. LLamTest -> "Apply result in error when left side is not a function"

### Recursive Functions (LRec)

1. N/A
2. N/A
3. N/A
4. N/A
   1. LRecTest -> "Rec evaluates correctly"
   2. LRecTest -> "Applying with Rec evaluates correctly"
5. N/A
   1. LRecTest -> "Rec type-checks correctly"
      3. LRecTest -> "Rec verifies that its expression matches the reported type"
6. N/A
   1. LRecTest -> "Rec returns an error when the function or parameter names are not valid identifiers"
   2. LRecTest -> "Rec returns an error when the function or parameter names are not valid identifiers"
   3. LRecTest -> "Applying with Rec evaluates correctly"
   4. LRecTest -> "Infinite recursion results in a stack overflow error", "Infinite recursion in nodes results in a
      DepthLimitExceededException in evaluation mode"

## Node Tree

1. NodeTreeTest -> any test that starts with "Can correctly read"
2. NodeSpec -> "VariableNode" -> "correctly convert from an expression (without types)", "correctly convert from an
   expression (with types)"

### Node Kinds

#### Outer Nodes

1. Not explicitly tested
2. Not explicitly tested

##### Expression Node

1. N/A
2. N/A
3. NodeSpec -> "VariableNode" -> "correctly convert from an expression (without types)", "correctly convert from an
   expression (with types)"

##### Type Node

1. N/A
2. N/A
3. NodeSpec -> "TypeNode" -> "correctly convert from a type"

#### Inner Nodes

1. NodeSpec -> "SubExprNode" -> "cannot be a root node"
2. N/A
3. N/A

##### Literal Node

1. N/A
2. N/A
3. NodeSpec -> "LiteralNode" -> tests beginning with "correctly convert to and from a string"

##### Subexpression Node

1. N/A
2. NodeSpec -> "SubExprNode" -> "not be able to have a TypeNode as a parent"

##### Subtype Node

1. N/A

### Tree Paths

1. N/A
2. NodeSpec -> "Tree paths" -> all tests

### Tree HTML

Some of the requirements here that are directly tested may be covered in the serverIntegration.test file.
A lot of the uncovered requirements are purely visual and rely on CSS styling, which is not tested.

1. N/A
2. N/A
   1. NodeHTMLSpec -> "be correct with single node tree" -> "root should have subtree and axiom classes"
   2. NodeHTMLSpec -> "be correct with single node tree" -> "should have a child '.expr' div"
      1. NodeHTMLSpec -> "be correct with single node tree" -> "should have a child '.expr' div" -> "should contain a
         div which contains an input"
         1. Not tested
         2. Indirectly covered in serverIntegration.test
      2. Not tested
         1. Not tested
         2. Not tested
      3. NodeHTMLSpec -> "be correct with single node tree" -> "should have a child '.expr' div" -> "should contain a
         result div"
         1. Not tested
         2. Not tested
         3. Not tested
         4. Not tested
         5. Not tested
   3. NodeHTMLSpec -> "be correct with single node tree" -> "root should have subtree and axiom classes"
      1. Not tested
      2. Not tested
   4. NodeHTMLSpec -> "be correct with a complex node tree" -> "should subtrees in edit/type-checking mode" -> "root
      should have two subtrees"
      1. Not tested
      2. NodeHTMLSpec -> "be correct with a complex node tree" -> "should subtrees in edit/type-checking mode" -> "root
         should have two subtrees"
      3. Not tested
   5. NodeHMTLSpec -> all tests mentioning 'data-tree-path'
   6. NodeHTMLSpec -> all tests mentioning 'data-node-string'
   7. Not tested

### Actions

#### Select Expression

1. ActionSpec -> "Can create Actions using createAction" -> "create a SelectExprAction"
2. ActionSpec -> "Can create Actions using createAction" -> "create a SelectExprAction"
   1. ActionSpec -> "SelectExprAction" -> "throw an error if the expression kind is not defined in the language"
3. ActionSpec -> "SelectExprAction" -> "throw an error when attempting to replace something other than an
   ExprChoiceNode"
4. ActionSpec -> "SelectExprAction" -> "replace a root ExprChoiceNode with selection", "replace a nested ExprChoiceNode
   with selection"

#### Select Type

1. ActionSpec -> "Can create Actions using createAction" -> "create a SelectTypeAction"
2. ActionSpec -> "Can create Actions using createAction" -> "create a SelectTypeAction"
   1. ActionSpec -> "SelectTypeAction" -> "throw an error if the type kind is not defined in the language"
3. ActionSpec -> "SelectTypeAction" -> "throw an error when attempting to replace something other than a TypeChoiceNode"
4. ActionSpec -> "SelectTypeAction" -> "replace a root TypeChoiceNode with selection", "replace a nested TypeChoiceNode
   with selection"

#### Edit Literal

1. ActionSpec -> "Can create Actions using createAction" -> "create an EditLiteralAction"
2. ActionSpec -> "Can create Actions using createAction" -> "create an EditLiteralAction"
3. ActionSpec -> "EditLiteralAction" -> "throw an error when attempting to replace something other than a LiteralNode"
4. ActionSpec -> "EditLiteralAction" -> "replace the contents of a nested LiteralNode"

#### Delete

1. ActionSpec -> "Can create Actions using createAction" -> "create a DeleteAction"
2. ActionSpec -> "Can create Actions using createAction" -> "create a DeleteAction"
3. ActionSpec -> "DeleteAction" -> "delete an expr node from the tree", "delete a type node from the tree", "throws an
   error when attempting to delete a literal node"

#### Paste

1. ActionSpec -> "Can create Actions using createAction" -> "create a PasteAction"
2. ActionSpec -> "Can create Actions using createAction" -> "create a PasteAction"
   1. ActionSpec -> "PasteAction" -> "throws an error when attempting to paste an invalid node string"
3. ActionSpec -> "PasteAction" -> "correctly paste an expr node string into a tree", "correctly paste a type node string
   into a tree"
   1. ActionSpec -> "PasteAction" -> "throws an error when attempting to paste into a literal node"
   2. ActionSpec -> "PasteAction" -> "correctly paste an expr node string into a tree"
   3. Not tested
4. ActionSpec -> "PasteAction" -> "correctly paste an expr node string into a tree", "correctly paste a type node string
   into a tree"

#### Identity

1. ActionSpec -> "Can create Actions using createAction" -> "create an IdentityAction"
2. ActionSpec -> "Can create Actions using createAction" -> "create an IdentityAction"
3. Not tested
4. Not tested
