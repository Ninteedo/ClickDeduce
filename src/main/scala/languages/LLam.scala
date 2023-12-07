package languages

import scalatags.Text
import scalatags.Text.TypedTag
import scalatags.Text.all.*

import scala.collection.immutable.List

class LLam extends LLet {
  // expressions
  case class Apply(e1: Expr, e2: Expr) extends Expr {
    override def getChildrenEval(env: Env = Map()): List[(Term, Env)] = (eval(e1, env), eval(e2, env)) match {
      case (v1: FunctionValue, v2) => List(
        (e1, env), (e2, env), (v1.getContainedFunction, env + (v1.getVarName -> v2))
      )
      case _ => List((e1, env), (e2, env))
    }
  }

  case class Lambda(v: Literal, typ: Type, e: Expr) extends Expr {
    override def getChildrenBase(env: Env): List[(Term, Env)] = List((v, env), (typ, env), (e, env + (v.toString -> PlaceholderValue(typ))))

    override def getChildrenEval(env: Env): List[(Term, Env)] = Nil

    override def getChildrenTypeCheck(tenv: TypeEnv): List[(Term, TypeEnv)] = List((e, tenv + (v.toString -> typ)))
  }

  object Lambda {
    def apply(v: Variable, typ: Type, e: Expr): Lambda = new Lambda(LiteralAny(v), typ, e)
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

    override lazy val valueText: TypedTag[String] = div(
      in.toHtml,
      raw(" → "),
      out.toHtml
    )
  }

  case class ApplyToNonFunctionErrorType(wrongType: Type) extends TypeError {
    override val message: String = s"Cannot apply with left expression being ${prettyPrint(wrongType)}"
  }

  case class IncompatibleTypeErrorType(typ1: Type, typ2: Type) extends TypeError {
    override val message: String = s"mismatched types for applying function (expected $typ1 but got $typ2)"
  }

  // values
  trait FunctionValue extends Value {
    def getContainedFunction: Expr

    def getVarName: Variable

    def evalApply(value: Value): Value
  }

  case class LambdaV(v: Variable, inputType: Type, e: Expr, env: Env) extends FunctionValue {
    override val typ: Type = Func(inputType, typeOf(e, envToTypeEnv(env) + (v -> inputType)))

    override def getVarName: Variable = v

    override def getContainedFunction: Expr = e

    override def evalApply(value: Value): Value = eval(e, env + (v -> value))

    override lazy val valueText: TypedTag[String] = {
      div(
        raw(s"λ$v. "),
        e.toHtml,
        raw(s" : "),
        typ.toHtml
      )
    }
  }

  case class ApplyToNonFunctionError(value: Value) extends EvalError {
    override val message: String = s"Cannot apply with left expression being ${prettyPrint(value)}"

    override val typ: Type = ApplyToNonFunctionErrorType(value.typ)
  }

  case class PlaceholderValue(override val typ: Type) extends Value {
    override def isPlaceholder: Boolean = true
  }

  override def eval(e: Expr, env: Env): Value = e match {
    case Lambda(LiteralAny(v), typ, e) => LambdaV(v, typ, e, env)
    case Apply(e1, e2) => eval(e1, env) match {
      case v1: FunctionValue => v1.evalApply(eval(e2, env))
      case v1 => ApplyToNonFunctionError(v1)
    }
    case _ => super.eval(e, env)
  }

  override def typeOf(e: Expr, env: TypeEnv): Type = e match {
    case Lambda(LiteralAny(v), typ, e) => Func(typ, typeOf(e, env + (v -> typ)))
    case Apply(e1, e2) => typeOf(e1, env) match {
      case t1: FunctionType => t1.typeOfApply(typeOf(e2, env))
      case t1 => ApplyToNonFunctionErrorType(t1)
    }
    case _ => super.typeOf(e, env)
  }

  override def prettyPrint(e: Expr): String = e match {
    case Lambda(v, typ, e) => s"λ$v:${prettyPrint(typ)}. ${prettyPrint(e)}"
    case Apply(e1, e2) => s"${prettyPrint(e1)} ${prettyPrint(e2)}"
    case _ => super.prettyPrint(e)
  }

  override def prettyPrint(v: Value): String = v match {
    case LambdaV(v, inputType, e, env) => {
      val eString: String = if (e == BlankExprDropDown()) "?" else prettyPrint(e)
      s"λ$v. $eString"
    }
    case PlaceholderValue(typ) => "?"
    case _ => super.prettyPrint(v)
  }

  override def prettyPrint(t: Type): String = t match {
    case Func(in, out) => s"${prettyPrint(in)} → ${prettyPrint(out)}"
    case ApplyToNonFunctionErrorType(typ) => s"CannotApplyError(${prettyPrint(typ)})"
    case IncompatibleTypeErrorType(typ1, typ2) => s"IncompatibleTypes(${prettyPrint(typ1)}, ${prettyPrint(typ2)})"
    case _ => super.prettyPrint(t)
  }

  override def calculateExprClassList: List[Class[Expr]] = {
    super.calculateExprClassList ++ List(classOf[Lambda], classOf[Apply]).map(_.asInstanceOf[Class[Expr]])
  }

  override def calculateTypeClassList: List[Class[Type]] = {
    super.calculateTypeClassList ++ List(classOf[Func]).map(_.asInstanceOf[Class[Type]])
  }
}

object LLam extends LLam {}
