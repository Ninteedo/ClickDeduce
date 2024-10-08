package languages

import convertors.*
import scalatags.Text
import scalatags.Text.all.*

import scala.collection.immutable.List

class LLam extends LLet {
  registerTerms("LLam", List(Apply, Lambda, Func, LambdaV, HiddenValue))

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

    override def toText: ConvertableText = MultiElement(e1.toTextBracketed, TextElement(" "), e2.toTextBracketed)
  }

  object Apply extends ExprCompanion {
    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(e1: Expr, e2: Expr) => Some(Apply(e1, e2))
      case Nil                      => Some(Apply(defaultExpr, defaultExpr))
      case _                        => None
    }
  }

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

    override def toText: ConvertableText = MultiElement(
      LambdaSymbol(),
      v.toText,
      SpaceAfter(MathElement.colon),
      TypeElement(typ.toTextBracketed),
      SpaceAfter(MathElement.period),
      e.toText
    )
  }

  object Lambda extends ExprCompanion {
    def apply(v: Variable, typ: Type, e: Expr): Lambda = new Lambda(Literal.fromString(v), typ, e)

    override def create(args: BuilderArgs): Option[Expr] = args match {
      case List(v: Literal, typ: Type, e: Expr) => Some(Lambda(v, typ, e))
      case Nil                                  => Some(Lambda(defaultLiteral, defaultType, defaultExpr))
      case _                                    => None
    }
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

    override val isError: Boolean = in.isError || out.isError

    override def typeCheck(tEnv: TypeEnv): Type = Func(in.typeCheck(tEnv), out.typeCheck(tEnv))

    override def toText: ConvertableText =
      MultiElement(in.toTextBracketed, SurroundSpaces(SingleRightArrow()), out.toTextBracketed)
  }

  object Func extends TypeCompanion {
    override def create(args: BuilderArgs): Option[Type] = args match {
      case List(in: Type, out: Type) => Some(Func(in, out))
      case Nil                       => Some(Func(defaultType, defaultType))
      case _                         => None
    }

    override val aliases: List[String] = List("Function")
  }

  case class ApplyToNonFunctionErrorType(wrongType: Type) extends TypeError {
    override val message: String = s"Cannot apply with left expression being ${wrongType.prettyPrint}"
  }

  case class IncompatibleTypeErrorType(typ1: Type, typ2: Type) extends TypeError {
    override val message: String = s"mismatched types for applying function (expected $typ1 but got $typ2)"
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

    override def toText: ConvertableText = MultiElement(
      LambdaSymbol(),
      ItalicsElement(TextElement(v)),
      SpaceAfter(MathElement.colon),
      TypeElement(properInputType.toTextBracketed),
      SpaceAfter(MathElement.period),
      e.toText
    )
  }

  object LambdaV extends ValueCompanion {
  }

  case class ApplyToNonFunctionError(value: Value) extends EvalError {
    override val message: String = s"Cannot apply with left expression being ${value.prettyPrint}"

    override val typ: Type = ApplyToNonFunctionErrorType(value.typ)
  }

  case class HiddenValue(override val typ: Type) extends Value {
    override def isPlaceholder: Boolean = true

    override val needsBrackets: Boolean = false

    override def toText: ConvertableText = MathElement("?")
  }

  object HiddenValue extends ValueCompanion {
  }

  // tasks
  setTasks(DefineAFunctionTask, IntToBoolFunctionTask, FunctionUsingFunctionAsInputTask)

  private object DefineAFunctionTask extends Task {
    override val name: String = "Define a Lambda function"
    override val description: String =
      "Define a Lambda function with a input variable name and type, and body expression. " +
        "The expression should successfully type-check."
    override val difficulty: Int = 2

    override def checkFulfilled(expr: Expr): Boolean = {
      !expr.typeCheck().isError && checkCondition(
        expr,
        cond = {
          case Lambda(_, _, _) => true
          case _               => false
        }
      )
    }
  }

  object IntToBoolFunctionTask extends Task {
    override val name: String = "Define a function that converts Int to Bool"
    override val description: String =
      "Define a Lambda function that takes an Int and returns a Bool. " +
        "The function should return true if the input is exactly 39, and false otherwise. " +
        "The expression should successfully type-check."
    override val difficulty: Int = 3

    override def checkFulfilled(expr: Expr): Boolean = {
      !expr.typeCheck().isError && checkCondition(
        expr,
        { (e, env) =>
          e match {
            case lambda: Lambda =>
              lambda.typ match {
                case IntType() =>
                  Apply(lambda, Num(39)).eval(env) == BoolV(true) &&
                  List(-39, -100, 0, 1, 38, 40, 100).forall(i =>
                    Apply(lambda, Num(LiteralInt(i))).eval(env) == BoolV(false)
                  )
                case _ => false
              }
            case _ => false
          }
        },
        ValueEnv.empty
      )
    }
  }

  object FunctionUsingFunctionAsInputTask extends Task {
    override val name: String = "Define a function that uses another function as input"
    override val description: String =
      "Define a Lambda function that takes another Lambda function as input, then applies a value to that input " +
        "function. " +
        "The expression should successfully type-check."
    override val difficulty: Int = 4

    override def checkFulfilled(expr: Expr): Boolean = {
      !expr.typeCheck().isError && checkCondition(
        expr,
        { (e, env) =>
          e match {
            case Lambda(v1, typ, body) =>
              typ match {
                case Func(_, _) =>
                  checkCondition(
                    body,
                    { (e2, env2) =>
                      e2 match {
                        case Apply(e3, _) =>
                          checkCondition(
                            e3,
                            {
                              case Var(v2) => v2 == v1
                              case _       => false
                            },
                            env2
                          )
                        case _ => false
                      }
                    },
                    env
                  )
                case _ => false
              }
            case _ => false
          }
        },
        ValueEnv.empty
      )
    }
  }
}

object LLam extends LLam {}
