package languages

import scalatags.Text
import scalatags.Text.TypedTag
import scalatags.Text.all.*

import scala.collection.immutable.List

class LLam extends LLet {
  // expressions
  case class Apply(e1: Expr, e2: Expr) extends Expr {
    override def evalInner(env: Env): Value = e1.eval(env) match {
      case v1: FunctionValue => v1.evalApply(e2.eval(env))
      case v1                => ApplyToNonFunctionError(v1)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = e1.typeCheck(tEnv) match {
      case t1: FunctionType => t1.typeOfApply(e2.typeCheck(tEnv))
      case t1               => ApplyToNonFunctionErrorType(t1)
    }

    override def getChildrenEval(env: Env = Map()): List[(Term, Env)] = (e1.eval(env), e2.eval(env)) match {
      case (v1: FunctionValue, v2) => List((e1, env), (e2, env), v1.getFunctionEvaluation(v2))
      case _                       => List((e1, env), (e2, env))
    }

    override def prettyPrint: String = s"((${e1.prettyPrint}) ${e2.prettyPrint})"
  }

  case class Lambda(v: Literal, typ: Type, e: Expr) extends Expr {
    override def evalInner(env: Env): Value = v match {
      case LiteralIdentifier(identifier) => LambdaV(identifier, typ, e, env)
      case _                             => InvalidIdentifierEvalError(v)
    }

    override def typeCheckInner(tEnv: TypeEnv): Type = v match {
      case LiteralIdentifier(identifier) => Func(typ, e.typeCheck(tEnv + (identifier -> typ)))
      case _                             => InvalidIdentifierTypeError(v)
    }

    override def getChildrenBase(env: Env): List[(Term, Env)] =
      List((v, env), (typ, env), (e, env + (v.toString -> HiddenValue(typ))))

    override def getChildrenEval(env: Env): List[(Term, Env)] = Nil

    override def getChildrenTypeCheck(tEnv: TypeEnv): List[(Term, TypeEnv)] = List((e, tEnv + (v.toString -> typ)))

    override def prettyPrint: String = s"λ$v: ${typ.prettyPrint}. ${e.prettyPrint}"
  }

  object Lambda {
    def apply(v: Variable, typ: Type, e: Expr): Lambda = new Lambda(Literal.fromString(v), typ, e)
  }

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

    override lazy val valueText: TypedTag[String] = div(in.toHtml, raw(" → "), out.toHtml)

    override def prettyPrint: String = s"(${in.prettyPrint} → ${out.prettyPrint})"
  }

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
    def getFunctionEvaluation(applyValue: Value): (Expr, Env)

    def evalApply(value: Value): Value
  }

  case class LambdaV(v: Variable, inputType: Type, e: Expr, env: Env) extends FunctionValue {
    override val typ: Type = Func(inputType, e.typeCheck(envToTypeEnv(env) + (v -> inputType)))

    override def getFunctionEvaluation(applyValue: Value): (Expr, Env) = (e, env + (v -> applyValue))

    override def evalApply(value: Value): Value = e.eval(env + (v -> value))

    override lazy val valueText: TypedTag[String] = div(
      raw(LambdaV(v, TypePlaceholder(inputType.toHtml.toString), ExprPlaceholder(e.toHtml.toString), env).prettyPrint)
    )

    override def prettyPrint: String = {
      val eString: String = if (e == BlankExprDropDown()) "?" else e.prettyPrint
      s"λ$v: ${inputType.prettyPrint}. $eString"
    }
  }

  case class ApplyToNonFunctionError(value: Value) extends EvalError {
    override val message: String = s"Cannot apply with left expression being ${value.prettyPrint}"

    override val typ: Type = ApplyToNonFunctionErrorType(value.typ)
  }

  case class HiddenValue(override val typ: Type) extends Value {
    override def isPlaceholder: Boolean = true

    override def prettyPrint: String = "?"
  }

  override def calculateExprClassList: List[Class[Expr]] = {
    super.calculateExprClassList ++ List(classOf[Lambda], classOf[Apply]).map(_.asInstanceOf[Class[Expr]])
  }

  override def calculateTypeClassList: List[Class[Type]] = {
    super.calculateTypeClassList ++ List(classOf[Func]).map(_.asInstanceOf[Class[Type]])
  }
}

object LLam extends LLam {}
