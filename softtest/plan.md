# Test Plan

## Scala

The Scala code will be tested using ScalaTest.

- There will be a test file for each of the individual [languages](requirements.md#languages) (`LArith`, `LIf`, etc).
- There will be a single test file for all the [action](requirements.md#actions) classes.
- There will be a single test file for all the [node](requirements.md#node-tree) classes.
- There will be an additional test file for checking the correctness of the conversion from node tree to HTML.
- There will be a test file for the web server argument handling.
- There will be a test file for the web server request handling.

## TypeScript

The TypeScript code will be tested using Jest.

## Unit Tests

The project will make extensive use of unit tests to ensure that each function works as expected.

There will be unit tests
