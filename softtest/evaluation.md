# Evaluation

## Coverage

A coverage report for the project can be found [here](coverage.md).

I believe that `95%` line and method coverage is a good aim for this project; the frontend meets this goal, but the
backend does not.
At this level of coverage, almost all of the code is covered, and the `5%` remaining would be either difficult to test,
or unreachable code.

I do not believe that `100%` coverage for the entire project is a good goal, as it would encourage removing currently
unreachable code that may be useful in the future.
It would also encourage rewriting code in pursuit of an arbitrary metric, which would be detrimental to the
maintainability of the code.

As mentioned in the coverage report, there were difficulties with computing the branch coverage for the Scala code.
If it could be computed accurately, I would aim for `90%` branch coverage.
I believe high branch coverage would be more difficult to achieve than high line and method coverage, as it would
require more complex tests, and would be more likely to require unreachable code to be removed.
This may be possible to achieve with other tools, but I did not have time to investigate this.

The Scala backend has an overall coverage of `89%` for methods and `88%` for lines, which is a good level of coverage.
This does mean that `11%` of methods and lines are not covered, but as discussed in the coverage report,
this is not terrible.
The branch coverage is `33%`, which is not a good level of coverage, but as discussed in the coverage report,
it is unclear how accurate this is.

The TypeScript front end has an overall coverage of `96%` for statements, `92%` for branches, `95%` for functions, and
`95%` for lines. This is an exceptional level of coverage, and I am satisfied with this.

The most important aspect is that the defined languages accurately follow the specification,
since they are the purpose of the project, and with a mathematical basis, any issues would be highly visible.
All the defined languages have `100%` coverage for methods and lines in the test suite.

I do not believe `100%` coverage for the languages is unreasonable, as they are relatively simple, and the
specification is well-defined.

The main ways to improve the coverage would be to (and the reasons why these weren't done were):
1. Create test cases for the server thread, which is hard due to its asynchronous nature.
2. Create test cases for the functionality of PanZoom, which is a low priority since it is a third-party library. 
3. Create test cases for the `zoomToFit` function in the frontend, which is difficult since depends on its
   interaction with PanZoom, and the fact that it a purely visual function.
4. Create test cases for ensuring that the text in error messages is correct, which are not defined in the
   specification, and are not important for the functionality of the program.
5. Remove some unreachable error handling code, which was left in for ease of future development and completeness.
   I would argue, however, that doing this in pursuit of a higher coverage score would be detrimental to the
   maintainability of the code.

## Mutations

A mutation testing report for the project can be found [here](mutation.md).

I believe that setting a mutation score goal is not particularly useful, as it is hard to predict how many mutants
will be created, and how many of these have effects that compromise the functionality of the program.

If `25%` of the mutants just replace error messages, then a target of `90%` would be unrealistic.
If all of the mutants are hand-crafted to break the functionality, then a target of `90%` would be too low.

Since the mutation testing was automated and done by a tool I was unfamiliar with, I did not have a good idea of
what a good mutation score would be.
I also think that a lot of the mutants generated were not useful, particularly in the Scala code.

I set a target of `80%` for the mutation score, though I am unsure how useful this is.

There were practically no mutants that affected the evaluation and type-checking behaviour of the languages,
which is the most important part of the project.

Therefore, I do not believe an evaluation of the correctness of the project can be made based on the mutation score.

Mutation testing was not particularly useful for the Scala code for the languages.
This is because the automated tools for mutation testing did not manage to create mutants
of the evaluation and type-checking code, due to these being more complex than an automated tool can handle.

## Requirements

An assessment of which requirements have associated tests can be found [here](./requirements_to_tests.md).

I set a target of `90%` of requirement having tests directly associated with them.
This is because I believed that it is important to cover every requirement, but I understand that some requirements
may be difficult to test directly.

In total, out of 207 requirements, 27 are not tested.
This means that `87%` of requirements are tested.

This does not quite meet the target, but I believe it is a good level of coverage 
(considering that a lot of the uncovered requirements are UI-related, and are hard to test automatically).

## Limitations

I did not have time to set up a process for tests running in a real browser or by a real user.
Since most of the frontend code is to do with the UI, this means that the tests would struggle
to identify issues with the UI that are not apparent from the DOM.

Due to time limitations, there is no process for testing the security of the website.

The tests that exist for evaluating the performance of the website are not thorough.
In particular, there are no tests for how the server performs under load.

The Scala code for running the server is not directly tested, only indirectly through the system tests
for the frontend.

## Improvements

I would like to have a process for testing the website in a real browser environment with a real user.
This would allow for more accurate testing of the UI, though it would be significantly more time-consuming
than automated testing.

Security testing and performance testing would be useful, but are not a priority for this project.

I would like to implement more advanced mutation testing for the Scala code.
This would possibly involve hand-crafting mutants with a better understanding of the code,
rather than relying on an automated tool which is not able to create useful mutants.

A higher coverage of requirements could be achieved by introducing user testing for the interface.
