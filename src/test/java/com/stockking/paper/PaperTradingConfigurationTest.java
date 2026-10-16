package com.stockking.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperTradingConfigurationTest {
    private static final Instant NOW = Instant.parse("2026-10-16T07:00:00Z");

    @TempDir
    Path tempDir;

    private ObjectMapper mapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private PaperTradeLedger.Signal signal(String id) {
        return new PaperTradeLedger.Signal(id, "strategy-config-test", "TEST",
                PaperTradeLedger.Side.BUY, NOW, new BigDecimal("100"),
                new BigDecimal("95"), new BigDecimal("110"), 1);
    }

    @Test
    void disabledPersistenceDoesNotReadOrModifyConfiguredJournal() throws Exception {
        Path path = tempDir.resolve("journal.json");
        String corruptContent = "{not-json";
        Files.writeString(path, corruptContent);
        PaperTradingConfiguration configuration = new PaperTradingConfiguration();

        PaperTradeLedger ledger = configuration.paperTradeLedger(
                Clock.fixed(NOW, ZoneOffset.UTC), mapper(), false, path.toString());

        assertTrue(ledger.allTrades().isEmpty());
        assertEquals(corruptContent, Files.readString(path));
    }

    @Test
    void enabledPersistenceLoadsExistingJournalThroughConfiguration() throws Exception {
        Path path = tempDir.resolve("journal.json");
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        PaperTradeLedger original = new PaperTradeLedger(clock);
        original.open(signal("configured-restore"));
        new PaperTradeJournalStore(path, mapper()).save(original.allTrades());
        PaperTradingConfiguration configuration = new PaperTradingConfiguration();

        PaperTradeLedger restored = configuration.paperTradeLedger(
                clock, mapper(), true, path.toString());

        assertEquals(1, restored.allTrades().size());
        assertEquals("configured-restore", restored.allTrades().getFirst().signalId());
    }

    @Test
    void enabledPersistenceFailsClosedForCorruptJournal() throws Exception {
        Path path = tempDir.resolve("journal.json");
        Files.writeString(path, "{not-json");
        PaperTradingConfiguration configuration = new PaperTradingConfiguration();

        assertThrows(java.io.IOException.class, () -> configuration.paperTradeLedger(
                Clock.fixed(NOW, ZoneOffset.UTC), mapper(), true, path.toString()));
        assertEquals("{not-json", Files.readString(path));
    }
}
