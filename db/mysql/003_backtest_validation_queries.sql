-- Read-only smoke checks for the StockKing development schema.
USE stockking_dev;

SHOW TABLES;

SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
ORDER BY table_name;

SELECT version, description, installed_at
FROM schema_version
ORDER BY version;

-- Once backtest runs exist, summarize completed runs (no writes):
SELECT br.backtest_run_id, br.run_key, s.strategy_key, i.symbol, br.timeframe,
       br.range_start, br.range_end, br.status,
       JSON_UNQUOTE(JSON_EXTRACT(br.metrics_json, '$.netPnl')) AS net_pnl,
       JSON_UNQUOTE(JSON_EXTRACT(br.metrics_json, '$.maxDrawdown')) AS max_drawdown
FROM backtest_runs br
JOIN strategy_versions sv ON sv.strategy_version_id = br.strategy_version_id
JOIN strategies s ON s.strategy_id = sv.strategy_id
JOIN instruments i ON i.instrument_id = br.instrument_id
ORDER BY br.created_at DESC
LIMIT 50;
