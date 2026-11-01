# xir-sax

[![Build](https://github.com/flowdigger/xir-sax/actions/workflows/ci.yml/badge.svg)](https://github.com/flowdigger/xir-sax/actions/workflows/ci.yml)
[![Release](https://github.com/flowdigger/xir-sax/actions/workflows/release.yml/badge.svg)](https://github.com/flowdigger/xir-sax/actions/workflows/release.yml)
[![Coverage CI](https://github.com/flowdigger/xir-sax/actions/workflows/coverage.yml/badge.svg)](https://github.com/flowdigger/xir-sax/actions/workflows/coverage.yml)
[![CodeQL](https://github.com/flowdigger/xir-sax/actions/workflows/codeql.yml/badge.svg)](https://github.com/flowdigger/xir-sax/actions/workflows/codeql.yml)
[![Dependency Check](https://github.com/flowdigger/xir-sax/actions/workflows/dependency-check.yml/badge.svg)](https://github.com/flowdigger/xir-sax/actions/workflows/dependency-check.yml)
[![SpotBugs Security](https://github.com/flowdigger/xir-sax/actions/workflows/spotbugs-security.yml/badge.svg)](https://github.com/flowdigger/xir-sax/actions/workflows/spotbugs-security.yml)
[![Dependabot](https://img.shields.io/badge/dependabot-enabled-025E8C?logo=dependabot)](https://github.com/flowdigger/xir-sax/security/dependabot)
[![Coverage](https://codecov.io/gh/flowdigger/xir-sax/graph/badge.svg?branch=main)](https://codecov.io/gh/flowdigger/xir-sax?branch=main)
[![Maven Central](https://img.shields.io/maven-central/v/org.flowdigger/xir-sax.svg)](https://search.maven.org/artifact/org.flowdigger/xir-sax)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21+-green.svg)](https://www.oracle.com/java/)
[![Gradle](https://img.shields.io/badge/gradle-9.5+-blue.svg)](https://gradle.org/)

SAX parser, serializer, XMLReader for bracket-based XML/XDM S-expression format.

## Position in XIR spine

- **Problem solved**: parse and serialize XIR deterministically for build pipelines and tool interoperability.
- **Relation to XIR**: core runtime component for XIR language processing.
- **Why it matters**: enables reproducible observation/derivation tooling without repeated format remapping.

```text
Structured Knowledge -> XIR -> Observation -> Derivation -> Knowledge -> Applications
```

Next repository in reading path: [xir-g4](https://github.com/flowdigger/xir-g4).

## Coordinates

```text
org.flowdigger:xir-sax:<version>
```

## Namespace

- Java package: `org.flowdigger.xir`
- Internal XDM bridge namespace URI: `urn:org.flowdigger.xml:xdm`
- Internal XDM bridge prefix: `xdm`

## Included APIs

- `XirParser` - parses S-expression input into SAX events
- `XirSerializer` - renders SAX events as canonical or legacy S-expression
- `XirReader` - `XMLReader` facade over parser

## Canonical syntax excerpt

- `()` nodes
- `{}` associative structures
- `[]` sequences
- `.` document head
- `?` processing instruction head
- `!` comment head

Examples:

- `(book (title "XML"))`
- `(book { id "b1" xmlns:m "urn:math" } (m:title "XML"))`
- `(. { version "1.0" encoding "UTF-8" } (book))`
- `{ name John age 42 }`
- `[ A B C ]`
- `(xs:boolean true)`

Notes:

- Bare `{...}` and `[...]` are canonical map/sequence values.
- Legacy `(xdm:map ...)` / `(xdm:array ...)` input is still accepted by parser for compatibility.
- To disambiguate element with first child map, use explicit empty attrs block: `(element {} { key value })`.
- XML in `http://www.w3.org/2005/xpath-functions` (`map`, `array`, `list`, `string`, `number`, `boolean`) is recognized in SAX serialization path.
- `XirSerializer` autotyping mode defaults to strict-string and supports optional promote mode.

## Run tests

```bash
./gradlew test
```

## Test Coverage

Generate coverage report and enforce current minimum line coverage baseline (>= 0%):

```bash
./gradlew coverage
```

Coverage report outputs:

- XML: `build/reports/jacoco/test/jacocoTestReport.xml`
- HTML: `build/reports/jacoco/test/html/index.html`

CI coverage workflow: `.github/workflows/coverage.yml`

Codecov upload uses GitHub OIDC in CI (`codecov/codecov-action`) and reads `build/reports/jacoco/test/jacocoTestReport.xml`.

## Building

```bash
./gradlew build
```

Required Java version: **21+**

## GitHub Actions

- `ci.yml` runs build and unit tests on push and pull request
- `coverage.yml` runs coverage task, uploads JaCoCo report artifact, uploads coverage to Codecov
- `codeql.yml` runs GitHub CodeQL static analysis
- `dependency-check.yml` runs OWASP Dependency-Check and uploads reports
- `spotbugs-security.yml` runs SpotBugs + FindSecBugs and uploads report
- `release.yml` publishes signed artifacts using Sonatype Central Publisher API
- `dependabot.yml` enables weekly updates for Gradle and GitHub Actions

## License

[MIT](LICENSE)
