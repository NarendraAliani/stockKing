# Safe legacy import plan

## Purpose

Bring the inherited scanner into StockKing incrementally without exposing broker credentials, connecting to a broker at startup, sending accidental notifications, or changing the user’s existing database.

## Non-negotiable operating defaults

- Execution mode remains paper.
- Live order placement remains disabled and must be blocked at the service boundary, not merely hidden in the UI.
- Broker connections are opt-in, explicit user actions; page views and application startup must not connect.
- Scheduled jobs that synchronize data or send notifications are disabled unless explicitly enabled in a local environment.
- Secrets are read from environment variables or a local untracked secrets file. They must never be returned to browser DTOs or logged.
- Schema creation/changes are migration-controlled. Hibernate must not update a valuable database automatically.
- No schema dump is imported into an existing database as a setup step.

## Import sequence

### Gate 0 — Preserve and identify inputs

1. Keep the original archive and SQL dump unchanged as read-only evidence.
2. Record SHA-256 hashes of both files locally.
3. Confirm whether the SQL dump is schema-only or contains rows, and identify which database it came from.
4. Scan the archive and Git history for credentials. Rotate any credentials that appeared in the original archive; deleting a file does not remove secrets from Git history.
5. Never copy local caches, build output, `.env`, broker tokens, or raw local configuration into Git.

### Gate 1 — Build reproducibility

1. Import source in a dedicated branch, preserving the current minimal foundation.
2. Resolve the missing SmartStream dependency only after confirming license and whether the source really needs it. Do not commit an unknown/proprietary JAR.
3. Restore a Gradle wrapper that can be verified and pinned to the selected Gradle release.
4. Run a clean Java 21 build and tests in CI. Report the actual result; do not infer success from a workflow file existing.

### Gate 2 — Runtime and broker safety

1. Remove startup auto-connect behavior.
2. Ensure scanner pages, backtest pages, and API reads never initiate broker connections.
3. Keep Dhan and Angel One adapters behind an explicit connection command and a paper-only execution boundary.
4. Confirm no order-placement call is reachable while live trading is disabled.
5. Keep the incomplete SmartStream stub marked unsupported; do not show a connected/healthy state unless a real session is confirmed.

### Gate 3 — Database protection

1. Use a new disposable development database for first integration tests.
2. Inspect every migration against the supplied schema dump before enabling migrations.
3. Replace destructive script-master refresh (`TRUNCATE` of `nfo_scripts` / `equity_scripts`) with staging tables, validation, transactional swap where supported, and a recoverable backup/rollback strategy.
4. Do not execute the inherited script sync or import the SQL dump against the user’s existing database.
5. Add integration tests proving startup and failed refresh leave prior data intact.

### Gate 4 — Notifications and secrets

1. Remove bot tokens and broker secrets from browser responses/settings DTOs.
2. Use a single notification adapter with explicit response/error handling and redacted logs.
3. Test Telegram failure, timeout, and rejected-token cases. Do not report success unless the provider confirms delivery acceptance.
4. Add Arattai only through a documented, supported integration; do not invent an API or store its credentials in client-side settings.

### Gate 5 — Trading correctness and UI

1. Reconcile underlying-first research requirements with the legacy options-focused history.
2. Add tests for candle ordering, exchange timezone/session boundaries, transaction costs, position sizing, stop/target rules, duplicate signals, daily loss limits, and consecutive-loss controls.
3. Compare multiple strategies on the same underlying/time range and expose assumptions and data coverage.
4. Build the web UI around signal provenance, paper positions, trade journal, risk limits, and audit history.
5. Validate performance only on out-of-sample data; backtests are not guarantees.

## Acceptance checklist

- [ ] Clean checkout builds on Java 21 with a pinned Gradle wrapper.
- [ ] CI clean build and tests pass.
- [ ] Secret scan passes; credentials are rotated and not present in current files or history.
- [ ] Startup is safe without broker credentials and does not connect to brokers.
- [ ] Live execution is unreachable while disabled.
- [ ] No job truncates or overwrites production tables.
- [ ] Database migrations pass against a disposable copy and have a rollback plan.
- [ ] Notification delivery status is truthful and tokens never reach the browser.
- [ ] Paper-mode tests cover risk limits and core trade lifecycle.
- [ ] A human explicitly approves any later operation against the existing database.

## Current status

This document is a plan and safety gate, not a claim that the inherited application has been imported or that these checks have passed. The existing minimal StockKing foundation should remain buildable while legacy modules are integrated in small, reviewed increments.
