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

        assertTrue(controller.trades(null, null, null).isEmpty());
        assertTrue(controller.trades(PaperTradeLedger.Status.OPEN, null, null).isEmpty());
        assertTrue(controller.trades(PaperTradeLedger.Status.CLOSED, null, null).isEmpty());
    }

    @Test
    void tradesEndpointCanFilterByOpenOrClosedStatus() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T07:00:00Z"), ZoneOffset.UTC);
        PaperTradeLedger ledger = new PaperTradeLedger(clock);
        ledger.open(new PaperTradeLedger.Signal("open-1", "strategy-a", "TEST",
                PaperTradeLedger.Side.BUY, clock.instant(), new BigDecimal("100"),
                new BigDecimal("95"), new BigDecimal("110"), 1));
        ledger.open(new PaperTradeLedger.Signal("closed-1", "strategy-a", "TEST",
                PaperTradeLedger.Side.SELL, clock.instant(), new BigDecimal("100"),
                new BigDecimal("105"), new BigDecimal("90"), 1));
        ledger.close("closed-1", new BigDecimal("95"), "TEST_EXIT");
        PaperDashboardController controller = new PaperDashboardController(ledger,
                new PaperRiskGuard(RiskPolicy.initialDefaults(), new BigDecimal("100000"),
                        clock, ZoneId.of("Asia/Kolkata")));

        assertEquals(1, controller.trades(PaperTradeLedger.Status.OPEN, null, null).size());
        assertEquals("open-1", controller.trades(PaperTradeLedger.Status.OPEN, null, null).get(0).signalId());
        assertEquals(1, controller.trades(PaperTradeLedger.Status.CLOSED, null, null).size());
        assertEquals("closed-1", controller.trades(PaperTradeLedger.Status.CLOSED, null, null).get(0).signalId());
        assertEquals(2, controller.trades(null, null, null).size());
        assertEquals(1, controller.trades(null, PaperTradeLedger.Side.BUY, null).size());
        assertEquals(1, controller.trades(null, PaperTradeLedger.Side.SELL, null).size());
        assertEquals(2, controller.trades(null, null, "test").size());
        assertEquals(0, controller.trades(null, null, "OTHER").size());
        assertEquals(1, controller.trades(PaperTradeLedger.Status.CLOSED,
                PaperTradeLedger.Side.SELL, " TEST ").size());
    }

    @Test
    void tradeDetailEndpointReturnsKnownTradeAndNotFoundForUnknownSignal() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T07:00:00Z"), ZoneOffset.UTC);
        PaperTradeLedger ledger = new PaperTradeLedger(clock);
        ledger.open(new PaperTradeLedger.Signal("detail-1", "strategy-a", "TEST",
                PaperTradeLedger.Side.BUY, clock.instant(), new BigDecimal("100"),
                new BigDecimal("95"), new BigDecimal("110"), 1));
        PaperDashboardController controller = new PaperDashboardController(ledger,
                new PaperRiskGuard(RiskPolicy.initialDefaults(), new BigDecimal("100000"),
                        clock, ZoneId.of("Asia/Kolkata")));

        var found = controller.tradeBySignalId("detail-1");
        var missing = controller.tradeBySignalId("missing");

        assertEquals(200, found.getStatusCode().value());
        assertEquals("detail-1", found.getBody().signalId());
        assertEquals(404, missing.getStatusCode().value());
    }

    @Test
    void riskEndpointIsReadOnlyAndReportsLiveTradingDisabled() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T07:00:00Z"), ZoneOffset.UTC);
        PaperDashboardController controller = new PaperDashboardController(
                new PaperTradeLedger(clock),
                new PaperRiskGuard(RiskPolicy.initialDefaults(), new BigDecimal("100000"),
                        clock, ZoneId.of("Asia/Kolkata")));

        var risk = controller.risk();

        assertTrue(risk.entriesAllowed());
        assertEquals("NONE", risk.riskStatus());
        assertEquals(0, risk.entriesUsedToday());
        assertEquals(BigDecimal.ZERO, risk.dailyRealizedGrossPnl());
        assertFalse(risk.liveTradingEnabled());
    }
}
