package languages

import convertors.*
import scalatags.Text
import scalatags.Text.all.*

import scala.collection.immutable.List

class LLam extends LLet {
  // expressions
  case class Apply(e1: Expr, e2: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = e1.eval(env) match {
      case v1: FunctionValue => v1.evalApply(e2.eval(env))
      case v1                => ApplyToNonFunctionError(v1)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = e1.typeCheck(tEnv) match {
      case t1: FunctionType => t1.typeOfApply(e2.typeCheck(tEnv))
      case t1               => ApplyToNonFunctionErrorType(t1)
    }

    override def getChildrenEval(env: ValueEnv = ValueEnv.empty): List[(Term, ValueEnv)] =
      (e1.eval(env), e2.eval(env)) match {
        case (v1: FunctionValue, v2) => List((e1, env), (e2, env), v1.getFunctionEvaluation(v2))
        case _                       => List((e1, env), (e2, env))
      }

    override def prettyPrint: String = s"${e1.prettyPrintBracketed} ${e2.prettyPrintBracketed}"

    override def toText: ConvertableText = MultiElement(SpaceAfter(e1.toTextBracketed), e2.toTextBracketed)
  }

  addExprBuilder(
    "Apply",
    {
      case List(e1: Expr, e2: Expr) => Some(Apply(e1, e2))
      case Nil                      => Some(Apply(defaultExpr, defaultExpr))
      case _                        => None
    }
  )

  case class Lambda(v: Literal, typ: Type, e: Expr) extends Expr {
    override def evalInner(env: ValueEnv): Value = v match {
      case LiteralIdentifier(identifier) => LambdaV(identifier, typ.typeCheck(envToTypeEnv(env)), e, env)
      case _                             => InvalidIdentifierEvalError(v)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = v match {
      case LiteralIdentifier(identifier) =>
        val inputType = typ.typeCheck(tEnv)
        Func(inputType, e.typeCheck(tEnv + (identifier -> inputType)))
      case _ => InvalidIdentifierTypeError(v)
    }

    override def getChildrenBase(env: ValueEnv): List[(Term, ValueEnv)] =
      List((v, env), (typ, env), (e, env + (v.toString -> HiddenValue(typ))))

    override def getChildrenEval(env: ValueEnv): List[(Term, ValueEnv)] = Nil

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = List((e, tEnv + (v.toString -> typ)))

    override def prettyPrint: String = s"λ$v: ${typ.prettyPrintBracketed}. ${e.prettyPrint}"

    override def toText: ConvertableText = MultiElement(
      LambdaSymbol(),
      v.toText,
      SpaceAfter(MathElement.colon),
      typ.toTextBracketed,
      SpaceAfter(MathElement.period),
      e.toText
    )
  }

  object Lambda {
    def apply(v: Variable, typ: Type, e: Expr): Lambda = new Lambda(Literal.fromString(v), typ, e)
  }

  addExprBuilder(
    "Lambda",
    {
      case List(v: Literal, typ: Type, e: Expr) => Some(Lambda(v, typ, e))
      case Nil                                  => Some(Lambda(defaultLiteral, defaultType, defaultExpr))
      case _                                    => None
    }
  )

  // types
  trait FunctionType extends Type {
    def typeOfApply(argType: Type): Type
  }

  case class Func(in: Type, out: Type) extends FunctionType {
    override def typeOfApply(argType: Type): Type = if (argType == in) {
      out
    } else {
      IncompatibleTypeErrorType(in, argType)
    }

    override def typeCheck(tEnv: TypeEnv): Type = Func(in.typeCheck(tEnv), out.typeCheck(tEnv))

    override def prettyPrint: String = s"${in.prettyPrintBracketed} → ${out.prettyPrintBracketed}"

    override def toText: ConvertableText =
      MultiElement(in.toTextBracketed, SurroundSpaces(SingleRightArrow()), out.toTextBracketed)
  }

  addTypeBuilder(
    "Func",
    {
      case List(in: Type, out: Type) => Some(Func(in, out))
      case Nil                       => Some(Func(defaultType, defaultType))
      case _                         => None
    }
  )

  case class ApplyToNonFunctionErrorType(wrongType: Type) extends TypeError {
    override val message: String = s"Cannot apply with left expression being ${wrongType.prettyPrint}"

    override def prettyPrint: String = s"CannotApplyError(${wrongType.prettyPrint})"
  }

  case class IncompatibleTypeErrorType(typ1: Type, typ2: Type) extends TypeError {
    override val message: String = s"mismatched types for applying function (expected $typ1 but got $typ2)"

    override def prettyPrint: String = s"IncompatibleTypes(${typ1.prettyPrint}, ${typ2.prettyPrint})"
  }

  // values
  trait FunctionValue extends Value {
    def getFunctionEvaluation(applyValue: Value): (Expr, ValueEnv)

    def evalApply(value: Value): Value
  }

  case class LambdaV(v: Variable, inputType: Type, e: Expr, env: ValueEnv) extends FunctionValue {
    private val properInputType: Type = inputType.typeCheck(envToTypeEnv(env))

    override val typ: Type = Func(inputType, e.typeCheck(envToTypeEnv(env) + (v -> properInputType)))

    override def getFunctionEvaluation(applyValue: Value): (Expr, ValueEnv) = (e, env + (v -> applyValue))

    override def evalApply(value: Value): Value = e.eval(env + (v -> value))

    override def prettyPrint: String = {
      val eString: String = if (e == BlankExprDropDown()) "?" else e.prettyPrint
      s"λ$v: ${properInputType.prettyPrintBracketed}. $eString"
    }

    override def toText: ConvertableText = MultiElement(
      LambdaSymbol(),
      TextElement(v),
      SpaceAfter(MathElement.colon),
      properInputType.toText,
      SpaceAfter(MathElement.period),
      e.toText
    )
  }

  addValueBuilder(
    "LambdaV",
    {
      case List(v: Variable, inputType: Type, e: Expr, env: ValueEnv) => Some(LambdaV(v, inputType, e, env))
      case _                                                          => None
    }
  )

  case class ApplyToNonFunctionError(value: Value) extends EvalError {
    override val message: String = s"Cannot apply with left expression being ${value.prettyPrint}"

    override val typ: Type = ApplyToNonFunctionErrorType(value.typ)
  }

  case class HiddenValue(override val typ: Type) extends Value {
    override def isPlaceholder: Boolean = true

    override def prettyPrint: String = "?"

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = MathElement("?")
  }

  addValueBuilder(
    "HiddenValue",
    {
      case List(typ: Type) => Some(HiddenValue(typ))
      case _               => None
    }
  )
}

object LLam extends LLam {}
