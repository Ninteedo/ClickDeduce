## Development

### Requirements

- JDK 18+
- [sbt 1.5+](https://www.scala-sbt.org/1.x/docs/Setup.html)
- [Node.js 14+](https://nodejs.org/en/learn/getting-started/how-to-install-nodejs)

### Setup

To install the required dependencies, run the following commands:

```bash
sbt update
npm install
```

### Project Layout

Project source files are present in [src/main/scala](src/main/scala) for the Scala code
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

Or, to skip the ScalaJS build, run:

```bash
npm run build:webpack
```

This will generate the website in the [build](build) directory,
including [index.html](build/index.html) and [bundle.js](build/bundle.js).
