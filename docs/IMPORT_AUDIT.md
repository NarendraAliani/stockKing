# Initial source archive audit

This report is based on the supplied `options-stocks-pro-master.zip` archive.

## Confirmed from the archive

- 156 Java source files and six Thymeleaf templates.
- Java 21 toolchain and Spring Boot 3.3.5, built with Gradle Kotlin DSL.
- Existing Dhan and Angel One source integrations, scanning services, backtesting services, and six Flyway SQL migration files.
- No Java `*Test.java` test files in the archive.
- No Gradle wrapper JAR and no `libs/smartstream-slim-2.2.0-fixed.jar`, although the original build references that local JAR.
- The supplied `application.properties` contains credential-looking fields and machine-specific MySQL configuration. It was not copied into this repository; a placeholder-based configuration was added instead.
- The source's script-master refresh path runs `TRUNCATE TABLE nfo_scripts` and `TRUNCATE TABLE equity_scripts` before reloading them.
- The available Java implementation of `SmartStreamTickerImpl` logs connect/subscribe operations but does not implement an actual WebSocket connection. Do not treat that class as proof of a working Angel One streaming feed.
- The legacy `TelegramService` contains a comment indicating its send request is not actually posted, despite logging a success-like message. It should not be relied on for delivery. A different notification service also exists and needs separate tests.

## Verification status

A Gradle test run could not be started from the supplied archive: the wrapper JAR is missing, and the container has no network connectivity to retrieve it. The source has not been compiled or tested in this environment. Historical test report Markdown in the archive is not a substitute for running the current code.

## Safety gates before local startup

1. Restore a complete Gradle wrapper and resolve all declared dependencies.
2. Audit all required configuration properties and provide a local-only configuration file.
3. Use a disposable database initially; do not point to the user's existing database.
4. Review the truncating script refresh code and replace it with a recoverable, staged refresh process before any run against valuable data.
5. Keep live order placement disabled and implement a paper-trading execution boundary.
6. Add automated tests for critical strategy, risk, persistence, and notification behavior.

## Limitations

This is a first-pass static archive review, not a complete security review or proof of functional correctness. The new repository currently contains safe onboarding files and this audit report; the entire source archive has not yet been imported into GitHub.
