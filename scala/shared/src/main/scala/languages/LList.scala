package languages

import convertors.*
import languages.env.*
import languages.terms.*
import languages.terms.builders.*
import languages.terms.errors.*
import languages.terms.exprs.Expr
import languages.terms.literals.*
import languages.terms.types.Type
import languages.terms.values.Value

class LList extends LPoly {
  registerTerms("LList", List(ListNil, Cons, CaseList, ListType, NilV, ConsV))

  // expressions
  case class ListNil(elTyp: Type) extends Expr {
    override protected def evalInner(env: ValueEnv): Value = NilV(elTyp)

    override protected def typeCheckInner(tEnv: TypeEnv): Type = ListType(elTyp)

    override def toText: ConvertableText = TextElement("Nil")

    override val needsBrackets: Boolean = false
  }

  object ListNil extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(elTyp: Type) => Some(ListNil(elTyp))
      case Nil               => Some(ListNil(defaultType))
      case _                 => None
    }

    override val aliases: List[String] = List("ListNil")
  }

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

    override val aliases: List[String] = List("ListCons", "::")
  }

  case class CaseList(
    list: Expr,
    nilCase: Expr,
    headVar: LiteralIdentifierBind,
    tailVar: LiteralIdentifierBind,
    consCase: Expr
  ) extends Expr {
    override protected def evalInner(env: ValueEnv): Value = list.eval(env) match {
      case NilV(_)           => nilCase.eval(env)
      case ConsV(head, tail) => consCase.eval(consEnv(env, head, tail))
      case v                 => ListCaseNotListError(v)
    }

    override protected def typeCheckInner(tEnv: TypeEnv): Type = list.typeCheck(tEnv) match {
      case ListType(elTyp) =>
        (nilCase.typeCheck(tEnv), consCase.typeCheck(consTEnv(tEnv, elTyp))) match {
          case (nilTyp, _) if nilTyp.isError   => nilTyp
          case (_, consTyp) if consTyp.isError => consTyp
          case (nilTyp, consTyp) =>
            if nilTyp.typeCheck(tEnv) != consTyp.typeCheck(tEnv)
            then ListTypeMismatchError(nilTyp, consTyp)
            else nilTyp.typeCheck(tEnv)
        }
      case t => ListCaseNotListTypeError(t)
    }

    private def consEnv(env: ValueEnv, head: Value, tail: Value): ValueEnv =
      env + (headVar -> head) + (tailVar -> tail)
    private def consTEnv(tEnv: TypeEnv, elTyp: Type): TypeEnv =
      tEnv + (headVar -> elTyp) + (tailVar -> ListType(elTyp))

    override def toText: ConvertableText = MultiElement(
      TextElement("case"),
      SpaceAfter(SubscriptElement(TextElement("list"))),
      list.toTextBracketed,
      TextElement(" of { "),
      TextElement("Nil"),
      SurroundSpaces(DoubleRightArrow()),
      nilCase.toTextBracketed,
      TextElement("; "),
      headVar.toText,
      SurroundSpaces(TextElement("::")),
      tailVar.toText,
      SurroundSpaces(DoubleRightArrow()),
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
            list.typeCheck(TypeEnv.fromValueEnv(env)) match {
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
      case List(
            list: Expr,
            nilCase: Expr,
            headVar: LiteralIdentifierBind,
            tailVar: LiteralIdentifierBind,
            consCase: Expr
          ) =>
        Some(CaseList(list, nilCase, headVar, tailVar, consCase))
      case Nil =>
        Some(
          CaseList(defaultExpr, defaultExpr, LiteralIdentifierBind.default, LiteralIdentifierBind.default, defaultExpr)
        )
      case _ => None
    }

    override val aliases: List[String] = List("ListCase")
  }

  // types
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

  // values
  trait ListValue extends Value {
    def elems: List[Value]
  }

  case class NilV(elTyp: Type) extends ListValue {
    override def toText: ConvertableText = TextElement("[]")

    override val typ: Type = ListType(elTyp)

    override val needsBrackets: Boolean = false

    override val isError: Boolean = elTyp.isError

    override val elems: List[Value] = List()
  }

  object NilV extends ValueCompanion {}

  case class ConsV(head: Value, tail: Value) extends ListValue {
    //    override def toText: ConvertableText = MultiElement(head.toTextBracketed, TextElement(" :: "), tail.toText)
    override def toText: ConvertableText = ListElement(elems.map(_.toText))

    override val typ: Type = ListType(head.typ)

    override val isError: Boolean = head.isError || tail.isError

    override def elems: List[Value] = tail match {
      case l: ListValue => head +: l.elems
      case _            => List(head)
    }
  }

  object ConsV extends ValueCompanion {}

  // exceptions
  case class ListTypeMismatchError(init: Type, tail: Type) extends TypeError {
    override val message: String = s"Expected matching list types, but got $init and $tail"
  }

  case class ListCaseNotListError(v: Value) extends EvalError {
    override val message: String = s"ListCase expected a list, but got $v"

    override val typ: Type = ListCaseNotListTypeError(v.typ)
  }

  case class ListCaseNotListTypeError(t: Type) extends TypeError {
    override val message: String = s"ListCase expected a list, but got $t"
  }
}

object LList extends LList {}
