# StockKing

StockKing is a local-first trading research and signal platform, starting with paper trading. It builds on the supplied Spring Boot application and will be developed in staged, testable increments.

> **Safety default:** paper trading only. No live order placement is enabled by this project setup. Trading signals are research outputs, not guarantees of performance.

## Current baseline

- Java 21
- Spring Boot 3.3.5
- Gradle Kotlin DSL
- Thymeleaf web UI
- Spring Data JPA / MySQL
- Existing broker integration code for Dhan and Angel One
- Existing scanner and backtesting modules

The imported source archive did not include the Gradle wrapper JAR or automated Java test files. Build and test status is therefore **not yet verified**.

## Local setup

1. Install a JDK 21 distribution.
2. Install Gradle 8.x locally, or restore the Gradle wrapper using `gradle wrapper` from the project root.
3. Create a dedicated local MySQL database and user for StockKing. Do not point the application at a production or valuable existing database during first run.
4. Set required connection and broker credentials as environment variables or in an ignored local configuration file.
5. Review configuration and database migration behavior before starting the server.
6. Run tests, then start the application from your IDE or with Gradle.

Do not commit real access tokens, API keys, passwords, Telegram tokens, exports, or database dumps.

## Configuration

Use environment variables for secrets and machine-specific values. The committed `application.properties` is intentionally not part of this baseline because the supplied version contains credential-looking fields and local database settings.

The source uses keys including:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `DHAN_ACCESS_TOKEN`
- `DHAN_CLIENT_ID`
- `ANGEL_API_KEY`
- `ANGEL_CLIENT_ID`
- `ANGEL_PASSWORD`
- `ANGEL_TOTP_SECRET`

Spring's relaxed binding can map environment variable names to property keys (for example, `dhan.access.token` to `DHAN_ACCESS_TOKEN`). Review any other required application properties in source before startup.

## Development stages

1. Safely establish a reproducible local build and configuration.
2. Audit data access and preserve compatibility with the existing database without destructive schema changes.
3. Verify scanner and backtest correctness, including look-ahead bias, transaction costs and risk assumptions.
4. Add paper-trading and risk controls before considering any live execution feature.
5. Integrate notification channels only after verifying their APIs and testing delivery behavior.

## Important notes

- Never treat a successful signal/backtest as evidence of future profits.
- Separate signal generation, paper fills and broker-reported fills.
- Protect credentials at rest and never log tokens.
- Do not enable Hibernate automatic schema updates against an existing database without an explicit schema review.
- Keep all first-run testing isolated from real funds and production data.

## Repository status

This repository currently contains the safe onboarding baseline. The full application source is being staged after configuration and secret handling are made safe and a reproducible build can be verified.
