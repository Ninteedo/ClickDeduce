package languages

import convertors.*
import scalatags.Text.all.*

class LPoly extends LData {
  registerTerms("LPoly", List(Poly, ApplyType, TypeVar, PolyType, PolyV))

  // expressions

  case class Poly(v: LiteralIdentifier, e: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = PolyV(TypeVar(v), e, env)

    override def typeCheckInner(tEnv: TypeEnv): Type =
      PolyType(TypeVar(v), e.typeCheck(tEnv + (v -> TypeContainer(TypeVar(v)))))

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] =
      List((v, env), (e, env + (v -> TypeValueContainer(TypeVar(v)))))

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] =
      List((v, tEnv), (e, tEnv + (v -> TypeContainer(TypeVar(v)))))

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] =
      List((e, env + (v -> TypeValueContainer(TypeVar(v)))))

    override def toText: ConvertableText =
      MultiElement(LambdaSymbol(capital = true), v.toText, MathElement.period, e.toTextBracketed)
  }

  object Poly extends ExprCompanion {
    def apply(v: Variable, e: Expr): Poly = Poly(LiteralIdentifier(v), e)

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(v: LiteralIdentifier, e: Expr) => Some(Poly(v, e))
      case Nil                                 => Some(Poly(LiteralIdentifier.default, defaultExpr))
      case _                                   => None
    }

    override val aliases: List[String] = List("Polymorphic", "PolyType")
  }

  case class ApplyType(e: Expr, typ: Type) extends Expr {
    override def evalInner(env: ValueEnv): Value = e.eval(env) match {
      case PolyV(tv, e, env) =>
        tv match {
          case TypeVar(v) => e.eval(env + (v -> TypeValueContainer(typ)))
          case other      => PolyVRequiresTypeVar(other)
        }
      case other => CannotApplyTypeUnlessPolyV(other)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = e.typeCheck(tEnv) match {
      case PolyType(tv, incompleteType) =>
        tv match {
          case TypeVar(v) => incompleteType.typeCheck(tEnv + (v -> typ))
          case other      => PolyVRequiresTypeVarType(other)
        }
      case other => CannotApplyTypeUnlessPolyType(other)
    }

    override def toText: ConvertableText =
      MultiElement(e.toTextBracketed, TextElement("["), typ.toText, TextElement("]"))
  }

  object ApplyType extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e: Expr, t: Type) => Some(ApplyType(e, t))
      case Nil                    => Some(ApplyType(defaultExpr, defaultType))
      case _                      => None
    }
  }

  // types

  case class TypeVar(v: LiteralIdentifier) extends Type {
    override def typeCheck(tEnv: TypeEnv): Type = guardValidIdentifierType(
      v,
      tEnv.get(v) match {
        case None              => UnknownTypeVar(v)
        case Some(TypeVar(t))  => TypeVar(t)
        case Some(other: Type) => other.typeCheck(tEnv)
      }
    )

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = v.toText
  }

  object TypeVar extends TypeCompanion {
    def apply(v: Variable): TypeVar = TypeVar(LiteralIdentifier(v))

    override def create(args: BuilderArgs): Option[Type] = args match {
      case List(v: LiteralIdentifier) => Some(TypeVar(v))
      case Nil              => Some(TypeVar(LiteralIdentifier.default))
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

    override val isError: Boolean = typeVar.isError || incompleteType.isError
  }

  object PolyType extends TypeCompanion {
    override def create(args: BuilderArgs): Option[Type] = args match {
      case List(tv: Type, t: Type) => Some(PolyType(tv, t))
      case Nil                     => Some(PolyType(defaultType, defaultType))
      case _                       => None
    }

    override protected val isHidden: Boolean = true
  }

  // values

  case class PolyV(typeVar: Type, e: Expr, env: ValueEnv) extends Value {
    override val typ: Type = typeVar match {
      case TypeVar(v) => PolyType(typeVar, e.typeCheck(envToTypeEnv(env) + (v -> TypeContainer(typeVar))))
      case other      => PolyVRequiresTypeVarType(other)
    }

    override def toText: ConvertableText = MultiElement(
      LambdaSymbol(capital = true),
      typeVar.toTextBracketed,
      SpaceAfter(MathElement.period),
      e.toTextBracketed
    )
  }

  object PolyV extends ValueCompanion {}

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

  // tasks
  setTasks(CreatePolyFunctionTask)

  object CreatePolyFunctionTask extends Task {
    override val name: String = "Create a Polymorphic Function"
    override val description: String =
      "Use the Poly expression to create a type variable which is then used as the input type for a lambda function." +
        " The function must use the input variable." +
        " It must successfully type-check."
    override val difficulty: Int = 3

    override def checkFulfilled(expr: Expr): Boolean = !expr.typeCheck().isError && checkCondition(
      expr,
      (e, env) =>
        e match {
          case Poly(typVar, e) =>
            checkCondition(
              e,
              (e, env) =>
                e match {
                  case Lambda(lamVar, lamTyp, e) =>
                    checkCondition(
                      lamTyp,
                      (t, env) =>
                        t match {
                          case TypeVar(v) => v == typVar
                          case _          => false
                        },
                      envToTypeEnv(env)
                    ) && checkCondition(
                      e,
                      (e, env) =>
                        e match {
                          case Var(v) => v == lamVar
                          case _      => false
                        },
                      env
                    )
                  case _ => false
                },
              env
            )
          case _ => false
        },
      ValueEnv.empty
    )
  }
}

object LPoly extends LPoly {}
