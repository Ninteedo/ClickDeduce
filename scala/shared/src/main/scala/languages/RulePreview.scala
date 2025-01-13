package languages

import convertors.*
import languages.terms.types.Type
import scalatags.Text.TypedTag
import scalatags.Text.all.*

trait InferenceRulePart {
  def toText: ConvertableText
}

case class EvalRulePart(t: ConvertableText) extends InferenceRulePart {
  override def toText: ConvertableText = t
}

object EvalRulePart {
  def apply(l: ConvertableText, r: ConvertableText): EvalRulePart = EvalRulePart(MultiElement(l, Symbols.doubleDownArrow.spacesAround, r))

  def eToV(n: Int): EvalRulePart = EvalRulePart(TermCommons.e(n), TermCommons.v(n))

  def reflexive(t: ConvertableText): EvalRulePart = EvalRulePart(t, t)
}

case class EvaluationRulePartEnv(l: ConvertableText, r: ConvertableText, lookups: List[ConvertableText]) extends InferenceRulePart {
  override def toText: ConvertableText = MultiElement(ListElement(lookups, start = Symbols.sigma, end = NullElement()))
}

case class EvalSubst(l: ConvertableText, r: ConvertableText) extends InferenceRulePart {
  override def toText: ConvertableText = SquareBracketedElement(MultiElement(l, Symbols.forwardSlash, r))
}

case class EvalMultiSubst(pairs: EvalSubst*) extends InferenceRulePart {
  override def toText: ConvertableText = SquareBracketedElement(
    ListElement(pairs.map(p => MultiElement(p.l, Symbols.forwardSlash, p.r)), start = NullElement(), end = NullElement())
  )
}

case class TypeCheckRulePart(t: ConvertableText) extends InferenceRulePart {
  override def toText: ConvertableText = t
}

object TypeCheckRulePart {
  def apply(l: ConvertableText, r: Type): TypeCheckRulePart = TypeCheckRulePart(l, r.toText)

  def apply(l: ConvertableText, r: ConvertableText, binds: List[ConvertableText] = Nil): TypeCheckRulePart =
    TypeCheckRulePart(MultiElement(
      Symbols.gamma,
      if binds.isEmpty then NullElement() else MultiElement(MathElement.comma.spaceAfter, ListElement(binds, start = NullElement(), end = NullElement())),
      Symbols.turnstile.spacesAround,
      l,
      TextElement(":").spaceAfter,
      r
    ))

  def eTo(n: Int, t: Type): TypeCheckRulePart = TypeCheckRulePart(TermCommons.e(n), t)

  def eTo(n: Int, t: ConvertableText): TypeCheckRulePart = TypeCheckRulePart(TermCommons.e(n), t)
}

case class TypeCheckRuleBind(l: ConvertableText, r: ConvertableText) extends InferenceRulePart {
  override def toText: ConvertableText = MultiElement(l, MathElement(":"), r)
}

abstract class InferenceRulePreview {
  val assumptions: Seq[InferenceRulePart]
  val conclusion: InferenceRulePart

  protected val divClass: String

  def toHtml: TypedTag[String] = div(
    cls := divClass,
    div(
      cls := ClassDict.RULE_PREVIEW_ASSUMPTIONS,
      assumptions.map(a => a.toText.asHtml)
    ),
    div(
      cls := ClassDict.RULE_PREVIEW_CONCLUSION,
      conclusion.toText.asHtml
    )
  )
}

case class EvalRulePreview(conclusion: InferenceRulePart, assumptions: InferenceRulePart*)
  extends InferenceRulePreview {
  override protected val divClass: String = ClassDict.RULE_EVAL
}

case class TypeCheckRulePreview(conclusion: InferenceRulePart, assumptions: InferenceRulePart*)
  extends InferenceRulePreview {
  override protected val divClass: String = ClassDict.RULE_TYPE
}

case class RulePreview(typeCheckRule: Seq[TypeCheckRulePreview], evaluationRule: Seq[EvalRulePreview]) {
  def toHtml: TypedTag[String] = div(
    div(typeCheckRule.map(_.toHtml)),
    div(evaluationRule.map(_.toHtml))
  )
}

object RulePreview {
  def apply(typeCheckRule: TypeCheckRulePreview, evaluationRule: EvalRulePreview): RulePreview =
    RulePreview(List(typeCheckRule), List(evaluationRule))
}

class RulePreviewBuilder {
  private var typeCheckRules: List[TypeCheckRulePreview] = Nil
  private var evaluationRules: List[EvalRulePreview] = Nil

  def addTypeCheckRule(rule: TypeCheckRulePreview): RulePreviewBuilder = {
    typeCheckRules = typeCheckRules :+ rule
    this
  }

  def addEvaluationRule(rule: EvalRulePreview): RulePreviewBuilder = {
    evaluationRules = evaluationRules :+ rule
    this
  }

  def build: RulePreview = RulePreview(typeCheckRules, evaluationRules)
}

class EvalRuleBuilder {
  private var assumptions: List[InferenceRulePart] = Nil
  private var conclusion: Option[InferenceRulePart] = None

  def addAssumption(assumption: InferenceRulePart): EvalRuleBuilder = {
    assumptions = assumptions :+ assumption
    this
  }

  def setConclusion(conclusion: InferenceRulePart): EvalRuleBuilder = {
    this.conclusion = Some(conclusion)
    this
  }

  def build: EvalRulePreview = EvalRulePreview(conclusion.get, assumptions: _*)
}

class TypeCheckRuleBuilder {
  private var assumptions: List[InferenceRulePart] = Nil
  private var conclusion: Option[InferenceRulePart] = None

  def addAssumption(assumption: InferenceRulePart): TypeCheckRuleBuilder = {
    assumptions = assumptions :+ assumption
    this
  }

  def setConclusion(conclusion: InferenceRulePart): TypeCheckRuleBuilder = {
    this.conclusion = Some(conclusion)
    this
  }

  def build: TypeCheckRulePreview = TypeCheckRulePreview(conclusion.get, assumptions: _*)
}
