package com.stockking.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockking.paper.PaperTradeLedger;
import com.stockking.risk.PaperRiskGuard;
import com.stockking.risk.RiskPolicy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PaperDashboardControllerTest {
    @Test
    void dashboardIsReadOnlyAndReportsSafePaperDefaults() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T07:00:00Z"), ZoneOffset.UTC);
        PaperTradeLedger ledger = new PaperTradeLedger(clock);
        PaperRiskGuard guard = new PaperRiskGuard(RiskPolicy.initialDefaults(),
                new BigDecimal("100000"), clock, ZoneId.of("Asia/Kolkata"));
        PaperDashboardController controller = new PaperDashboardController(ledger, guard);

        var dashboard = controller.dashboard();

        assertEquals("paper", dashboard.tradingMode());
        assertFalse(dashboard.liveTradingEnabled());
        assertEquals(0, dashboard.totalTrades());
        assertEquals(0, dashboard.openTrades());
        assertEquals(0, dashboard.closedTrades());
        assertEquals(0, dashboard.entriesUsedToday());
        assertEquals(BigDecimal.ZERO, dashboard.dailyRealizedGrossPnl());
        assertTrue(dashboard.entriesAllowed());
        assertEquals("NONE", dashboard.riskStatus());
    }

    @Test
    void tradesEndpointReturnsAnImmutableEmptySnapshot() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T07:00:00Z"), ZoneOffset.UTC);
        PaperDashboardController controller = new PaperDashboardController(
                new PaperTradeLedger(clock),
                new PaperRiskGuard(RiskPolicy.initialDefaults(), new BigDecimal("100000"),
                        clock, ZoneId.of("Asia/Kolkata")));

        assertTrue(controller.trades().isEmpty());
    }
}
