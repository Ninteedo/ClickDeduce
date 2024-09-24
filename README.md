# Usage

ClickDeduce is available at [clickdeduce.rgh.dev](https://clickdeduce.rgh.dev/).
The guide page can be found at [clickdeduce.rgh.dev/guide](https://clickdeduce.rgh.dev/guide).

To view the API docs, visit [clickdeduce.rgh.dev/api](https://clickdeduce.rgh.dev/api).

## Development

### Requirements

- JDK 18+
- [sbt 1.5+](https://www.scala-sbt.org/1.x/docs/Setup.html)
- [Node.js 16+](https://nodejs.org/en/learn/getting-started/how-to-install-nodejs)

### Setup

To install the required dependencies, run the following commands:

```bash
sbt update
npm install
```

### Project Layout

Project source files are present in [the shared Scala code directory](scala/shared/src/main/scala) for the Scala code
and [webapp/scripts](webapp/scripts) for the TypeScript code.

There are functions missing from the TypeScript code which require the ScalaJS code to be built first.
To build the ScalaJS code, run the following command:

```bash
sbt fastOptJS
```

This will generate `clickdeduce-opt.js` in `webapp/scripts`.

### Building Website

To build the website, run the following command:

```bash
npm run build
```

This will generate the website in the [webapp/build](webapp/build) directory,
including [index.html](webapp/build/index.html) for the main page.
