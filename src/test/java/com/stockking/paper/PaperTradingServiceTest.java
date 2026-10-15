package com.stockking.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockking.risk.PaperRiskGuard;
import com.stockking.risk.RiskPolicy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PaperTradingServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-15T07:00:00Z");

    private PaperTradingService service() {
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        var ledger = new PaperTradeLedger(clock);
        var guard = new PaperRiskGuard(RiskPolicy.initialDefaults(), new BigDecimal("100000"),
                clock, ZoneId.of("Asia/Kolkata"));
        return new PaperTradingService(ledger, guard);
    }

    private PaperTradeLedger.Signal buy(String id) {
        return new PaperTradeLedger.Signal(id, "strategy-a", "TEST", PaperTradeLedger.Side.BUY,
                NOW, new BigDecimal("100"), new BigDecimal("95"), new BigDecimal("110"), 1);
    }

    @Test
    void duplicateSignalDoesNotConsumeAnotherEntry() {
        var service = service();
        var first = service.open(buy("same"));
        var duplicate = service.open(buy("same"));
        assertEquals(first, duplicate);
        assertEquals(1, service.riskSnapshot().entriesUsed());
    }

    @Test
    void invalidSignalDoesNotConsumeEntry() {
        var service = service();
        var invalid = new PaperTradeLedger.Signal("bad", "strategy-a", "TEST", PaperTradeLedger.Side.BUY,
                NOW, new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("110"), 1);
        assertThrows(IllegalArgumentException.class, () -> service.open(invalid));
        assertEquals(0, service.riskSnapshot().entriesUsed());
    }

    @Test
    void closeRecordsPnlOnlyOnceAndStopsAfterConsecutiveLosses() {
        var service = service();
        service.open(buy("one"));
        service.close("one", new BigDecimal("90"), "TEST_LOSS");
        service.close("one", new BigDecimal("80"), "DUPLICATE_CLOSE");
        service.open(buy("two"));
        service.close("two", new BigDecimal("90"), "TEST_LOSS");
        var rejected = assertThrows(PaperTradingService.RiskRejectedException.class,
                () -> service.open(buy("three")));
        assertTrue(rejected.getMessage().contains("CONSECUTIVE_LOSS_LIMIT"));
        assertEquals(new BigDecimal("-20"), service.riskSnapshot().dailyPnl());
    }
}
