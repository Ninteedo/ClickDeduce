package languages

import convertors.*
import scalatags.Text.all.*

class LPoly extends LData {
  Poly.register()
  ApplyType.register()
  TypeVar.register()
  PolyType.register()
  PolyV.register()

  // expressions

  case class Poly(v: Literal, e: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = PolyV(TypeVar(v), e, env)

    override def typeCheckInner(tEnv: TypeEnv): Type =
      PolyType(TypeVar(v), e.typeCheck(tEnv + (v.toString -> TypeContainer(TypeVar(v)))))

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] =
      List((v, env), (e, env + (v.toString -> TypeValueContainer(TypeVar(v)))))

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] =
      List((v, tEnv), (e, tEnv + (v.toString -> TypeContainer(TypeVar(v)))))

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] =
      List((e, env + (v.toString -> TypeValueContainer(TypeVar(v)))))

    override def toText: ConvertableText =
      MultiElement(LambdaSymbol(capital = true), v.toText, MathElement.period, e.toTextBracketed)
  }

  object Poly extends ExprCompanion {
    def apply(v: Variable, e: Expr): Poly = Poly(Literal.fromString(v), e)

    override def createExpr(args: BuilderArgs): Option[Expr] = args match {
      case List(v: Literal, e: Expr) => Some(Poly(v, e))
      case Nil                       => Some(Poly(defaultLiteral, defaultExpr))
      case _                         => None
    }

    override val aliases: List[String] = List("Polymorphic", "PolyType")
  }

  case class ApplyType(e: Expr, typ: Type) extends Expr {
    override def evalInner(env: ValueEnv): Value = e.eval(env) match {
      case PolyV(tv, e, env) =>
        tv match {
          case TypeVar(v) => e.eval(env + (v.toString -> TypeValueContainer(typ)))
          case other      => PolyVRequiresTypeVar(other)
        }
      case other => CannotApplyTypeUnlessPolyV(other)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = e.typeCheck(tEnv) match {
      case PolyType(tv, incompleteType) =>
        tv match {
          case TypeVar(v) => incompleteType.typeCheck(tEnv + (v.toString -> typ))
          case other      => PolyVRequiresTypeVarType(other)
        }
      case other => CannotApplyTypeUnlessPolyType(other)
    }

    override def toText: ConvertableText =
      MultiElement(e.toTextBracketed, TextElement("["), typ.toText, TextElement("]"))
  }

  object ApplyType extends ExprCompanion {
    override def createExpr(args: BuilderArgs): Option[Expr] = args match {
      case List(e: Expr, t: Type) => Some(ApplyType(e, t))
      case Nil                    => Some(ApplyType(defaultExpr, defaultType))
      case _                      => None
    }
  }

  // types

  case class TypeVar(v: Literal) extends Type {
    override def typeCheck(tEnv: TypeEnv): Type = tEnv.get(v.toString) match {
      case None             => UnknownTypeVar(v)
      case Some(TypeVar(t)) => TypeVar(t)
      case Some(other)      => other.typeCheck(tEnv)
    }

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = v.toText
  }

  object TypeVar extends TypeCompanion {
    def apply(v: Variable): TypeVar = TypeVar(Literal.fromString(v))

    override def createType(args: List[Literal | Term]): Option[Type] = args match {
      case List(v: Literal) => Some(TypeVar(v))
      case Nil              => Some(TypeVar(defaultLiteral))
      case _                => None
    }
  }

  case class PolyType(typeVar: Type, incompleteType: Type) extends Type {
    override def toText: ConvertableText =
      MultiElement(
        ForAllSymbol(),
        typeVar.toTextBracketed,
        SpaceAfter(MathElement.period),
        incompleteType.toTextBracketed
      )
  }

  object PolyType extends TypeCompanion {
    override def createType(args: List[Literal | Term]): Option[Type] = args match {
      case List(tv: Type, t: Type) => Some(PolyType(tv, t))
      case Nil                     => Some(PolyType(defaultType, defaultType))
      case _                       => None
    }

    override protected val isHidden: Boolean = true
  }

  // values

  case class PolyV(typeVar: Type, e: Expr, env: ValueEnv) extends Value {
    override val typ: Type = typeVar match {
      case TypeVar(v) => PolyType(typeVar, e.typeCheck(envToTypeEnv(env) + (v.toString -> TypeContainer(typeVar))))
      case other      => PolyVRequiresTypeVarType(other)
    }

    override def toText: ConvertableText = MultiElement(
      LambdaSymbol(capital = true),
      typeVar.toTextBracketed,
      SpaceAfter(MathElement.period),
      e.toTextBracketed
    )
  }

  object PolyV extends ValueCompanion {
    override protected def createValue(args: List[Any]): Option[Value] = args match {
      case List(tv: Type, e: Expr, env: ValueEnv) => Some(PolyV(tv, e, env))
      case _                                      => None
    }
  }

  // errors

  case class CannotApplyTypeUnlessPolyV(v: Value) extends EvalError {
    override val message: String = s"Cannot apply type to non-polymorphic value $v"

    override val typ: Type = CannotApplyTypeUnlessPolyType(v.typ)
  }

  case class CannotApplyTypeUnlessPolyType(t: Type) extends TypeError {
    override val message: String = s"Cannot apply type to non-polymorphic type $t"
  }

  case class PolyVRequiresTypeVar(v: Type) extends EvalError {
    override val message: String = s"Polymorphic value requires a type variable, not $v"

    override val typ: Type = PolyVRequiresTypeVarType(v)
  }

  case class PolyVRequiresTypeVarType(t: Type) extends TypeError {
    override val message: String = s"Polymorphic value requires a type variable, not $t"
  }

  case class UnknownTypeVar(v: Literal) extends TypeError {
    override val message: String = s"Unknown type variable $v"
  }
}

object LPoly extends LPoly {}
