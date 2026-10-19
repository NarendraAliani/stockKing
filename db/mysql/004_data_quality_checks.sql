-- StockKing development data-quality checks (MySQL 8.0+).
-- Read-only: this file contains SELECT statements only.
-- Run with stockking_dev selected, or keep the fully-qualified database names.

USE stockking_dev;

-- 1) Row counts: distinguish an empty pipeline from a populated one.
SELECT 'instruments' AS entity, COUNT(*) AS row_count FROM instruments
UNION ALL
SELECT 'market_candles', COUNT(*) FROM market_candles
UNION ALL
SELECT 'strategies', COUNT(*) FROM strategies
UNION ALL
SELECT 'strategy_versions', COUNT(*) FROM strategy_versions
UNION ALL
SELECT 'backtest_runs', COUNT(*) FROM backtest_runs
UNION ALL
SELECT 'backtest_trades', COUNT(*) FROM backtest_trades
UNION ALL
SELECT 'trade_signals', COUNT(*) FROM trade_signals
UNION ALL
SELECT 'paper_trades', COUNT(*) FROM paper_trades;

-- 2) Candle coverage by instrument and timeframe.
-- Empty result means no candle series have been ingested yet.
SELECT
    i.exchange_code,
    i.symbol,
    c.timeframe,
    COUNT(*) AS candle_count,
    MIN(c.open_time) AS first_candle_utc,
    MAX(c.open_time) AS last_candle_utc,
    SUM(CASE WHEN c.is_final = FALSE THEN 1 ELSE 0 END) AS non_final_candles,
    SUM(CASE WHEN c.volume < 0 THEN 1 ELSE 0 END) AS negative_volume_rows
FROM market_candles c
JOIN instruments i ON i.instrument_id = c.instrument_id
GROUP BY i.exchange_code, i.symbol, c.timeframe
ORDER BY i.exchange_code, i.symbol, c.timeframe;

-- 3) OHLC and basic source/time validation.
-- These checks should return zero rows. Database CHECK constraints provide
-- an additional guard, but ingestion should validate data before insert too.
SELECT
    candle_id, instrument_id, timeframe, open_time,
    open_price, high_price, low_price, close_price, volume, data_source
FROM market_candles
WHERE open_time IS NULL
   OR timeframe IS NULL OR TRIM(timeframe) = ''
   OR data_source IS NULL OR TRIM(data_source) = ''
   OR open_price <= 0 OR high_price <= 0 OR low_price <= 0 OR close_price <= 0
   OR high_price < low_price
   OR high_price < open_price OR high_price < close_price
   OR low_price > open_price OR low_price > close_price
   OR volume < 0
LIMIT 200;

-- 4) Look for duplicate logical bars regardless of data source.
-- Different sources can legitimately have separate rows; investigate any
-- duplicates before choosing a canonical source for a backtest.
SELECT
    instrument_id, timeframe, open_time,
    COUNT(*) AS source_rows,
    GROUP_CONCAT(DISTINCT data_source ORDER BY data_source) AS sources
FROM market_candles
GROUP BY instrument_id, timeframe, open_time
HAVING COUNT(*) > 1
ORDER BY open_time DESC
LIMIT 200;

-- 5) Instrument metadata that needs review before historical ingestion.
SELECT
    instrument_id, exchange_code, symbol, instrument_type,
    lot_size, tick_size, expiry_date, strike_price, option_type, active
FROM instruments
WHERE exchange_code IS NULL OR TRIM(exchange_code) = ''
   OR symbol IS NULL OR TRIM(symbol) = ''
   OR lot_size <= 0
   OR (instrument_type IN ('FUTURE', 'OPTION') AND expiry_date IS NULL)
   OR (instrument_type = 'OPTION' AND (strike_price IS NULL OR option_type IS NULL))
ORDER BY instrument_id
LIMIT 200;

-- 6) Backtest run status and date coverage.
SELECT
    status,
    COUNT(*) AS run_count,
    MIN(range_start) AS earliest_range_start,
    MAX(range_end) AS latest_range_end
FROM backtest_runs
GROUP BY status
ORDER BY status;

-- 7) Completed runs with their stored metrics and recorded trade counts.
SELECT
    r.backtest_run_id,
    r.run_key,
    r.status,
    r.timeframe,
    r.range_start,
    r.range_end,
    r.initial_capital,
    r.commission_rate,
    r.slippage_bps,
    COUNT(t.backtest_trade_id) AS trade_count,
    r.metrics_json
FROM backtest_runs r
LEFT JOIN backtest_trades t ON t.backtest_run_id = r.backtest_run_id
GROUP BY
    r.backtest_run_id, r.run_key, r.status, r.timeframe,
    r.range_start, r.range_end, r.initial_capital,
    r.commission_rate, r.slippage_bps, r.metrics_json
ORDER BY r.created_at DESC
LIMIT 100;
