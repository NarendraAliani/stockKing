# StockKing

StockKing is a local-first trading research and signal platform. The initial foundation is paper-trading-first; broker execution is not implemented or enabled by this baseline.

> **Safety default:** paper trading only. Signals and backtests are research outputs, not guarantees of performance.

## Current foundation

- Java 21 and Spring Boot 3.3.5
- Gradle Kotlin DSL
- Thymeleaf, Spring Web, and validation
- `/` redirects to the read-only paper-trading dashboard at `/paper`
- `/api/status` reports `status: ok`, paper mode, and live trading disabled when the status endpoint responds
- Read-only paper APIs: `/api/paper/dashboard`, `/api/paper/trades`, `/api/paper/trades/{signalId}`, `/api/paper/trades/export.csv`, `/api/paper/risk`, and `/api/paper/performance`
- In-memory paper-trade lifecycle and risk guard with automated tests
- MockMvc integration tests exercise paper-trade list/detail HTTP behavior and the safe status response
- GitHub Actions CI for clean compile/test runs
- Windows `app.bat` launcher that updates its branch, checks prerequisites, safely handles an occupied app port, runs tests, and starts in paper mode
- Safe application defaults with no broker credentials and no database connection

## Important status distinction

This repository now contains a **new StockKing foundation**, not the full inherited options/stocks scanner application. The supplied archive's 156 Java files have been reviewed but have **not yet been imported** into this repository. The old application's scanning, broker, persistence, and backtesting functionality is therefore not available in this baseline.

GitHub Actions currently passes for the feature branch; check [Actions](https://github.com/NarendraAliani/stockKing/actions) for the latest run on the exact commit you plan to use.

## Build and run locally

Requirements: JDK 21 and Gradle 8.x.

On Windows, open the repository folder in VS Code, check out `feat/windows-app-launcher`, and run `app.bat` from the integrated PowerShell terminal:

```powershell
.\\app.bat
```

The launcher fetches and fast-forward updates that branch, refuses to overwrite a dirty working tree, verifies Java/Gradle, checks port 8080, runs `clean test`, and starts the application with paper trading enabled and live trading disabled. It is intended to run from a clean checkout; commit or stash your own changes before launching. It only attempts to stop an existing listener when its process command line identifies the StockKing application; it does not terminate unrelated processes. Keep the launcher window open while the server runs.

For a manual build:

```bash
gradle clean test
gradle bootRun
```

Useful local URLs:

- Home: `http://localhost:8080/` (redirects to the paper dashboard)
- Status: `http://localhost:8080/api/status` (reports `ok` while this endpoint responds; it is not a dependency/database health check)
- Paper dashboard: `http://localhost:8080/paper`
- Dashboard JSON: `http://localhost:8080/api/paper/dashboard`
- Paper trades JSON: `http://localhost:8080/api/paper/trades`
- One paper trade by signal ID: `http://localhost:8080/api/paper/trades/{signalId}` (returns 404 when no matching paper trade exists)
- Open trades only: `http://localhost:8080/api/paper/trades?status=OPEN`
- Closed trades only: `http://localhost:8080/api/paper/trades?status=CLOSED`
- Buy trades only: `http://localhost:8080/api/paper/trades?side=BUY`
- One instrument only: `http://localhost:8080/api/paper/trades?instrument=RELIANCE`
- Paper risk snapshot: `http://localhost:8080/api/paper/risk`
- Paper performance summary: `http://localhost:8080/api/paper/performance`
- Download paper-trade journal as CSV: `http://localhost:8080/api/paper/trades/export.csv`

The trades API is read-only and supports optional combined filters: `status` (`OPEN` or `CLOSED`), `side` (`BUY` or `SELL`), and `instrument` (case-insensitive exact instrument ID match; surrounding whitespace is ignored). Filters can be combined, for example `/api/paper/trades?status=OPEN&side=BUY&instrument=RELIANCE`. Omitting all filters returns all trades. The CSV export downloads the current in-memory ledger with headers and all trade lifecycle fields; text cells are quoted and formula-like leading characters are neutralized for spreadsheet safety. It does not persist data. The performance summary reports closed-trade count, wins, losses, breakeven trades, win rate, net realized gross P&L, and maximum gross drawdown in chronological close order. Open trades are excluded. These are descriptive gross figures only; fees, taxes, slippage, and realistic fill simulation are not included. The paper ledger and risk counters are in-memory and reset on restart. The dashboard is read-only; this foundation does not send broker orders. The baseline has no database or broker connection configured and is intentionally not the inherited scanner app yet.

## Get the repository into VS Code on Windows

Open **Terminal → New Terminal** in VS Code and run these commands to clone the feature branch into a new local folder:

```powershell
cd D:\\
git clone --branch feat/windows-app-launcher --single-branch https://github.com/NarendraAliani/stockKing.git StockKing
cd .\\StockKing
code .
```

If the repo is already on your computer, open its existing folder in VS Code and run:

```powershell
git switch feat/windows-app-launcher
git pull --ff-only origin feat/windows-app-launcher
```

Then start the app from the repository root:

```powershell
.\\app.bat
```

Keep the terminal running while you use the local dashboard at `http://localhost:8080/paper`. The commands above download/update the repository; they do not deploy it to a remote server.

## Local database schema (development only)

A proposed MySQL 8 schema is available under `db/mysql/` for a disposable local database. It covers instrument masters, OHLCV candles, versioned strategies, backtest runs/trades, signals, and paper trades. It is not yet connected to the Spring application. Read `db/mysql/README.md` and inspect the scripts before running them; do not use an existing valuable database.

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
6. Design and test restart-safe paper-journal persistence as an opt-in local file store (without connecting an existing database).
7. Expand dashboard integration tests and operational startup/readiness checks.
8. Implement notification delivery only after delivery channels and secret handling are reviewed.



### Opt-in restart-safe paper journal

The local JSON journal can now be enabled explicitly. It remains **disabled by default**:

```powershell
$env:STOCKKING_PAPER_PERSISTENCE_ENABLED = "true"
$env:STOCKKING_PAPER_JOURNAL_PATH = "D:\StockKing\data\paper-journal.json"
.\\app.bat
```

The journal path is local-file storage only and is independent of any database. On startup, the app loads and validates the journal; an invalid or unsupported journal prevents startup rather than silently resetting trade history. Each successful ledger open/close is written before the in-memory ledger changes. If a write fails, the operation is rejected and the entry risk reservation is rolled back.

When persistence is enabled, today's entry count, realized daily gross P&L, and consecutive-loss count are reconstructed from journal timestamps. Trades and risk counters remain paper-only. Back up the journal file if you need to preserve it; this is not a substitute for a tested backup/restore process, and filesystem atomic-move/durability guarantees vary by platform.

To disable persistence for a run, set `STOCKKING_PAPER_PERSISTENCE_ENABLED=false` or remove the variable. Do not point the journal path at a database file or credentials file.


### Database data-quality checks

The development MySQL schema is still separate from the Spring application. After creating `stockking_dev`, use the read-only checks in [`db/mysql/004_data_quality_checks.sql`](db/mysql/004_data_quality_checks.sql) to inspect row counts, candle coverage, OHLC validity, duplicate logical bars, instrument metadata, and backtest status. Instructions are in [`db/mysql/README.md`](db/mysql/README.md). These checks do not create market data or execute backtests; zero row counts are expected until a reviewed ingestion pipeline is added. Keep broker execution disabled and do not point development scripts at a valuable or legacy database.
