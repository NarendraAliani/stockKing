# Local run and import checklist

This is a gate checklist, not a claim that the application is runnable yet.

## Before importing source

- [ ] Remove local caches such as `.gradle/` and generated build output from the import.
- [ ] Remove the raw `src/main/resources/application.properties`; it contains credentials and machine-specific database settings.
- [ ] Scan the complete source archive for secrets, tokens, passwords, webhook URLs, and personal data.
- [ ] Restore the missing Gradle wrapper JAR or regenerate the wrapper from a compatible installed Gradle version.
- [ ] Resolve the missing `libs/smartstream-slim-2.2.0-fixed.jar` dependency without committing proprietary or unlicensed binaries.
- [ ] Confirm whether the vendored Angel One classes are sufficient to compile without that JAR.

## Before starting locally

- [ ] Use Java 21 and a disposable local MySQL database.
- [ ] Use a dedicated, least-privilege database user.
- [ ] Keep broker tokens blank until the app can start without them.
- [ ] Change Hibernate schema behavior from `update` to a reviewed strategy; never use `update` against the existing database as a first run.
- [ ] Review Flyway migrations and existing-schema compatibility before enabling migrations.
- [ ] Disable or guard script synchronization until its `TRUNCATE TABLE` behavior is made recoverable.
- [ ] Keep all order placement disabled; use paper trading only.

## Build and test acceptance criteria

- [ ] A clean checkout can run `./gradlew test` (or a documented equivalent).
- [ ] CI compiles the application from a clean environment.
- [ ] Tests cover strategy calculations, candle ordering/timezones, duplicate signal prevention, risk sizing, stop/target handling, and notification failures.
- [ ] Application startup does not require live broker credentials.
- [ ] Startup and shutdown do not modify any database other than the disposable development database.
- [ ] No secrets appear in logs or Git history.

## Completion definition

The source import is complete only when the source tree is present in GitHub, a clean build is verified, and the above safety gates have explicit pass/fail results. A README or a historical report alone is not completion evidence.
