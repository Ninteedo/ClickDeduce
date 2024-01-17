# Server

## Process

See [ArgsHandlingSpec](../src/test/scala/app/ArgsHandlingSpec.scala)

The main requirements of the server process are to correctly handle the command line arguments,
and to correctly start the server.

There should be a suite of tests for each command line argument, to ensure that the process sets the server up
correctly in each case.

## API

See [RouteSpec](../src/test/scala/app/RouteSpec.scala)

The server responses are coded using Akka's `Route` class, which means that the route can be tested directly.

There should be tests for each endpoint, as specified in the [API requirements](./requirements.md#api).

This means that tests can be conducted without needing to start the server, and without needing to send HTTP requests.

The API access and HTTP responses will also be separately tested using the webapp tests,
so these tests should focus on the server functionality, rather than the API.

## Actions

See [ActionSpec](../src/test/scala/languages/ActionSpec.scala)

There should be a suite of tests for each kind of action,
to ensure that the server responds correctly to each action request.

Each test should be grouped by the action it is testing, and should test each of the properties specified in the
[action requirements](./requirements.md#actions).

The tests should be conducted with multiple trees and tree paths, to ensure that the server responds correctly
in all cases.

# Webapp

## Mock

See [server_mock.test.ts](../webapp/scripts/test/server_mock.test.ts)

To ensure that the webapp is tested in isolation, it should be tested using mock server responses.

This will require replacing the JavaScript `fetch` function with a mock version, which returns the expected response.

This also means that the code for the webapp will need to use the `fetch` function for all requests.

Using the mocked responses will make it possible to write tests knowing exactly what the server will return 
and therefore what the webapp should do in response.

All main features of the webapp should be tested using this method.

## Server Integration

See [server_integration.test.ts](../webapp/scripts/test/server_integration.test.ts)

The webapp should also be tested with the server running, to ensure that the webapp and server work together correctly.

This tests that the webapp is able to send requests to the server and handle the responses correctly,
but also relies on the server returning the correct responses.

These tests should focus on cases where the webapp relies on the server's responses to function correctly.
Tests written with mock responses that do not rely on the server will not need to be included in this suite.

The server can be started using the `sbt run` command, so the test file should use a subprocess library to
start the server and then run the tests, closing the server once done.
This will also make it easier to run the tests in a CI environment, rather than managing multiple separate processes.

# Languages

Each language should have its own class in the [`languages`](../src/main/scala/languages) package and its own
corresponding [test suite](../src/test/scala/languages).

These tests should make sure that each language follows the specification
in the [language requirements](./requirements.md#languages).

This should include a separate test for each evaluation and type-checking rule,
to ensure that the property specified is fulfilled.

There should also be tests with larger expressions to ensure that the behaviour is correct for more complex programs.

The expression syntax is designed so that each rule can be tested in isolation, without needing to test the whole
language in every case. This is because a rule only depends on the values/types of its sub-expressions. It does not
depend on anything in the sub-expressions beyond that. It also should be independent of its parent expression(s), except
for the environment it is evaluated in.
