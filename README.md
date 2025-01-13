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

## Creating a new language

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

### Adding Terms

Terms require a case class that extends `Expr`, `Type`, or `Value` depending on the type of term.
They also require a companion object that extends `ExprCompanion`, `TypeCompanion`, or `ValueCompanion` respectively.
Finally, the companion object's `register` method needs to be called within the language's body.

#### Case Class

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

#### Companion Object

The companion object primarily exists to provide a `register` method that registers the term with the language.

It has to implement the `createExpr` method if it is for an `Expr` or `createType` for a `Type`,
which when provided a list of literals and terms returns an instance of the associated term if the arguments are valid.
These should also handle the case where no arguments are provided, in which case the default term should be returned.

The companion object may also provide a list of aliases for the term,
which are also matched when the user filters the term list.

```scala 3
object Bool extends ExprCompanion {
 def apply(b: Boolean): Bool = new Bool(LiteralBool(b))

 override def create(args: BuilderArgs): Option[Expr] = args match {
   case List(b: Literal) => Some(Bool(b))
   case Nil              => Some(Bool(defaultLiteral))
   case _                => None
 }

 override val aliases: List[String] = List("Boolean", "True", "False")
}
```

```scala 3
object IfThenElse extends ExprCompanion {
 override def create(args: BuilderArgs): Option[Expr] = args match {
   case List(cond: Expr, then_expr: Expr, else_expr: Expr) => Some(IfThenElse(cond, then_expr, else_expr))
   case Nil                                                => Some(IfThenElse(defaultExpr, defaultExpr, defaultExpr))
   case _                                                  => None
 }
}
```

### Registering Language

The language needs to be registered in the `knownLanguages` list
in [ScalaJsEntry.scala](scala/js/src/main/scala/app/ScalaJsEntry.scala).
The languages in the interface appear in the order they are listed in here.
The key is the display name of the language, and the value is an instance of the language.

```scala 3
private val knownLanguages: List[(String, ClickDeduceLanguage)] = List(
 "LArith" -> LArith(),
 "LIf" -> LIf(),
 "LLet" -> LLet(),
 "LLam" -> LLam(),
 "LRec" -> LRec(),
 "LData" -> LData(),
 "LPoly" -> LPoly(),
)
```

### `LList` Example

We will implement a new language that adds lists.

#### Grammar
Expressions, types, and values matching the following grammar will be added:

```
e ::= ... | nil | e1 :: e2 | case-list e of {nil => e1; x :: xs => e2}
v ::= ... | nil | v1 :: v2
τ ::= ... | list[τ]
```

Evaluation rules:
```
---------
nil ⇓ nil

 e1 ⇓ v1   e2 ⇓ v2
-------------------
e1 :: e2 ⇓ v1 :: v2

         e ⇓ nil      e1 ⇓ v
----------------------------------------
case e of {nil => e1; x :: xs => e2} ⇓ v

  e ⇓ v1 :: v2    e2[x := v1, xs := v2] ⇓ v
---------------------------------------------
case-list e of {nil => e1; x :: xs => e2} ⇓ v
```

Type-checking rules:
```
-----------------
Γ ⊢ nil : list[τ]

Γ ⊢ e1 : τ   Γ ⊢ e2 : list[τ]
-----------------------------
   Γ ⊢ e1 :: e2 : list[τ]

Γ ⊢ e : list[τ1]   Γ ⊢ e1 : τ2   Γ[x ::= τ1, xs ::= list[τ2]] ⊢ e2 : τ2
-----------------------------------------------------------------------
           Γ ⊢ case-list e of {nil => e1; x :: xs => e2} : τ2
```

#### List Type

Our first step is to implement the `ListType` case class and its companion object.

The `ListType` case class extends `Type` and contains a field, `elTyp`,
which is the type of the elements in the list.

Note that the companion object's `create` method returns an instance of `ListType` when
provided with a list containing a single type for the `elTyp` field.
If no arguments are provided, a list of the default type is returned.
Otherwise, `None` is returned.

