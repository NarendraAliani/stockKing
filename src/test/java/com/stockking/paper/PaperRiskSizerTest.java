package com.stockking.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockking.risk.RiskPolicy;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaperRiskSizerTest {
    private final RiskPolicy policy = RiskPolicy.initialDefaults();

    @Test
    void sizesDownToWholeLotsWithinRiskAndCapitalBudgets() {
        var request = new PaperRiskSizer.Request(
                new BigDecimal("100000"),
                new BigDecimal("100"),
                new BigDecimal("95"),
                new BigDecimal("1.0"),
                new BigDecimal("50"),
                10);

        var result = PaperRiskSizer.size(request, policy);

        assertTrue(result.accepted());
        assertEquals(200, result.quantity());
        assertEquals(new BigDecimal("1000.0"), result.estimatedRisk());
        assertEquals(new BigDecimal("20000"), result.capitalRequired());
    }

    @Test
    void rejectsRiskPercentageAboveConfiguredPolicy() {
        var request = new PaperRiskSizer.Request(
                new BigDecimal("100000"), new BigDecimal("100"), new BigDecimal("95"),
                new BigDecimal("1.1"), new BigDecimal("50"), 1);

        var result = PaperRiskSizer.size(request, policy);

        assertFalse(result.accepted());
        assertEquals("RISK_PERCENT_OUT_OF_POLICY", result.reason());
        assertEquals(0, result.quantity());
    }

    @Test
    void rejectsStopEqualToEntry() {
        var request = new PaperRiskSizer.Request(
                new BigDecimal("100000"), new BigDecimal("100"), new BigDecimal("100"),
                new BigDecimal("1.0"), new BigDecimal("50"), 1);

        var result = PaperRiskSizer.size(request, policy);

        assertFalse(result.accepted());
        assertEquals("STOP_EQUALS_ENTRY", result.reason());
    }

    @Test
    void rejectsWhenBudgetCannotBuyOneWholeLot() {
        var request = new PaperRiskSizer.Request(
                new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("499"),
                new BigDecimal("1.0"), new BigDecimal("50"), 10);

        var result = PaperRiskSizer.size(request, policy);

        assertFalse(result.accepted());
        assertEquals("RISK_OR_CAPITAL_BUDGET_TOO_SMALL", result.reason());
    }

    @Test
    void rejectsMissingInputs() {
        var request = new PaperRiskSizer.Request(
                new BigDecimal("100000"), new BigDecimal("100"), null,
                new BigDecimal("1.0"), new BigDecimal("50"), 1);

        var result = PaperRiskSizer.size(request, policy);

        assertFalse(result.accepted());
        assertEquals("MISSING_INPUT", result.reason());
    }
}
