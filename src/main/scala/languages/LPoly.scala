package languages

import scalatags.Text.TypedTag
import scalatags.Text.all.*

class LPoly extends LData {
  // expressions

  case class Poly(v: Literal, e: Expr) extends Expr {
    override def evalInner(env: Env): Value = {
      PolyV(TypeVar(v), e, env)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = {
      PolyType(TypeVar(v), e.typeCheck(tEnv + (v.toString -> TypeVar(v))))
    }

    override def prettyPrint: String = s"Λ$v. ${e.prettyPrintBracketed}"

    override def getChildrenBase(env: Env): List[(Term, Env)] =
      List((v, env), (e, env + (v.toString -> TypeVarV(v, TypeVar(v)))))

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] =
      List((v, tEnv), (e, tEnv + (v.toString -> TypeVar(v))))

    override def getChildrenEval(env: Env): List[(Term, Env)] =
      List((e, env + (v.toString -> TypeVarV(v, TypeVar(v)))))
  }

  object Poly {
    def apply(v: Variable, e: Expr): Poly = Poly(Literal.fromString(v), e)
  }

  case class ApplyType(e: Expr, typ: Type) extends Expr {
    override def evalInner(env: Env): Value = e.eval(env) match {
      case PolyV(tv, e, env) =>
        tv match {
          case TypeVar(v) => e.eval(env + (v.toString -> TypeVarV(v, typ)))
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

    override def prettyPrint: String = s"${e.prettyPrintBracketed}[${typ.prettyPrint}]"
  }

  // types

  case class TypeVar(v: Literal) extends Type {
    override def prettyPrint: String = v.toString

    override def typeCheck(tEnv: TypeEnv): Type = tEnv.getOrElse(v.toString, UnknownTypeVar(v))

    override val needsBrackets: Boolean = false
  }

  object TypeVar {
    def apply(v: Variable): TypeVar = TypeVar(Literal.fromString(v))
  }

  case class PolyType(typeVar: Type, incompleteType: Type) extends Type {
    override def prettyPrint: String = s"Λ${typeVar.prettyPrintBracketed}. ${incompleteType.prettyPrintBracketed}"

    override lazy val valueText: TypedTag[String] = div(
      raw(
        PolyType(TypePlaceholder(typeVar), TypePlaceholder(incompleteType)).prettyPrint
      )
    )
  }

  // values

  case class PolyV(typeVar: Type, e: Expr, env: Env) extends Value {
    override val typ: Type = PolyType(typeVar, e.typeCheck(envToTypeEnv(env) + (typeVar.toString -> typeVar)))

    override def prettyPrint: String = s"Λ${typeVar.prettyPrintBracketed}. ${e.prettyPrintBracketed}"

    override lazy val valueText: TypedTag[String] = div(
      raw(PolyV(TypePlaceholder(typeVar), ExprPlaceholder(e), env).prettyPrint)
    )
  }

  case class TypeVarV(v: Literal, t: Type) extends Value {
    override val typ: Type = t

    override def prettyPrint: String = s"${v.toString}[${t.prettyPrint}]"

    override val needsBrackets: Boolean = false
  }

  object TypeVarV {
    def apply(v: Variable, t: Type): TypeVarV = TypeVarV(Literal.fromString(v), t)
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

  override def calculateExprClassList: List[Class[Expr]] =
    super.calculateExprClassList ++ List(classOf[Poly], classOf[ApplyType]).map(_.asInstanceOf[Class[Expr]])

  override def calculateTypeClassList: List[Class[Type]] =
    super.calculateTypeClassList ++ List(classOf[PolyType], classOf[TypeVar]).map(_.asInstanceOf[Class[Type]])
}

object LPoly extends LPoly {}