```scala 3
case class ListType(elTyp: Type) extends Type {
  override def typeCheck(tEnv: TypeEnv): Type = ListType(elTyp.typeCheck(tEnv))

  override def toText: ConvertableText =
    MultiElement(TextElement("List"), TextElement("["), elTyp.toText, TextElement("]"))

  override val needsBrackets: Boolean = false

  override val isError: Boolean = elTyp.isError
}

object ListType extends TypeCompanion {
  override def create(args: BuilderArgs): Option[Type] = args match {
    case List(elTyp: Type) => Some(ListType(elTyp))
    case Nil               => Some(ListType(defaultType))
    case _                 => None
  }
}
```

#### Values

Creating new value classes is similar to creating new types.
We need to implement a case class that extends `Value` and a companion object that extends `ValueCompanion`.

`ValueCompanion` does not need to implement any methods, as they are not interacted with by users in the same way.

##### Nil

```scala 3
case class NilV(elTyp: Type) extends Value {
 override def toText: ConvertableText = TextElement("Nil")

 override val typ: Type = ListType(elTyp)

 override val needsBrackets: Boolean = false

 override val isError: Boolean = elTyp.isError
}

object NilV extends ValueCompanion {}
```

##### Cons

```scala 3
case class ConsV(head: Value, tail: Value) extends Value {
 override def toText: ConvertableText = MultiElement(head.toTextBracketed, TextElement(" :: "), tail.toText)

 override val typ: Type = ListType(head.typ)

 override val isError: Boolean = head.isError || tail.isError
}

object ConsV extends ValueCompanion {}
```

#### Expressions

As shown in the grammar, we need to implement
`nil` (a list with no elements),
`::` (aka `Cons`, a list with a head and a tail),
and `case-list` (a case expression for lists).

##### Nil

Starting with `nil`,
we need to create a case class that extends `Expr` and a companion object that extends `ExprCompanion`.

The name `Nil` is already used in Scala, so we will use `ListNil` instead.

ClickDeduce does not have type inference, so we need to provide the type of the list as an argument,
so that it can be type-checked correctly.

```scala 3
case class ListNil(elTyp: Type) extends Expr {
  override protected def evalInner(env: ValueEnv): Value = NilV(elTyp)

  override protected def typeCheckInner(tEnv: TypeEnv): Type = ListType(elTyp)

  override def toText: ConvertableText = TextElement("Nil")
}

object ListNil extends ExprCompanion {
  override def create(args: BuilderArgs): Option[Expr] = args match {
    case List(elTyp: Type) => Some(ListNil(elTyp))
    case Nil               => Some(ListNil(defaultType))
    case _                 => None
  }

  override protected val name: String = "Nil"

  override protected val aliases: List[String] = List("ListNil")
}
```

##### Cons

Next, we will implement the `Cons` case class.

For both evaluation and type-checking, we match against the result of evaluating/type-checking
both the head and the tail of the list.
If either of them is an error, we return that error.

For type-checking, we also need to ensure that the tail is a list of the same type as the head.

```scala 3
case class Cons(head: Expr, tail: Expr) extends Expr {
   override protected def evalInner(env: ValueEnv): Value = (head.eval(env), tail.eval(env)) match {
     case (headV, _) if headV.typ.isError => headV
     case (_, tailV) if tailV.typ.isError => tailV
     case (headV, tailV)                  => ConsV(headV, tailV)
   }

   override protected def typeCheckInner(tEnv: TypeEnv): Type = (head.typeCheck(tEnv), tail.typeCheck(tEnv)) match {
     case (headTyp, _) if headTyp.isError              => headTyp
     case (_, tailTyp) if tailTyp.isError              => tailTyp
     case (elTyp, ListType(elTyp2)) if elTyp == elTyp2 => ListType(elTyp)
     case (headTyp, tailTyp)                           => ListTypeMismatchError(headTyp, tailTyp)
   }

   override def toText: ConvertableText = MultiElement(head.toTextBracketed, TextElement(" :: "), tail.toText)
}

object Cons extends ExprCompanion {
  override def create(args: BuilderArgs): Option[Expr] = args match {
    case List(head: Expr, tail: Expr) => Some(Cons(head, tail))
    case Nil                          => Some(Cons(defaultExpr, defaultExpr))
    case _                            => None
  }

  override protected val aliases: List[String] = List("ListCons", "::")
}
```

