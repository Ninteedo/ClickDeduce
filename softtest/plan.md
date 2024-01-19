# Test Plan

## Overview

Tests will be used throughout the development of the project to ensure that the code works as expected and matches
the requirements.

The IDE being used for development is IntelliJ IDEA, which has built-in support for running tests.
Its test interface highlights the tests which have failed, and includes debugging functionality.
It will be configured to run the tests automatically whenever a commit is made to the local git repository.

There will be two separate test tools used for the project, one for the Scala code and one for the TypeScript code.

Scala is a statically typed language, so the compiler will catch errors resulting from incorrect types.
This also applies to TypeScript, which is a statically typed version of JavaScript.

The Scala tests will feature a mix of unit tests and integration tests.

The TypeScript tests will feature a mix of unit tests, integration tests, and system tests.

## Scala

The Scala code will be tested using ScalaTest.

These will constitute a mixture of unit tests and integration tests.
A lot of the project will have highly interdependent code, so it will be difficult to fully separate some of the modules
from each other.

For example, the action classes fully depend on the implementation of the node classes, so it will not be possible to
test the action classes without also testing the node classes.

- There will be a test file for each of the individual [languages](requirements.md#languages) (`LArith`, `LIf`, etc.).
   - The languages will be developed in sequence, so the test files should only be created once the development of the
     language has started.
   - Each test file should ensure that each of the defined language constructs matches the requirements.
   - These should take the form of unit tests for each of the properties of each construct.
   - There should also be some integration tests which use a wider range of constructs to ensure that they work together
     as expected.
     These should also use constructs from earlier languages, but are not expected to use features from
     later languages.
- There will be a single test file for all the [node](requirements.md#node-tree) classes.
   - Each node class with specific requirements will have its own test section.
   - The most complex parts of the node classes are parsing them from strings and converting them to HTML.
      - The HTML conversion will be tested in a separate test file.
- There will be an additional test file for checking the correctness of the conversion from node tree to HTML.
   - Matching the exact HTML output as a string is not feasible.
      - Any changes to the HTML output structure would require each test to be manually updated.
   - Instead, the HTML output will be parsed using [jsoup](https://jsoup.org/) and the resulting DOM will be checked.
      - The project will use the jsoup implementation
        from [Scala Scraper](https://github.com/ruippeixotog/scala-scraper), but will not use the web scraping
        functionality.
      - This will allow the tests to check the structure of the HTML output without having to match the exact string.
      - The structure will primarily be checked by using CSS selectors to find elements in the DOM and then checking
        that they have the expected properties.
      - This is limited in that it will be challenging to check whether there are any extra elements in the DOM which
        should not be there.
        This is accepted as a limitation of the test plan.
- There will be a single test file for all the [action](requirements.md#actions) classes.
   - Each action should have a unit test for each of its requirements.
   - Since the action classes depend on the node classes, those will need to be implemented first.
   - There should also be tests for invalid actions, which should throw an exception.
- There will be a test file for the web server argument handling.
- There will be a test file for the web server request handling.

## TypeScript

The TypeScript code will be tested using Jest.

- There will be a test file for the actions handling
- There will be a test file for the interface methods
- There will be a test file for the node tree handling
- There will be a test file for the fetch mock behaviour
- There will be a test file for the web server integration

### Mock Testing

Most of the webpage's code depends on responses from the server, so it will be necessary to mock the server responses
to test the code for the unit tests and some of the integration tests.

Jest provides mocking functionality, so this will be used to mock the server responses.
A custom fetch mock function will be used which will allow the tests to specify the response for each request.
The tests will then be able to check that the code behaves as expected for each response.

### Unit Tests

Similarly to the Scala code, some of the TypeScript code will be highly interdependent, so it will be difficult to
fully separate some modules from each other.

Generally, the behaviour of the action methods is dependent on the functionality of the tree manipulation methods.

### Integration Tests

The integration tests here have two different focuses.
The first is to test that the methods which are dependent on each other work together as expected.
The second is to test that the server and client work together as expected.

### System Tests

Since the website is primarily supposed to be interactive, there will be a number of system tests where
the test code will simulate user interaction with the website.

Jest normally runs in a Node.js environment which does not have a DOM, meaning it would not be possible to
test the website in this way.
However, [jsdom](https://github.com/jsdom/jsdom) can be used to simulate a DOM in Node.js, allowing the tests to
be run in a headless environment.

These tests will run while the server is running, and will use the server's API to interact with the website.
This means that these tests will closely resemble the real-world usage of the website, other than the fact that
the tests will be run in a headless browser.

- Frontend system tests will be included in server integration tests.
- These will consist of a number of tests which simulate user interaction with the website.
   - This will consist of events such as clicking buttons, entering text, keyboard events, and mouse events.
   - The tests will check that the state of the page is as expected after each event, or a series of events.
- The tests will be run in a headless browser using jsdom.
- The tests will be run using Jest. 

### Performance Tests

It is important that the website is responsive and does not have any noticeable lag when the user interacts with it.

I anticipate the main performance issue will be the time taken for the server to parse a request, evaluate it, and
create an HTML representation of the result.

To test this, I will create a number of tests which will send a request to the server and measure the time taken for
the server to respond.

There will also be a number of tests which will send a number of requests to the server in quick succession and
measure the time taken for the server to respond to all of them.

I am not prioritising this, as I believe worrying about DDoS attacks is unnecessary for this project.

### Security Tests

While security is important, it is not a priority for this project.
If I have time, I will look into security testing, but I do not expect to have time to do this.

This is an academic project, so I do not expect it to be used in a production environment.
I am also considering reducing the role of the server in the future, so extensive security testing may be wasted
effort.

## Limitations

- There are no system tests written for the Scala code.
This is because the Scala code is primarily used for the server, which is not intended to be used interactively.

- The tests for the HTML conversion will not be able to check for extra elements in the DOM which should not be there.

- There is a limit to how independent the tests for some parts of the Scala and TypeScript code can be.
  This means that some tests will be more akin to integration tests than unit tests.

- The unit test and some of the integration tests for the TypeScript code will have to accurately mock server responses.

- The system tests for the TypeScript code will not be able to test the website in a real browser environment
automatically. 
The accuracy of these tests is dependent on the accuracy of the jsdom implementation. 

- The performance tests will not be able to test the performance of the website in a real browser environment.

- With low priority, it is unlikely that there will be any security tests.
