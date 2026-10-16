package com.stockking.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PaperTradeLedgerTest {
    private static final Instant NOW = Instant.parse("2026-10-15T07:00:00Z");

    private PaperTradeLedger ledger() {
        return new PaperTradeLedger(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private PaperTradeLedger.Signal buy(String id) {
        return new PaperTradeLedger.Signal(id, "strategy-a", "TEST", PaperTradeLedger.Side.BUY,
                NOW, new BigDecimal("100"), new BigDecimal("95"), new BigDecimal("110"), 10);
    }

    @Test
    void opensPaperTradeAndDuplicateSignalDoesNotCreateAnother() {
        var ledger = ledger();
        var first = ledger.open(buy("signal-1"));
        var duplicate = ledger.open(buy("signal-1"));
        assertEquals("PAPER-signal-1", first.tradeId());
        assertEquals(first, duplicate);
        assertEquals(1, ledger.allTrades().size());
    }

    @Test
    void closesBuyAndComputesGrossPnl() {
        var ledger = ledger();
        ledger.open(buy("signal-2"));
        var closed = ledger.close("signal-2", new BigDecimal("108"), "TARGET");
        assertEquals(PaperTradeLedger.Status.CLOSED, closed.status());
        assertEquals(0, closed.realizedGrossPnl().compareTo(new BigDecimal("80")));
        assertEquals(NOW, closed.closedAt());
    }

    @Test
    void duplicateCloseDoesNotChangeExistingTrade() {
        var ledger = ledger();
        ledger.open(buy("signal-3"));
        var first = ledger.close("signal-3", new BigDecimal("108"), "TARGET");
        var repeated = ledger.close("signal-3", new BigDecimal("90"), "REPLAY");
        assertEquals(first, repeated);
    }

    @Test
    void rejectsDirectionallyInvalidStopAndTarget() {
        var ledger = ledger();
        var bad = new PaperTradeLedger.Signal("bad", "strategy-a", "TEST", PaperTradeLedger.Side.BUY,
                NOW, new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("110"), 1);
        assertThrows(IllegalArgumentException.class, () -> ledger.open(bad));
        assertEquals(0, ledger.allTrades().size());
    }

    @Test
    void computesSellPnlWithInverseDirection() {
        var ledger = ledger();
        var sell = new PaperTradeLedger.Signal("sell-1", "strategy-a", "TEST", PaperTradeLedger.Side.SELL,
                NOW, new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("90"), 2);
        ledger.open(sell);
        var closed = ledger.close("sell-1", new BigDecimal("95"), "MANUAL_PAPER_EXIT");
        assertEquals(0, closed.realizedGrossPnl().compareTo(new BigDecimal("10")));
    }
}
