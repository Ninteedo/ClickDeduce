package languages

import languages.LWhile.*
import languages.env.{TypeEnv, ValueEnv}
import languages.terms.literals.LiteralIdentifierBind
import nodes.ExprNode
import org.scalatest.matchers.should.Matchers.*

class LWhileTest extends TestTemplate {
  property("While statement evaluates correctly") {
    val i = "i"
    val product = "product"
    val stmt = SeqStmt(
      SeqStmt(
        AssignStmt(LiteralIdentifierBind(i), Num(0)),
        AssignStmt(LiteralIdentifierBind(product), Num(1))
      ),
      WhileStmt(
        LessThan(Var(i), Num(5)),
        SeqStmt(
          AssignStmt(LiteralIdentifierBind(i), Plus(Var(i), Num(1))),
          AssignStmt(LiteralIdentifierBind(product), Times(Var(product), Var(i))),
        )
      )
    )
    stmt.eval() shouldEqual EnvValue(
      ValueEnv.empty + (i -> NumV(5)) + (product -> NumV(120))
    )
    stmt.typeCheck() shouldEqual EnvType(
      TypeEnv.empty + (i -> IntType()) + (product -> IntType())
    )
    ExprNode.fromExpr(LWhile, stmt).willDepthLimitBeExceeded() shouldBe false
  }
}
