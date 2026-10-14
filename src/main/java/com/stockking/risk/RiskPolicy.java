package com.stockking.risk;

import java.math.BigDecimal;

/** Immutable risk limits for the initial paper-trading phase. */
public record RiskPolicy(
        BigDecimal riskPerTradePercent,
        BigDecimal maxDailyLossPercent,
        int maxConsecutiveLosses,
        int maxEntriesPerDay,
        boolean liveTradingEnabled) {

    public RiskPolicy {
        if (riskPerTradePercent == null || riskPerTradePercent.signum() <= 0
                || riskPerTradePercent.compareTo(new BigDecimal("1.0")) > 0) {
            throw new IllegalArgumentException("Risk per trade must be greater than 0 and at most 1.0 percent");
        }
        if (maxDailyLossPercent == null || maxDailyLossPercent.signum() <= 0) {
            throw new IllegalArgumentException("Maximum daily loss must be positive");
        }
        if (maxConsecutiveLosses < 1 || maxEntriesPerDay < 1) {
            throw new IllegalArgumentException("Loss and entry limits must be at least 1");
        }
        if (liveTradingEnabled) {
            throw new IllegalArgumentException("Live trading is disabled in the initial StockKing baseline");
        }
    }

    public static RiskPolicy initialDefaults() {
        return new RiskPolicy(new BigDecimal("1.0"), new BigDecimal("2.0"), 2, 3, false);
    }
}
