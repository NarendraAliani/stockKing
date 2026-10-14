# StockKing

StockKing is a local-first trading research and signal platform, intended to start with paper trading. It is being assembled from the supplied Spring Boot application in staged, testable increments.

> **Safety default:** paper trading only. No live order placement is enabled by this repository baseline. Trading signals are research outputs, not guarantees of performance.

## Planned technology baseline

- Java 21
- Spring Boot 3.3.5
- Gradle Kotlin DSL
- Thymeleaf web UI
- Spring Data JPA / MySQL
- Supplied source contains Dhan and Angel One integration code, scanner services, and backtesting modules

## Current repository status

This repository currently contains safety/onboarding files and an initial source-archive audit report. **The full Java application has not yet been imported into this repository, and this repository is not runnable as an application yet.**

The supplied ZIP does not contain the Gradle wrapper JAR or the local SmartStream JAR referenced by the original build. It also has no Java `*Test.java` test files. Build and test status is therefore not verified. See [`docs/IMPORT_AUDIT.md`](docs/IMPORT_AUDIT.md) for confirmed findings and safety gates.

## Local setup guidance

1. Install a JDK 21 distribution.
2. Restore a complete Gradle wrapper or install a compatible Gradle distribution.
3. Create a dedicated disposable local MySQL database and restricted application user. Do not point a first run at a valuable existing database.
4. Set credentials using environment variables or a local file that is ignored by Git.
5. Review all database mutation and migration behavior before starting the server.
6. Run tests before using any scanner, backtest, or notification feature.

Do not commit real access tokens, API keys, passwords, Telegram tokens, exports, or database dumps.

## Configuration

The raw `application.properties` from the archive was not copied because it contains credential-looking fields and machine-specific database settings. A placeholder-based `application.properties` and `.env.example` should be used for local development once the application source is imported.

Common environment variables include `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `DHAN_ACCESS_TOKEN`, `DHAN_CLIENT_ID`, `ANGEL_API_KEY`, `ANGEL_CLIENT_ID`, `ANGEL_PASSWORD`, and `ANGEL_TOTP_SECRET`.

## Development stages

1. Import the source with credentials removed and establish a reproducible build.
2. Audit schema compatibility and preserve existing database contents.
3. Review script refresh, scanner, and backtest correctness, including look-ahead bias, costs, and risk assumptions.
4. Implement paper trading and risk controls before considering any live execution feature.
5. Integrate notifications only after verifying actual delivery behavior and API support.

## Safety notes

- Separate a generated signal, a simulated paper fill, and a broker-reported fill.
- Never log access tokens or full authorization headers.
- Do not enable Hibernate automatic schema updates against an existing database without explicit schema review.
- The inherited script-master refresh has `TRUNCATE TABLE` operations; do not run it against valuable data before replacing or explicitly approving that behavior.
- A backtest or signal is not evidence of future profits.
