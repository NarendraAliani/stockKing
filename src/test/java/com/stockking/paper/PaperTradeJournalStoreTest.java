package com.stockking.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperTradeJournalStoreTest {
    @TempDir
    Path tempDir;

    private ObjectMapper mapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private PaperTradeLedger.Trade openTrade(String signalId) {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T07:00:00Z"), ZoneOffset.UTC);
        return new PaperTradeLedger(clock).open(new PaperTradeLedger.Signal(signalId, "strategy-a", "TEST",
                PaperTradeLedger.Side.BUY, clock.instant(), new BigDecimal("100"),
                new BigDecimal("95"), new BigDecimal("110"), 2));
    }

    @Test
    void missingJournalLoadsAsEmptyWithoutCreatingAFile() throws Exception {
        Path path = tempDir.resolve("journal.json");
        PaperTradeJournalStore store = new PaperTradeJournalStore(path, mapper());

        assertTrue(store.load().isEmpty());
        assertTrue(Files.notExists(path));
    }

    @Test
    void savesAndRestoresOpenAndClosedTrades() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T07:00:00Z"), ZoneOffset.UTC);
        PaperTradeLedger ledger = new PaperTradeLedger(clock);
        ledger.open(new PaperTradeLedger.Signal("open-1", "strategy-a", "TEST",
                PaperTradeLedger.Side.BUY, clock.instant(), new BigDecimal("100"),
                new BigDecimal("95"), new BigDecimal("110"), 2));
        ledger.open(new PaperTradeLedger.Signal("closed-1", "strategy-a", "TEST",
                PaperTradeLedger.Side.SELL, clock.instant(), new BigDecimal("100"),
                new BigDecimal("105"), new BigDecimal("90"), 1));
        ledger.close("closed-1", new BigDecimal("95"), "PAPER_TEST_EXIT");
        Path path = tempDir.resolve("nested").resolve("journal.json");
        PaperTradeJournalStore store = new PaperTradeJournalStore(path, mapper());

        store.save(ledger.allTrades());

        assertEquals(ledger.allTrades(), store.load());
        assertTrue(Files.exists(path));
    }

    @Test
    void duplicateTradeIdsAreRejectedBeforeWriting() {
        PaperTradeLedger.Trade first = openTrade("signal-1");
        PaperTradeLedger.Trade duplicateTradeId = new PaperTradeLedger.Trade(
                first.tradeId(), "signal-2", first.strategyId(), first.instrumentId(), first.side(),
                first.quantity(), first.entryPrice(), first.stopPrice(), first.targetPrice(), first.status(),
                first.openedAt(), first.closedAt(), first.exitPrice(), first.realizedGrossPnl(), first.lastReason());
        PaperTradeJournalStore store = new PaperTradeJournalStore(tempDir.resolve("journal.json"), mapper());

        assertThrows(IOException.class, () -> store.save(List.of(first, duplicateTradeId)));
        assertTrue(Files.notExists(tempDir.resolve("journal.json")));
    }

    @Test
    void serializationFailurePreservesLastKnownGoodJournalAndCleansTemporaryFile() throws Exception {
        Path path = tempDir.resolve("journal.json");
        String knownGood = """
                {"schemaVersion":1,"trades":[]}
                """;
        Files.writeString(path, knownGood);
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public void writeValue(java.io.File resultFile, Object value) throws IOException {
                throw new IOException("simulated serialization failure");
            }
        };
        PaperTradeJournalStore store = new PaperTradeJournalStore(path, failingMapper);

        assertThrows(IOException.class, () -> store.save(List.of(openTrade("new-trade"))));

        assertEquals(knownGood, Files.readString(path));
        try (var files = Files.list(tempDir)) {
            assertEquals(List.of(path), files.toList());
        }
    }

    @Test
    void malformedJournalFailsClosedAndIsNotReplaced() throws Exception {
        Path path = tempDir.resolve("journal.json");
        Files.writeString(path, "{not-json");
        PaperTradeJournalStore store = new PaperTradeJournalStore(path, mapper());

        IOException error = assertThrows(IOException.class, store::load);
        assertTrue(error.getMessage().contains("unreadable or malformed"));
        assertEquals("{not-json", Files.readString(path));
    }

    @Test
    void directoryAtJournalPathFailsWithActionableMessage() throws Exception {
        Path path = tempDir.resolve("journal-directory");
        Files.createDirectory(path);
        PaperTradeJournalStore store = new PaperTradeJournalStore(path, mapper());

        IOException error = assertThrows(IOException.class, store::load);
        assertEquals("Paper journal path is not a regular file", error.getMessage());
        assertTrue(Files.isDirectory(path));
    }

    @Test
    void oversizedJournalFailsClosedWithoutChangingFile() throws Exception {
        Path path = tempDir.resolve("journal.json");
        byte[] oversized = new byte[10 * 1024 * 1024 + 1];
        java.util.Arrays.fill(oversized, (byte) ' ');
        Files.write(path, oversized);
        PaperTradeJournalStore store = new PaperTradeJournalStore(path, mapper());

        IOException error = assertThrows(IOException.class, store::load);

        assertEquals("Paper journal exceeds the 10 MiB safety limit", error.getMessage());
        assertEquals(oversized.length, Files.size(path));
    }

    @Test
    void unsupportedSchemaVersionFailsClosed() throws Exception {
        Path path = tempDir.resolve("journal.json");
        Files.writeString(path, """
                {"schemaVersion":999,"trades":[]}
                """);
        PaperTradeJournalStore store = new PaperTradeJournalStore(path, mapper());

        assertThrows(IOException.class, store::load);
    }

    @Test
    void invalidTradePricesAreRejectedBeforeWriting() {
        PaperTradeLedger.Trade valid = openTrade("valid-price");
        PaperTradeLedger.Trade invalidPrice = new PaperTradeLedger.Trade(
                valid.tradeId(), valid.signalId(), valid.strategyId(), valid.instrumentId(), valid.side(),
                valid.quantity(), BigDecimal.ZERO, valid.stopPrice(), valid.targetPrice(), valid.status(),
                valid.openedAt(), valid.closedAt(), valid.exitPrice(), valid.realizedGrossPnl(), valid.lastReason());
        Path path = tempDir.resolve("journal.json");
        PaperTradeJournalStore store = new PaperTradeJournalStore(path, mapper());

        IOException error = assertThrows(IOException.class, () -> store.save(List.of(invalidPrice)));

        assertEquals("Paper journal contains a non-positive trade price", error.getMessage());
        assertTrue(Files.notExists(path));
    }

    @Test
    void duplicateSignalIdsAreRejectedBeforeWriting() {
        PaperTradeJournalStore store = new PaperTradeJournalStore(tempDir.resolve("journal.json"), mapper());

        assertThrows(IOException.class, () -> store.save(List.of(openTrade("duplicate"), openTrade("duplicate"))));
        assertTrue(Files.notExists(tempDir.resolve("journal.json")));
    }
}
