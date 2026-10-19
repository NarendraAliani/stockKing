# StockKing local database schema

Target: MySQL 8.0+ (uses InnoDB, JSON, CHECK constraints and utf8mb4_0900_ai_ci).

## Files

- `001_create_stockking_dev.sql`: creates only the dedicated `stockking_dev` database; no DROP statements.
- `002_create_schema.sql`: creates the initial schema for instruments, OHLCV candles, strategy definitions/versions, backtest runs/trades, signals, and paper trades.
- `003_backtest_validation_queries.sql`: read-only queries to validate table/index setup and inspect backtest result summaries.
- `004_data_quality_checks.sql`: read-only row counts, candle coverage, OHLC/source validation, duplicate-bar review, instrument metadata checks, and backtest summaries.
- `005_backtest_preflight.sql`: read-only readiness counts, available candle series, potential candle-gap candidates, strategy-version review, and incomplete/empty backtest-run checks.

## Local setup (PowerShell)

From the repository root, first inspect the scripts. Then, if you have MySQL's client on PATH, run:

```powershell
mysql --version
mysql -u root -p < .\db\mysql\001_create_stockking_dev.sql
mysql -u root -p stockking_dev < .\db\mysql\002_create_schema.sql
```

Type your local MySQL password when prompted. Do not put passwords in the command or commit them to Git. If the mysql client isn't on PATH, use the full path to `mysql.exe` or MySQL Workbench and run the files in numeric order.

Verify tables:

```sql
USE stockking_dev;
SHOW TABLES;
SELECT * FROM schema_version;
```

## Run read-only data checks

After pulling the latest branch, run the new script in phpMyAdmin:

1. Select `stockking_dev` in the left sidebar.
2. Open the **Import** tab and select `db/mysql/004_data_quality_checks.sql` from your checkout, or open the file and paste its SQL into the **SQL** tab.
3. Execute the statements. It begins with row counts. Candle coverage will be empty until data is loaded. The OHLC/source and instrument-metadata queries should return zero rows on a clean dataset; duplicate logical bars may be legitimate when multiple data sources are stored, so review rather than automatically delete them.

The scripts contain only `USE`, `SELECT`, and read-only common table expressions/window functions. They do not insert, update, delete, or drop data.

## Backtest preflight

Run `db/mysql/005_backtest_preflight.sql` after the data-quality checks. It reports active instruments, finalized candles, strategies and versions; available candle series and their date coverage; potential gaps between bars; versioned strategy parameters; and runs that are not completed or have no recorded trades. Gap candidates require review against the relevant exchange calendar because weekends, holidays, and session breaks may be valid. These checks do not run a backtest or create data.

## Important safety boundary

This is a new, proposed development schema based on the current StockKing foundation and backtesting requirements. It is **not yet wired to the Spring application**, and it is not represented as an exact reconstruction of the legacy database schema. The source archive audit identified legacy Flyway migrations and a script refresh that truncates script-master tables; those legacy migrations must be reviewed separately before any import. Do not point this schema or the legacy app at an existing valuable database. Use only a disposable local database, and keep broker execution disabled.

## Table map

- `instruments`: exchange symbols, instrument type and derivative metadata.
- `market_candles`: normalized OHLCV bars keyed by instrument/timeframe/time/source.
- `strategies`, `strategy_versions`: stable strategy identity and immutable parameter snapshots.
- `backtest_runs`: parameterized run, period, costs, status and summary metrics.
- `backtest_trades`: reproducible entry/exit events and gross/net P&L per run.
- `trade_signals`: generated research signals with unique idempotency key.
- `paper_trades`: simulated positions and realized costs/P&L, separate from backtest trades.
- `schema_version`: initial schema marker; production migrations should later use Flyway.

Before running real backtests, add corporate-action adjustments where relevant, instrument expiry/lot/tick rules, time-zone normalization, brokerage/taxes/slippage assumptions, survivorship-bias controls, and tests against known candle sequences. The new data-quality script provides the first read-only checks, not a complete certification of market data.
