# Glossary

## Action

Represents a single modification to an [evaluation tree](#evaluation-tree) or [type-checking tree](#type-checking-tree).

While editing the tree, each change the user makes a request to the server to perform an action.
The server returns the new tree, which is updated according to the action.

All actions require the original node tree, a [tree path](#tree-path) to the node being modified,
and then whatever additional arguments that specific action requires.

## Axiom

An expression which cannot be reduced further.
For example, `3` is an axiom in the language of arithmetic.
An [evaluation tree](#evaluation-tree) is complete if all paths end in an axiom.

## Environment

A mapping from variables to values or types.
For example, in the expression `λ(x: Int). x + 1`, the environment is `{x: Int}`.
The environment is used to determine the value of a [variable](#variable).

## Evaluation

The process of determining the value of an expression.
In this project, this is done by applying [evaluation rules](#evaluation-rule) to subexpressions until the expression is
reduced to a value.
This is displayed as an [evaluation tree](#evaluation-tree), where each [node](#node) is an expression.

## Evaluation Error

If the [expression](#expression) is not well-typed, or if the [evaluation](#evaluation) process cannot be completed,
evaluation instead produces an error.

## Evaluation Rule

A rule which describes how to evaluate an expression.
For example, the evaluation rule for addition is `Plus(Num(x), Num(y)) -> Num(x + y)`.

## Evaluation Tree

A visual representation of the [evaluation](#evaluation) process, where each [node](#node) represents an expression or
part of an expression.

## Expression

Something that can be evaluated to a value in a given language.
For example `(3 + 5)` is an expression in the language of arithmetic which can be written as `Plus(Num(3), Num(5))`.
The process of finding the value of an expression is called [evaluation](#evaluation).

## Inner Node

A [node](#node) which has an [outer node](#outer-node) as its parent.
These may only have [outer nodes](#outer-node) as children.
Examples of inner nodes are [literal nodes](#literal-node) and [subexpression nodes](#subexpression-node).

## Language

A set of [evaluation rules](#evaluation-rule) and [type-checking rules](#type-checking-rule).
For example, the language of arithmetic is the set of all arithmetic evaluation rules and type-checking rules.

## Literal

A value which is not an expression.
For example, `3` is a literal in the language of arithmetic, or the variable name `foo` in more complex languages.
Literals can contain any text, but may have specific syntax rules depending on the expression using them.
A literal cannot have a [subtree](#subtree).

## Literal Node

An [inner node](#inner-node) which contains a [literal](#literal).
These appear in certain (expressions)[#expression], such as `Num` or `Var`.
In the interface, these are represented as text boxes.

## Node

Used in the internal representation of trees, since they are not always 1:1 with expressions.
These are constructed by the user through the interface.
They can have invalid contents, such as a blank expression or an invalid [literal](#literal).

There are two different kinds of nodes, [outer](#outer-node) and [inner](#inner-node) nodes.
The node structure enforces that Outer nodes can only have Inner nodes as arguments, and Inner nodes can only have Outer
nodes as children.

## Outer Node

A [node](#node) which appears as a rule in an [evaluation tree](#evaluation-tree).
These may only have [inner nodes](#inner-node) as arguments.

## Phantom Node

A [node](#node) which is never part of a node tree, but may appear in certain [evaluation rules](#evaluation-rule)
or [type-checking rules](#type-checking-rule).

These are required to represent certain rules, such as applying a function to an argument which creates a
third [subtree](#subtree).

## Scope

Synonymous with [environment](#environment).

## Subexpression Node

An [inner node](#inner-node) which contains an [outer node](#outer-node).

## Subtree

A part of an [evaluation](#evaluation)/[type-checking tree](#type-checking-tree).
For example, in the expression `(3 + 5) * 2`, the subtree `(3 + 5)` is the left child of the root node.

## Tree Path

A list of zero-indexed indices which describes the path from the root of a tree to a specific node.

## Type

What kind a value is.
For example, the type of `3` is `Int`, `true` is `Bool`, and `λ(x: Int). x + 1` is a function from `Int` to `Int`.
More complex composite types are possible, such as function types (e.g. `Int -> Bool`).

## Type-Checking

The process of determining the type of an expression.
Successful type-checking means that the expression is well-typed.
It is typically required for an expression to be well-typed before it can be successfully [evaluated](#evaluation).
This is similar to [evaluation](#evaluation), but instead of applying [evaluation rules](#evaluation-rule),
type-checking rules are applied.

## Type-Checking Rule

A rule which describes how to determine the type of an expression.
For example, the type-checking rule for addition is `Plus(Num(x), Num(y)) -> Int`.

## Type-Checking Tree

A visual representation of the [type-checking](#type-checking) process, similar to
an [evaluation tree](#evaluation-tree), but focused on determining the types of expressions.

## Value

The result of [evaluating](#evaluation) an expression.
For example, the value of `(3 + 5)` is `8`.

## Variable

A name which can be used to refer to a value.
For example, in the expression `λ(x: Int). x + 1`, `x` is a variable.
A variable can only be used in the scope in which it is defined.

## Variable Node

The primary kind of [outer node](#outer-node).
It is named this way since its contents can be any kind of [expression](#expression).
The name is a bit misleading, since it can contain any expression, not just a [variable](#variable).
Every expression has its own variable node.
