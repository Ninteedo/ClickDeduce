# Coverage

The automatically generated coverage reports appear in the `[coverage](../coverage)` directory, but this is not 
included in the repository.

There is a copy of the final coverage reports before submission in the [`softtest/coverage`](./coverage) directory,
which this document will refer to.

## Scala

### Coverage Tools

The Scala coverage report is generated using the IntelliJ IDEA coverage tool and can be viewed 
[here](./coverage/scala/index.html).

I had issues with multiple different coverage tools I tried, so I ended up using IntelliJ's built-in tool.
It was not perfect, but seemed to have less obvious issues than JaCoCo and Scoverage.

I believe the issues with these tools are due to the fact that they are primarily designed for Java, and
Scala's functional nature makes it difficult to determine which lines are actually executed.

There are instances where a line is marked as not covered, but a proceeding line is covered, which is
impossible in Scala.

[coverage issue example](./images/coverage_issue_example.png)

[coverage issue example 2](./images/scoverage_example.png)

[coverage issue example 3](./images/coverage_issue_example_3.png)

I also found that the branch coverage was inaccurate, as even a simple `if` statement was marked as hundreds of
branches, resulting in branch coverage under `30%` for the repository, which was not a useful measurement.

Understanding this, I believe that many of the coverage values are pessimistic, and that the actual coverage
is higher than reported.

I believe that the repository is well tested, and visually inspecting the coverage report shows that 
a lot of the uncovered lines should be covered in reality.

### App Package

The reported coverage for the `app` package is `85%` for methods and `75%` for classes.

The most important uncovered methods are `WebServer.main`, `WebServer.bundleScripts`, and `WebServer.runServer`.

It was not possible to test `WebServer.main` because it caused errors in the GitHub Actions workflow.
The unused test file is [ServerHostTest](../src/test/scala/app/ServerHostTest.scala.disabled).

#### Coverage During Jest Tests

However, while these methods are not covered during the Scala tests, they are executed during the 
Jest server integration tests. 

If this is considered sufficient, then the coverage is `100%` for methods, though
some lines for error handling are not covered. 

All but 4 lines are executed on the normal execution path, meaning that the line coverage is 
`169/173 (97%)`.

### Languages Package

The reported coverage for the `languages` package is `89%` for methods and `90%` for lines.

Each of the specific language classes (`LArith`, `LIf`, `LLet`, `LLam`, and `LRec`) have 
`100%` coverage for methods and lines. `LLet` is marked as having an uncovered line, but it clearly is covered 
(see [coverage tools](#coverage-tools) section).

The language trait classes (`AbstractLanguage`, `AbstractNodeLanguage`, and `AbstractActionLanguage`) have worse
coverage. I attribute this to two main factors:
1. These classes have a lot of error cases, the handling of some of which overlap, making others unreachable.
   For example, the `getParent` method of `InnerNode` has multiple error cases, but the `setParent` method
   will throw an error in the same cases, so the `getParent` error cases are unreachable.
2. There are some unused methods which have been left in the code for future use, for example the `indexOf` method
   of `OuterNode`.
3. There are some cases where a trait or abstract class has a method implementation which is overridden in every
   subclass, so it is unreachable. These could be removed, but are also left in for ease of future development.

#### Abstract Action Language

The reported coverage for the `languages/ActionLanguage` package is `80%` for methods and `87%` for lines.

## TypeScript

The TypeScript coverage report is generating using Jest with the `--coverage` flag and can be viewed
[here](./coverage/jest/lcov-report/index.html).
