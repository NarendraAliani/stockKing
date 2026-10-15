package com.stockking.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PaperRiskGuardTest {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-15T07:00:00Z"), ZoneOffset.UTC);

    private PaperRiskGuard guard() {
        return new PaperRiskGuard(RiskPolicy.initialDefaults(), new BigDecimal("100000"), CLOCK, IST);
    }

    @Test
    void allowsOnlyConfiguredNumberOfEntriesPerDay() {
        var guard = guard();
        assertTrue(guard.authorizeEntry().allowed());
        assertTrue(guard.authorizeEntry().allowed());
        assertTrue(guard.authorizeEntry().allowed());
        var rejected = guard.authorizeEntry();
        assertFalse(rejected.allowed());
        assertEquals(PaperRiskGuard.Rejection.DAILY_ENTRY_LIMIT, rejected.rejection());
        assertEquals(3, rejected.entriesUsed());
    }

    @Test
    void blocksEntriesAtDailyLossThreshold() {
        var guard = guard();
        guard.recordClosedTrade(new BigDecimal("-2000"));
        var result = guard.authorizeEntry();
        assertFalse(result.allowed());
        assertEquals(PaperRiskGuard.Rejection.DAILY_LOSS_LIMIT, result.rejection());
    }

    @Test
    void blocksAfterConfiguredConsecutiveLosses() {
        var guard = guard();
        guard.recordClosedTrade(new BigDecimal("-100"));
        guard.recordClosedTrade(new BigDecimal("-100"));
        var result = guard.authorizeEntry();
        assertFalse(result.allowed());
        assertEquals(PaperRiskGuard.Rejection.CONSECUTIVE_LOSS_LIMIT, result.rejection());
    }

    @Test
    void winningTradeResetsConsecutiveLossCount() {
        var guard = guard();
        guard.recordClosedTrade(new BigDecimal("-100"));
        guard.recordClosedTrade(new BigDecimal("50"));
        assertTrue(guard.authorizeEntry().allowed());
    }

    @Test
    void rejectsNullPnl() {
        var guard = guard();
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> guard.recordClosedTrade(null));
    }
}
