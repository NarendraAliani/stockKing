-- StockKing backtest preflight checks (MySQL 8.0+).
-- Read-only: this script never inserts, updates, deletes, or drops data.
-- Run only against the dedicated development database.
USE stockking_dev;

-- 1) High-level readiness. Counts of zero mean a prerequisite is not loaded.
SELECT
    (SELECT COUNT(*) FROM instruments WHERE active = TRUE) AS active_instruments,
    (SELECT COUNT(*) FROM market_candles WHERE is_final = TRUE) AS final_candles,
    (SELECT COUNT(*) FROM strategies) AS strategies,
    (SELECT COUNT(*) FROM strategy_versions) AS strategy_versions,
    (SELECT COUNT(*) FROM backtest_runs) AS backtest_runs,
    (SELECT COUNT(*) FROM backtest_trades) AS backtest_trades;

-- 2) Available finalized candle series. Pick a series only after checking its
-- source, timezone convention, adjustment policy, and date coverage.
SELECT
    i.instrument_id,
    i.exchange_code,
    i.symbol,
    c.timeframe,
    c.data_source,
    COUNT(*) AS finalized_candles,
    MIN(c.open_time) AS first_open_time_utc,
    MAX(c.open_time) AS last_open_time_utc,
    COUNT(DISTINCT c.open_time) AS distinct_open_times
FROM market_candles c
JOIN instruments i ON i.instrument_id = c.instrument_id
WHERE c.is_final = TRUE
GROUP BY i.instrument_id, i.exchange_code, i.symbol, c.timeframe, c.data_source
ORDER BY i.exchange_code, i.symbol, c.timeframe, c.data_source;

-- 3) Candle gaps for common intraday/daily timeframes.
-- This flags only gaps that are not exact multiples of the nominal interval;
-- exchange holidays, sessions, and breaks mean results require market-calendar
-- interpretation and are not automatically data errors.
SELECT
    instrument_id,
    timeframe,
    data_source,
    open_time AS current_open_time_utc,
    LAG(open_time) OVER (
        PARTITION BY instrument_id, timeframe, data_source
        ORDER BY open_time
    ) AS previous_open_time_utc,
    TIMESTAMPDIFF(
        SECOND,
        LAG(open_time) OVER (
            PARTITION BY instrument_id, timeframe, data_source
            ORDER BY open_time
        ),
        open_time
    ) AS seconds_since_previous
FROM market_candles
WHERE is_final = TRUE
  AND timeframe IN ('1m', '5m', '15m', '30m', '60m', '1h', '1d')
  AND open_time IS NOT NULL
ORDER BY instrument_id, timeframe, data_source, open_time
LIMIT 500;

-- 4) Strategy versions with parameters and code revision. A strategy should
-- be explicitly reviewed and versioned before a backtest run is created.
SELECT
    s.strategy_id,
    s.strategy_key,
    s.display_name,
    s.enabled,
    sv.strategy_version_id,
    sv.version_number,
    sv.parameters_json,
    sv.code_revision,
    sv.created_at
FROM strategies s
LEFT JOIN strategy_versions sv ON sv.strategy_id = s.strategy_id
ORDER BY s.strategy_key, sv.version_number;

-- 5) Runs that are not completed, failed runs, or completed runs with no trades.
SELECT
    r.backtest_run_id,
    r.run_key,
    r.status,
    r.timeframe,
    r.range_start,
    r.range_end,
    r.error_code,
    r.error_message,
    COUNT(t.backtest_trade_id) AS stored_trade_count
FROM backtest_runs r
LEFT JOIN backtest_trades t ON t.backtest_run_id = r.backtest_run_id
GROUP BY
    r.backtest_run_id, r.run_key, r.status, r.timeframe,
    r.range_start, r.range_end, r.error_code, r.error_message
HAVING r.status <> 'COMPLETED'
    OR COUNT(t.backtest_trade_id) = 0
ORDER BY r.created_at DESC
LIMIT 200;
