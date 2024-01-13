package languages

import scala.reflect.runtime.universe as ru

/**
 * Parent trait for all languages designed to be loaded in ClickDeduce.
 */
trait ClickDeduceLanguage extends AbstractActionLanguage {
  lang =>

  def createNewInstance(): ClickDeduceLanguage = {
    val constructor = lang.getClass.getConstructors()(0)
    constructor.newInstance().asInstanceOf[ClickDeduceLanguage]
  }

  def getSubclassesOf(parentClass: Class[_]): List[ru.ClassSymbol] = {
    val runtimeMirror = ru.runtimeMirror(getClass.getClassLoader)
    val classSymbol = runtimeMirror.classSymbol(parentClass)
    val subclasses = classSymbol.knownDirectSubclasses
    subclasses.map(_.asClass).toList
  }

  def createTerm(name: String, args: List[Term]): Term = {
    val exprClass = exprNameToClass(name)
    exprClass match {
      case Some(value) => {
        val constructor = value.getConstructors()(0)
        constructor.newInstance(args: _*).asInstanceOf[Expr]
      }
      case None => {
        val blankClass = blankClassList.find(_.getSimpleName == name)
        blankClass match {
          case Some(value) => MissingExpr()
          case None => MissingExpr()
        }
      }
    }
  }

  def createUnfilledExpr(name: String): Expr = {
    val exprClass = exprNameToClass(name)
    exprClass match {
      case Some(value) => {
        val constructor = value.getConstructors()(0)
        val arguments = constructor.getParameterTypes.map {
          case c if classOf[ClickDeduceLanguage].isAssignableFrom(c) => lang
          case c if classOf[Expr].isAssignableFrom(c) => BlankChildPlaceholder()
          case _ => BlankLiteral()
        }
        constructor.newInstance(arguments: _*).asInstanceOf[Expr]
      }
      case None => {
        val blankClass = blankClassList.find(_.getSimpleName == name)
        blankClass match {
          case Some(value) => MissingExpr()
          case None => MissingExpr()
        }
      }
    }
  }
}
