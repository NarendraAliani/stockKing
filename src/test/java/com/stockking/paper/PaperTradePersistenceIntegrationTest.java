package com.stockking.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockking.risk.PaperRiskGuard;
import com.stockking.risk.RiskPolicy;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperTradePersistenceIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-10-16T07:00:00Z");

    @TempDir
    Path tempDir;

    private ObjectMapper mapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private PaperTradeLedger ledger(Path path, Clock clock) throws Exception {
        return new PaperTradeLedger(clock, new PaperTradeJournalStore(path, mapper()));
    }

    private PaperTradeLedger.Signal buy(String id) {
        return new PaperTradeLedger.Signal(id, "strategy-a", "TEST", PaperTradeLedger.Side.BUY,
                NOW, new BigDecimal("100"), new BigDecimal("95"), new BigDecimal("110"), 1);
    }

    @Test
    void ledgerRestoresTradesAndPersistsLifecycleChanges() throws Exception {
        Path path = tempDir.resolve("paper-journal.json");
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        PaperTradeLedger first = ledger(path, clock);
        first.open(buy("persist-1"));
        first.close("persist-1", new BigDecimal("108"), "TEST_CLOSE");

        PaperTradeLedger restarted = ledger(path, clock);

        assertEquals(1, restarted.allTrades().size());
        assertEquals(PaperTradeLedger.Status.CLOSED, restarted.findBySignalId("persist-1").orElseThrow().status());
        assertEquals(new BigDecimal("8"), restarted.findBySignalId("persist-1").orElseThrow().realizedGrossPnl());
    }

    @Test
    void failedJournalWriteDoesNotMutateLedger() throws Exception {
        Path path = tempDir.resolve("missing-parent").resolve("paper-journal.json");
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        PaperTradeJournalStore store = new PaperTradeJournalStore(path, mapper());
        PaperTradeLedger ledger = new PaperTradeLedger(clock, store);
        java.nio.file.Files.createDirectory(path.getParent());
        java.nio.file.Files.createDirectory(path);
        PaperRiskGuard guard = new PaperRiskGuard(RiskPolicy.initialDefaults(), new BigDecimal("100000"),
                clock, ZoneId.of("Asia/Kolkata"));
        PaperTradingService service = new PaperTradingService(ledger, guard);

        assertThrows(IllegalStateException.class, () -> service.open(buy("write-failure")));
        assertEquals(0, ledger.allTrades().size());
        assertEquals(0, guard.snapshot().entriesUsed());
    }

    @Test
    void riskCountersCanBeReconstructedFromRestoredTrades() throws Exception {
        Path path = tempDir.resolve("risk-journal.json");
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        PaperTradeLedger ledger = ledger(path, clock);
        ledger.open(buy("loss-1"));
        ledger.close("loss-1", new BigDecimal("90"), "TEST_LOSS");
        ledger.open(buy("loss-2"));
        ledger.close("loss-2", new BigDecimal("90"), "TEST_LOSS");
        PaperTradeLedger restored = ledger(path, clock);
        PaperRiskGuard guard = new PaperRiskGuard(RiskPolicy.initialDefaults(), new BigDecimal("100000"),
                clock, ZoneId.of("Asia/Kolkata"));

        guard.restoreFromTrades(restored.allTrades());

        assertEquals(2, guard.snapshot().entriesUsed());
        assertEquals(new BigDecimal("-20"), guard.snapshot().dailyPnl());
        assertEquals(PaperRiskGuard.Rejection.CONSECUTIVE_LOSS_LIMIT, guard.snapshot().rejection());
    }
}
