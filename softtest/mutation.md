# Mutation Testing

This project uses [Stryker](https://stryker-mutator.io/) for mutation testing.

It automatically creates mutants by editing parts of the code such as operators, conditions, and literals.

Conveniently, it supports both JavaScript and Scala, so it can be used for both the frontend and backend of the project.

## Reports

The mutation reports are available in [`softtest/mutation`](./mutation).

- [Jest mutation testing report](./mutation/jest/mutation.html)
- [Scala mutation testing report](./mutation/scala/index.html)

## Analysis

### Scala

The Scala mutation testing report shows that out of 541 mutants, 158 survived, giving a mutation score of `66%`.

This score is not particularly high.
However, upon inspecting the mutants, a large portion of them are replacing string literals in places that are
unimportant for the functionality of the program.

With string literal mutants excluded, there are 199 mutants, of which 42 survived, giving a mutation
score of `79%`.
This means that nearly half of the original mutants were string literal replacements.

Upon further inspection, I do not believe that mutation testing is a good fit for the languages part of this project.

For example, the `LArith` language has no mutants that are not string literal replacements, while almost the entirety
of its behaviour is independent of string literals.

Throughout the Scala code, most of the string literals are used for error messages, which are not important for the
functionality of the program.

The main benefit of mutation testing is that I could find test cases which don't properly catch improper behaviour.
Given more time, I would aim to use this to improve the test suite, adding additional assertions where surviving mutants
are found.

### Jest

The Jest mutation testing report shows that out of 407 mutants, 100 were undetected, giving a mutation
score of `75%`.

I think mutation testing is more useful here than in the Scala code.
It identified multiple conditions that were not properly covered by the test suite.

It also exposed that the adjustments made to the DOM in the `treeManipulation` module were not properly tested.

Some surviving mutants were unimportant, such as replacing the initial value of a global variable which gets
reset by the `initialise` function.

Given more time, I would use the results of the mutation testing to improve the test suite, adding additional assertions
where surviving mutants are found.
