package languages

import languages.LList.*
import org.scalatest.matchers.should.Matchers.*

class LListTest extends TestTemplate {
  property("PolyFilterFunctionTask is checked correctly") {
    val polyFilterFunction = Poly("T",
      Lambda("f", Func(TypeVar("T"), BoolType()),
        Rec("filter", "xs", ListType(TypeVar("T")), ListType(TypeVar("T")),
          CaseList(
            Var("xs"),
            ListNil(TypeVar("T")),
            "y", "ys",
            IfThenElse(
              Apply(Var("f"), Var("y")),
              Cons(Var("y"), Apply(Var("filter"), Var("ys"))),
              Apply(Var("filter"), Var("ys"))
            )
          )
        )
      )
    )
    polyFilterFunction.typeCheck() shouldEqual PolyType(TypeVar("T"), Func(Func(TypeVar("T"), BoolType()), Func(ListType(TypeVar("T")), ListType(TypeVar("T")))))
    PolyFilterFunctionTask.checkFulfilled(polyFilterFunction) shouldEqual true
  }
}
