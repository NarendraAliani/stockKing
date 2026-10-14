# StockKing

StockKing is a local-first trading research and signal platform. The initial foundation is paper-trading-first; broker execution is not implemented or enabled by this baseline.

> **Safety default:** paper trading only. Signals and backtests are research outputs, not guarantees of performance.

## Current foundation

- Java 21 and Spring Boot 3.3.5
- Gradle Kotlin DSL
- Thymeleaf, Spring Web, and validation
- `/api/status` endpoint
- Immutable initial risk policy with tests
- GitHub Actions CI for clean compile/test runs
- Safe application defaults with no broker credentials and no database connection

## Important status distinction

This repository now contains a **new StockKing foundation**, not the full inherited options/stocks scanner application. The supplied archive's 156 Java files have been reviewed but have **not yet been imported** into this repository. The old application's scanning, broker, persistence, and backtesting functionality is therefore not available in this baseline.

The initial CI workflow is being used to verify the new foundation. See [Actions](https://github.com/NarendraAliani/stockKing/actions) for the actual result; a committed workflow is not itself proof of a passing build.

## Build and run locally

Requirements: JDK 21 and a Gradle 8.x installation.

```bash
gradle clean test
gradle bootRun
```

Then check `http://localhost:8080/api/status`. The initial baseline has no database configured and no broker connection configured, so it is intentionally not the inherited scanner app yet.

## Safe configuration

- Do not commit credentials, tokens, passwords, webhook URLs, database dumps, or local settings.
- The raw `application.properties` from the source archive was excluded because it contains credential-looking values and machine-specific settings.
- The original archive also included a Dhan token and Angel One credential values. Rotate those credentials before reuse.
- The inherited project uses Hibernate schema auto-update and includes a script refresh that truncates `nfo_scripts` and `equity_scripts`. Do not point it at your existing database before those paths are reviewed and made safe.
- Do not use live order placement during development.

## Source archive audit

See [initial audit](docs/IMPORT_AUDIT.md) and [local run checklist](docs/LOCAL_RUN_CHECKLIST.md). The supplied archive also references a missing local SmartStream JAR and does not include the Gradle wrapper JAR. The new foundation avoids the proprietary/local JAR dependency while CI validates its declared Maven dependencies.

## Next development stages

1. Import sanitized source and reconcile duplicate packages/configuration.
2. Restore or replace missing SmartStream dependency only after confirming licensing and required API usage.
3. Establish a reproducible full-application build and automated tests.
4. Review schema migrations, database writes, and script refresh behavior.
5. Verify scanner/backtest timing, costs, stop/target rules, and risk sizing.
6. Implement paper trade lifecycle, journal, and notification delivery.