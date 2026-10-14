package com.stockking.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RiskPolicyTest {
    @Test
    void initialDefaultsArePaperOnlyAndConservative() {
        RiskPolicy policy = RiskPolicy.initialDefaults();
        assertEquals(new BigDecimal("1.0"), policy.riskPerTradePercent());
        assertEquals(new BigDecimal("2.0"), policy.maxDailyLossPercent());
        assertEquals(2, policy.maxConsecutiveLosses());
        assertEquals(3, policy.maxEntriesPerDay());
    }

    @Test
    void rejectsRiskAboveOnePercent() {
        assertThrows(IllegalArgumentException.class, () -> new RiskPolicy(
                new BigDecimal("1.01"), new BigDecimal("2.0"), 2, 3, false));
    }

    @Test
    void refusesLiveTradingPolicy() {
        assertThrows(IllegalArgumentException.class, () -> new RiskPolicy(
                new BigDecimal("1.0"), new BigDecimal("2.0"), 2, 3, true));
    }
}
