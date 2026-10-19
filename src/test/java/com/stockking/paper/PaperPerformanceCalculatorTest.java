package com.stockking.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaperPerformanceCalculatorTest {
    private static final Instant NOW = Instant.parse("2026-10-15T07:00:00Z");

    private PaperTradeLedger.Trade trade(String id, PaperTradeLedger.Status status,
            String pnl, String closedAt) {
        Instant closed = closedAt == null ? null : Instant.parse(closedAt);
        return new PaperTradeLedger.Trade("PAPER-" + id, id, "test-strategy", "TEST",
                PaperTradeLedger.Side.BUY, 1, new BigDecimal("100"), new BigDecimal("95"),
                new BigDecimal("110"), status, NOW, closed,
                closed == null ? null : new BigDecimal("100"),
                new BigDecimal(pnl), status == PaperTradeLedger.Status.CLOSED ? "TEST_EXIT" : "PAPER_OPEN");
    }

    @Test
    void summarizesClosedTradesAndIgnoresOpenTrades() {
        var calculator = new PaperPerformanceCalculator();
        var summary = calculator.summarize(List.of(
                trade("open", PaperTradeLedger.Status.OPEN, "0", null),
                trade("win", PaperTradeLedger.Status.CLOSED, "50", "2026-10-15T07:01:00Z"),
                trade("loss", PaperTradeLedger.Status.CLOSED, "-20", "2026-10-15T07:02:00Z"),
                trade("flat", PaperTradeLedger.Status.CLOSED, "0", "2026-10-15T07:03:00Z")));

        assertEquals(3, summary.closedTrades());
        assertEquals(1, summary.winningTrades());
        assertEquals(1, summary.losingTrades());
        assertEquals(1, summary.breakevenTrades());
        assertEquals(0, summary.winRatePercent().compareTo(new BigDecimal("33.33")));
        assertEquals(0, summary.netRealizedGrossPnl().compareTo(new BigDecimal("30")));
        assertEquals(0, summary.maxDrawdownGross().compareTo(new BigDecimal("20")));
    }

    @Test
    void emptyLedgerReturnsZeroStatistics() {
        var summary = new PaperPerformanceCalculator().summarize(List.of());
        assertEquals(0, summary.closedTrades());
        assertEquals(0, summary.winningTrades());
        assertEquals(0, summary.losingTrades());
        assertEquals(0, summary.breakevenTrades());
        assertEquals(BigDecimal.ZERO, summary.winRatePercent());
        assertEquals(BigDecimal.ZERO, summary.netRealizedGrossPnl());
        assertEquals(BigDecimal.ZERO, summary.maxDrawdownGross());
    }

    @Test
    void drawdownUsesClosedTradeChronologicalOrder() {
        var summary = new PaperPerformanceCalculator().summarize(List.of(
                trade("later-loss", PaperTradeLedger.Status.CLOSED, "-30", "2026-10-15T07:03:00Z"),
                trade("first-win", PaperTradeLedger.Status.CLOSED, "100", "2026-10-15T07:01:00Z"),
                trade("second-loss", PaperTradeLedger.Status.CLOSED, "-40", "2026-10-15T07:02:00Z")));

        assertEquals(0, summary.netRealizedGrossPnl().compareTo(new BigDecimal("30")));
        assertEquals(0, summary.maxDrawdownGross().compareTo(new BigDecimal("70")));
    }
}
