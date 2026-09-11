# Exodex

Exodex is a Java-based astronomy application for exploring nearby stars and
exoplanets. It combines star catalog data with NASA exoplanet data and serves
the web interface and catalog API from an embedded Jetty web server.

The application is packaged as an executable JAR. Its main class starts the
server on port `42315`, so once it is running the web interface is available
at:

```text
http://localhost:6769/
```

## Requirements

- Java 8 or newer
- Apache Maven

## Build and Run

From the project root, run:

```bash
mvn clean package
cd target
java -jar exodex-1.0-SNAPSHOT-jobjar.jar
```

The first command cleans previous build output and creates the executable JAR
in the `target` directory. The final command starts the embedded web server.

## Project Structure

- `src/main/java` contains the server, API, catalog, and astronomy data code.
- `src/main/www` contains the web interface assets.
- `src/main/resources` contains packaged datasets and application resources.
- `src/test/java` contains the automated tests.
