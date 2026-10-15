# Import Readiness Checklist

This checklist is the gate between the clean StockKing foundation and importing the legacy scanner. It is intentionally conservative: existing trading records and credentials must remain untouched.

## Current state

- [ ] Confirm the exact source archive/version to import.
- [ ] Identify a verified database backup that contains the expected row data, not just table definitions.
- [x] Static inspection of `foscanner Schema.sql` found 25 `CREATE TABLE IF NOT EXISTS` statements and 25 `DROP TABLE IF EXISTS` statements, but no row inserts. Do not execute it against any valuable database.
- [ ] Run the legacy app only against an isolated disposable database until schema compatibility and writes are tested.
- [ ] Confirm no secrets or machine-specific settings are included in the import.
- [ ] Establish a reproducible Java 21 build without proprietary/local JARs or replace those dependencies with an approved adapter.
- [ ] Add tests for startup, API/UI paths, database migration, paper order lifecycle, and notification delivery.

## Mandatory safety gates

### Database
- Never import a dump into the existing database as an exploratory step.
- Take and verify a restorable backup before any schema change.
- Restore a candidate backup into a new, isolated database with a distinct name and account.
- Review every `DROP`, `TRUNCATE`, `DELETE`, schema auto-update, and bulk-refresh operation before running it.
- Keep Hibernate schema mutation disabled; use reviewed, versioned migrations only.
- Make scheduled jobs opt-in and prevent script refresh from running during startup or tests.
- Compare row counts and representative non-sensitive records before/after any migration. Do not publish sensitive row values in logs or CI artifacts.

### Broker and trading
- Paper mode is the only enabled execution mode.
- Broker connections must require an explicit user action; startup and page-render paths must not connect automatically.
- No live order endpoint may be enabled by a configuration default.
- Add tests proving that paper execution cannot invoke a broker order API.
- Risk sizing, max daily loss, per-trade risk, and kill-switch behavior must be enforced server-side, not just in the UI.

### Secrets and notifications
- Keep broker tokens, bot tokens, passwords, and webhook URLs server-side.
- Never return secrets in HTML, templates, logs, API responses, screenshots, or test artifacts.
- Test notification success/failure against the actual provider response; do not log success when delivery was not attempted or rejected.
- Use placeholders in example configuration and document required environment variables.

## Acceptance criteria before merging the application import

1. Clean checkout builds and tests on Java 21 in CI.
2. No credentials, private dumps, or local machine paths are committed.
3. A disposable database restore/migration test passes without touching a pre-existing database.
4. Startup and ordinary page/API requests do not connect to a broker or submit orders.
5. All scheduled/destructive work is disabled by default and explicitly tested.
6. Paper order lifecycle and risk controls have automated tests.
7. Notification tests validate provider responses and do not expose secrets.
8. README clearly states what is implemented versus planned.

## Local validation record

No database was opened, restored, or modified as part of this checklist. No SQL was executed. The existing StockKing foundation and the legacy scanner should be treated as separate until the above gates pass.
