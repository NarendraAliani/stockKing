package com.stockking.paper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Local, versioned paper-trade journal storage. This store is deliberately not wired into
 * application startup or the ledger until persistence is explicitly enabled and integration-tested.
 */
public final class PaperTradeJournalStore {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final long MAX_JOURNAL_BYTES = 10 * 1024 * 1024;

    private final Path journalPath;
    private final ObjectMapper objectMapper;

    public PaperTradeJournalStore(Path journalPath, ObjectMapper objectMapper) {
        this.journalPath = Objects.requireNonNull(journalPath, "journalPath")
                .toAbsolutePath().normalize();
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Loads an existing journal. A missing file represents an empty journal; callers must decide
     * whether that is acceptable for their configuration. Invalid journals are never overwritten.
     */
    public synchronized List<PaperTradeLedger.Trade> load() throws IOException {
        if (!Files.exists(journalPath)) return List.of();
        long size = Files.size(journalPath);
        if (size > MAX_JOURNAL_BYTES) {
            throw new IOException("Paper journal exceeds the 10 MiB safety limit");
        }
        JournalDocument document = objectMapper.readValue(journalPath.toFile(), JournalDocument.class);
        if (document == null || document.schemaVersion() != CURRENT_SCHEMA_VERSION) {
            throw new IOException("Unsupported or missing paper journal schema version");
        }
        List<PaperTradeLedger.Trade> trades = document.trades();
        validateTrades(trades);
        return List.copyOf(trades);
    }

    /**
     * Writes a complete snapshot to a sibling temporary file and replaces the target only after
     * serialization and disk flush succeed. The previous journal is retained on pre-move failures.
     */
    public synchronized void save(List<PaperTradeLedger.Trade> trades) throws IOException {
        validateTrades(trades);
        Path parent = journalPath.getParent();
        if (parent == null) throw new IOException("Paper journal must have a parent directory");
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, journalPath.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            objectMapper.writeValue(temporary.toFile(),
                    new JournalDocument(CURRENT_SCHEMA_VERSION, List.copyOf(trades)));
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            try {
                Files.move(temporary, journalPath, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, journalPath, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) Files.deleteIfExists(temporary);
        }
    }

    private static void validateTrades(List<PaperTradeLedger.Trade> trades) throws IOException {
        if (trades == null) throw new IOException("Paper journal trades list is missing");
        Set<String> signalIds = new HashSet<>();
        for (PaperTradeLedger.Trade trade : trades) {
            if (trade == null || trade.signalId() == null || trade.signalId().isBlank()
                    || trade.tradeId() == null || trade.tradeId().isBlank()
                    || trade.strategyId() == null || trade.strategyId().isBlank()
                    || trade.instrumentId() == null || trade.instrumentId().isBlank()
                    || trade.side() == null || trade.status() == null
                    || trade.quantity() < 1 || trade.entryPrice() == null
                    || trade.stopPrice() == null || trade.targetPrice() == null
                    || trade.openedAt() == null) {
                throw new IOException("Paper journal contains an invalid trade record");
            }
            if (!signalIds.add(trade.signalId())) {
                throw new IOException("Paper journal contains duplicate signal IDs");
            }
            if (trade.status() == PaperTradeLedger.Status.CLOSED
                    && (trade.closedAt() == null || trade.exitPrice() == null
                    || trade.realizedGrossPnl() == null)) {
                throw new IOException("Closed paper trade is missing close details");
            }
            if (trade.status() == PaperTradeLedger.Status.OPEN && trade.closedAt() != null) {
                throw new IOException("Open paper trade unexpectedly has a close timestamp");
            }
        }
    }

    public record JournalDocument(int schemaVersion, List<PaperTradeLedger.Trade> trades) {}
}
