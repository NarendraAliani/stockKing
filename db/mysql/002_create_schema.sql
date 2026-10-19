-- StockKing development schema (MySQL 8.0+).
-- Run only against a NEW disposable local database. This script creates tables but never drops data.
USE stockking_dev;

CREATE TABLE IF NOT EXISTS schema_version (
    version BIGINT NOT NULL PRIMARY KEY,
    description VARCHAR(200) NOT NULL,
    installed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS instruments (
    instrument_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    exchange_code VARCHAR(24) NOT NULL,
    symbol VARCHAR(64) NOT NULL,
    trading_symbol VARCHAR(96) NULL,
    instrument_type VARCHAR(24) NOT NULL DEFAULT 'EQUITY',
    underlying_symbol VARCHAR(64) NULL,
    expiry_date DATE NULL,
    strike_price DECIMAL(20,6) NULL,
    option_type VARCHAR(8) NULL,
    lot_size INT UNSIGNED NOT NULL DEFAULT 1,
    tick_size DECIMAL(20,8) NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    source_token VARCHAR(96) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_instruments_exchange_symbol UNIQUE (exchange_code, symbol, expiry_date, strike_price, option_type),
    CONSTRAINT chk_instruments_lot_size CHECK (lot_size > 0),
    CONSTRAINT chk_instruments_strike CHECK (strike_price IS NULL OR strike_price >= 0),
    INDEX idx_instruments_symbol (symbol),
    INDEX idx_instruments_underlying_expiry (underlying_symbol, expiry_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS market_candles (
    candle_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    timeframe VARCHAR(12) NOT NULL,
    open_time DATETIME(6) NOT NULL COMMENT 'UTC timestamp; normalize at ingestion',
    close_time DATETIME(6) NULL,
    open_price DECIMAL(20,8) NOT NULL,
    high_price DECIMAL(20,8) NOT NULL,
    low_price DECIMAL(20,8) NOT NULL,
    close_price DECIMAL(20,8) NOT NULL,
    volume DECIMAL(24,4) NOT NULL DEFAULT 0,
    open_interest DECIMAL(24,4) NULL,
    data_source VARCHAR(32) NOT NULL,
    is_final BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_candles_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id),
    CONSTRAINT uq_candles_bar UNIQUE (instrument_id, timeframe, open_time, data_source),
    CONSTRAINT chk_candles_ohlc CHECK (
        open_price > 0 AND high_price > 0 AND low_price > 0 AND close_price > 0
        AND high_price >= low_price
        AND high_price >= open_price AND high_price >= close_price
        AND low_price <= open_price AND low_price <= close_price
        AND volume >= 0
    ),
    INDEX idx_candles_series (instrument_id, timeframe, open_time),
    INDEX idx_candles_time (timeframe, open_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS strategies (
    strategy_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    strategy_key VARCHAR(96) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    description TEXT NULL,
    strategy_type VARCHAR(32) NOT NULL DEFAULT 'RULE_BASED',
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_strategies_key UNIQUE (strategy_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS strategy_versions (
    strategy_version_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    strategy_id BIGINT NOT NULL,
    version_number INT UNSIGNED NOT NULL,
    parameters_json JSON NOT NULL,
    code_revision VARCHAR(64) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_strategy_versions_strategy FOREIGN KEY (strategy_id) REFERENCES strategies(strategy_id),
    CONSTRAINT uq_strategy_version UNIQUE (strategy_id, version_number),
    CONSTRAINT chk_strategy_version_number CHECK (version_number > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS backtest_runs (
    backtest_run_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    run_key CHAR(36) NOT NULL,
    strategy_version_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    timeframe VARCHAR(12) NOT NULL,
    range_start DATETIME(6) NOT NULL,
    range_end DATETIME(6) NOT NULL,
    initial_capital DECIMAL(20,4) NOT NULL,
    commission_rate DECIMAL(12,8) NOT NULL DEFAULT 0,
    slippage_bps DECIMAL(12,4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    started_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    metrics_json JSON NULL,
    error_code VARCHAR(96) NULL,
    error_message VARCHAR(1000) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_backtest_run_key UNIQUE (run_key),
    CONSTRAINT fk_backtest_strategy_version FOREIGN KEY (strategy_version_id) REFERENCES strategy_versions(strategy_version_id),
    CONSTRAINT fk_backtest_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id),
    CONSTRAINT chk_backtest_range CHECK (range_end > range_start),
    CONSTRAINT chk_backtest_capital CHECK (initial_capital > 0),
    CONSTRAINT chk_backtest_costs CHECK (commission_rate >= 0 AND slippage_bps >= 0),
    INDEX idx_backtest_status_created (status, created_at),
    INDEX idx_backtest_strategy_created (strategy_version_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS backtest_trades (
    backtest_trade_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    backtest_run_id BIGINT NOT NULL,
    trade_number INT UNSIGNED NOT NULL,
    instrument_id BIGINT NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity DECIMAL(20,4) NOT NULL,
    entry_time DATETIME(6) NOT NULL,
    entry_price DECIMAL(20,8) NOT NULL,
    exit_time DATETIME(6) NULL,
    exit_price DECIMAL(20,8) NULL,
    stop_price DECIMAL(20,8) NULL,
    target_price DECIMAL(20,8) NULL,
    gross_pnl DECIMAL(20,4) NULL,
    fees DECIMAL(20,4) NOT NULL DEFAULT 0,
    slippage_cost DECIMAL(20,4) NOT NULL DEFAULT 0,
    net_pnl DECIMAL(20,4) NULL,
    exit_reason VARCHAR(48) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_backtest_trade_run FOREIGN KEY (backtest_run_id) REFERENCES backtest_runs(backtest_run_id),
    CONSTRAINT fk_backtest_trade_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id),
    CONSTRAINT uq_backtest_trade_number UNIQUE (backtest_run_id, trade_number),
    CONSTRAINT chk_backtest_trade_side CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT chk_backtest_trade_quantity CHECK (quantity > 0),
    CONSTRAINT chk_backtest_trade_prices CHECK (entry_price > 0 AND (exit_price IS NULL OR exit_price > 0)),
    INDEX idx_backtest_trades_run_time (backtest_run_id, entry_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS trade_signals (
    signal_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    signal_key VARCHAR(128) NOT NULL,
    strategy_version_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    timeframe VARCHAR(12) NOT NULL,
    side VARCHAR(8) NOT NULL,
    signal_time DATETIME(6) NOT NULL,
    entry_price DECIMAL(20,8) NOT NULL,
    stop_price DECIMAL(20,8) NOT NULL,
    target_price DECIMAL(20,8) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
    reason VARCHAR(500) NULL,
    metadata_json JSON NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_trade_signal_key UNIQUE (signal_key),
    CONSTRAINT fk_signal_strategy_version FOREIGN KEY (strategy_version_id) REFERENCES strategy_versions(strategy_version_id),
    CONSTRAINT fk_signal_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id),
    CONSTRAINT chk_signal_side CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT chk_signal_prices CHECK (entry_price > 0 AND stop_price > 0 AND target_price > 0),
    INDEX idx_signals_instrument_time (instrument_id, signal_time),
    INDEX idx_signals_strategy_time (strategy_version_id, signal_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS paper_trades (
    paper_trade_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    signal_id BIGINT NULL,
    trade_key VARCHAR(128) NOT NULL,
    instrument_id BIGINT NOT NULL,
    strategy_version_id BIGINT NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity DECIMAL(20,4) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    opened_at DATETIME(6) NOT NULL,
    entry_price DECIMAL(20,8) NOT NULL,
    stop_price DECIMAL(20,8) NOT NULL,
    target_price DECIMAL(20,8) NOT NULL,
    closed_at DATETIME(6) NULL,
    exit_price DECIMAL(20,8) NULL,
    gross_pnl DECIMAL(20,4) NOT NULL DEFAULT 0,
    fees DECIMAL(20,4) NOT NULL DEFAULT 0,
    slippage_cost DECIMAL(20,4) NOT NULL DEFAULT 0,
    net_pnl DECIMAL(20,4) NOT NULL DEFAULT 0,
    exit_reason VARCHAR(48) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_paper_trade_key UNIQUE (trade_key),
    CONSTRAINT fk_paper_trade_signal FOREIGN KEY (signal_id) REFERENCES trade_signals(signal_id),
    CONSTRAINT fk_paper_trade_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id),
    CONSTRAINT fk_paper_trade_strategy_version FOREIGN KEY (strategy_version_id) REFERENCES strategy_versions(strategy_version_id),
    CONSTRAINT chk_paper_trade_side CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT chk_paper_trade_status CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED')),
    CONSTRAINT chk_paper_trade_quantity CHECK (quantity > 0),
    CONSTRAINT chk_paper_trade_prices CHECK (entry_price > 0 AND stop_price > 0 AND target_price > 0
        AND (exit_price IS NULL OR exit_price > 0)),
    INDEX idx_paper_trades_status_opened (status, opened_at),
    INDEX idx_paper_trades_instrument_opened (instrument_id, opened_at)
) ENGINE=InnoDB;

INSERT INTO schema_version (version, description)
VALUES (1, 'Initial StockKing development schema for market data, strategies, backtests, signals and paper trades')
ON DUPLICATE KEY UPDATE description = VALUES(description);
