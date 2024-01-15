package languages

/** Parent trait for all languages designed to be loaded in ClickDeduce.
  */
trait ClickDeduceLanguage extends AbstractActionLanguage {
  lang =>

  def createNewInstance(): ClickDeduceLanguage = {
    val constructor = lang.getClass.getConstructors()(0)
    constructor.newInstance().asInstanceOf[ClickDeduceLanguage]
  }
}