##### Case List

Finally, we will implement the `CaseList` case class.
This is significantly more complex than the previous two expressions.
It has different evaluation and type-checking rules based on the value of the list being matched.

It has to manage variable bindings for the `::` case, which requires overriding the
`getChildrenBase`, `getChildrenTypeCheck`, and `getChildrenEval` methods.

```scala 3
case class CaseList(list: Expr, nilCase: Expr, headVar: Literal, tailVar: Literal, consCase: Expr) extends Expr {
 override protected def evalInner(env: ValueEnv): Value = list.eval(env) match {
   case NilV(_)           => nilCase.eval(env)
   case ConsV(head, tail) => consCase.eval(consEnv(env, head, tail))
   case v                 => ListCaseNotListError(v)
 }

 override protected def typeCheckInner(tEnv: TypeEnv): Type = list.typeCheck(tEnv) match {
   case ListType(elTyp) =>
     (nilCase.typeCheck(tEnv), consCase.typeCheck(consTEnv(tEnv, elTyp))) match {
       case (nilTyp, _) if nilTyp.isError          => nilTyp
       case (_, consTyp) if consTyp.isError        => consTyp
       case (nilTyp, consTyp) if nilTyp == consTyp => nilTyp
       case (nilTyp, consTyp)                      => ListTypeMismatchError(nilTyp, consTyp)
     }
   case t => ListCaseNotListTypeError(t)
 }

 private def consEnv(env: ValueEnv, head: Value, tail: Value): ValueEnv =
   env + (headVar.toString -> head) + (tailVar.toString -> tail)
 private def consTEnv(tEnv: TypeEnv, elTyp: Type): TypeEnv =
   tEnv + (headVar.toString -> elTyp) + (tailVar.toString -> ListType(elTyp))

 override def toText: ConvertableText = MultiElement(
   TextElement("case"),
   SpaceAfter(SubscriptElement(TextElement("list"))),
   list.toTextBracketed,
   TextElement(" of { "),
   TextElement("Nil"),
   SurroundSpaces(Symbols.doubleRightArrow),
   nilCase.toTextBracketed,
   TextElement("; "),
   headVar.toText,
   SurroundSpaces(TextElement("::")),
   tailVar.toText,
   SurroundSpaces(Symbols.doubleRightArrow),
   consCase.toTextBracketed,
   TextElement(" }")
 )

 override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] = List(
   (list, env),
   (nilCase, env),
   (
     consCase,
     list.eval(env) match {
       case ConsV(head, tail) => consEnv(env, head, tail)
       case _ =>
         list.typeCheck(envToTypeEnv(env)) match {
           case ListType(elTyp) => consEnv(env, HiddenValue(elTyp), HiddenValue(ListType(elTyp)))
           case _               => env
         }
     }
   )
 )

 override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = List(
   (list, tEnv),
   (nilCase, tEnv),
   (
     consCase,
     list.typeCheck(tEnv) match {
       case ListType(elTyp) => consTEnv(tEnv, elTyp)
       case _               => tEnv
     }
   )
 )

 override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] = List(
   (list, env),
   list.eval(env) match {
     case ConsV(head, tail) => (consCase, consEnv(env, head, tail))
     case NilV(_)           => (nilCase, env)
     case _                 => (nilCase, env)
   }
 )
}

object CaseList extends ExprCompanion {
 override def create(args: BuilderArgs): Option[Expr] = args match {
   case List(list: Expr, nilCase: Expr, headVar: Literal, tailVar: Literal, consCase: Expr) =>
     Some(CaseList(list, nilCase, headVar, tailVar, consCase))
   case Nil => Some(CaseList(defaultExpr, defaultExpr, defaultLiteral, defaultLiteral, defaultExpr))
   case _   => None
 }

 override protected val aliases: List[String] = List("ListCase")
}
```
