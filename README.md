# Usage

ClickDeduce is available at [clickdeduce.rgh.dev](https://clickdeduce.rgh.dev/).
The guide page can be found at [clickdeduce.rgh.dev/guide](https://clickdeduce.rgh.dev/guide).

To view the API docs, visit [clickdeduce.rgh.dev/api](https://clickdeduce.rgh.dev/api).

## Development

### Requirements

- JDK 18+
- [sbt 1.5+](https://www.scala-sbt.org/1.x/docs/Setup.html)
- [Node.js 16+](https://nodejs.org/en/learn/getting-started/how-to-install-nodejs)

### Setup

To install the required dependencies, run the following commands:

```bash
sbt update
npm install
```

### Project Layout

Project source files are present in [the shared Scala code directory](scala/shared/src/main/scala) for the Scala code
and [webapp/scripts](webapp/scripts) for the TypeScript code.

There are functions missing from the TypeScript code which require the ScalaJS code to be built first.
To build the ScalaJS code, run the following command:

```bash
sbt fastOptJS
```

This will generate `clickdeduce-opt.js` in `webapp/scripts`.

### Building Website

To build the website, run the following command:

```bash
npm run build
```

This will generate the website in the [webapp/build](webapp/build) directory,
including [index.html](webapp/build/index.html) for the main page.

### Creating a new language

To begin, create a new `.scala` class file in the [languages](scala/shared/src/main/scala/languages) directory.
Using `LIf` as an example it should start as follows
(replace `LIf` with the name of the new language, and `LArith` with the language being extended):

```scala 3
package languages

import convertors.*

class LIf extends LArith {

}

object LIf extends LIf {}
```

A language can extend an existing language or can be created from scratch by instead implementing `ClickDeduceLanguage`.

#### Adding Terms

Terms require a case class that extends `Expr`, `Type`, or `Value` depending on the type of term.
They also require a companion object that extends `ExprCompanion`, `TypeCompanion`, or `ValueCompanion` respectively.
Finally, the companion object's `register` method needs to be called within the language's body.

##### Case Class

The only state that a term should contain is in its constructor fields, all of which should be immutable.

All terms need to implement the `toText` method, which returns a `ConvertableText` object that represents the term in a common format that is later converted to HTML, LaTeX, or text as needed.

Expressions also need to implement the `evalInner`, `typeCheckInner`.

Types may implement `typeCheck` if they have subtypes (e.g. the function type, `Func` in `LLam`).

Here are examples of the classes for `Bool` and `IfThenElse`:

```scala 3
case class Bool(b: Literal) extends Expr {
 override def evalInner(env: ValueEnv): Value = b match {
   case LiteralBool(b) => BoolV(b)
   case _              => UnexpectedArgValue(s"Bool can only accept LiteralBool, not $b")
 }

 override def typeCheckInner(tEnv: TypeEnv): Type = b match {
   case LiteralBool(_) => BoolType()
   case _              => UnexpectedArgType(s"Bool can only accept LiteralBool, not $b")
 }

 override val needsBrackets: Boolean = false

 override def toText: ConvertableText = TextElement(b.toString)
}
```

```scala 3
case class IfThenElse(cond: Expr, then_expr: Expr, else_expr: Expr) extends Expr {
 override def evalInner(env: ValueEnv): Value = cond.eval(env) match {
   case BoolV(true)    => then_expr.eval(env)
   case BoolV(false)   => else_expr.eval(env)
   case v if v.isError => v
   case v              => TypeMismatchError("IfThenElse", v.typ, BoolType())
 }

 override def typeCheckInner(tEnv: TypeEnv): Type = cond.typeCheck(tEnv) match {
   case BoolType() =>
     val t1 = then_expr.typeCheck(tEnv)
     val t2 = else_expr.typeCheck(tEnv)
     if (t1 == t2) t1
     else TypeMismatchType(t1, t2)
   case t => TypeMismatchType(t, BoolType())
 }

 override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] = cond.eval(env) match {
   case BoolV(true)  => List((cond, env), (then_expr, env))
   case BoolV(false) => List((cond, env), (else_expr, env))
   case _            => List((cond, env), (then_expr, env), (else_expr, env))
 }

 override def toText: ConvertableText = MultiElement(
   TextElement("if "),
   cond.toTextBracketed,
   TextElement(" then "),
   then_expr.toTextBracketed,
   TextElement(" else "),
   else_expr.toTextBracketed
 )
}
```

##### Companion Object

The companion object primarily exists to provide a `register` method that registers the term with the language.

It has to implement the `createExpr` method if it is for an `Expr` or `createType` for a `Type`,
which when provided a list of literals and terms returns an instance of the associated term if the arguments are valid.
These should also handle the case where no arguments are provided, in which case the default term should be returned.

The companion object may also provide a list of aliases for the term,
which are also matched when the user filters the term list.

```scala 3
object Bool extends ExprCompanion {
 def apply(b: Boolean): Bool = new Bool(LiteralBool(b))

 override def createExpr(args: BuilderArgs): Option[Expr] = args match {
   case List(b: Literal) => Some(Bool(b))
   case Nil              => Some(Bool(defaultLiteral))
   case _                => None
 }

 override val aliases: List[String] = List("Boolean", "True", "False")
}
```

```scala 3
object IfThenElse extends ExprCompanion {
 override def createExpr(args: BuilderArgs): Option[Expr] = args match {
   case List(cond: Expr, then_expr: Expr, else_expr: Expr) => Some(IfThenElse(cond, then_expr, else_expr))
   case Nil                                                => Some(IfThenElse(defaultExpr, defaultExpr, defaultExpr))
   case _                                                  => None
 }
}
```
