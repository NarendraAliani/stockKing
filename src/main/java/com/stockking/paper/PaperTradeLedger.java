package com.stockking.paper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Thread-safe paper ledger with optional local persistence and no broker/database/network integration. */
public final class PaperTradeLedger {
    public enum Side { BUY, SELL }
    public enum Status { OPEN, CLOSED }

    public record Signal(String signalId, String strategyId, String instrumentId, Side side, Instant observedAt,
            BigDecimal entryPrice, BigDecimal stopPrice, BigDecimal targetPrice, int quantity) {}

    public record Trade(String tradeId, String signalId, String strategyId, String instrumentId, Side side,
            int quantity, BigDecimal entryPrice, BigDecimal stopPrice, BigDecimal targetPrice, Status status,
            Instant openedAt, Instant closedAt, BigDecimal exitPrice, BigDecimal realizedGrossPnl, String lastReason) {}

    private final Clock clock;
    private final PaperTradeJournalStore journalStore;
    private final Map<String, Trade> tradesBySignalId = new LinkedHashMap<>();

    public PaperTradeLedger(Clock clock) {
        this(clock, null);
    }

    /**
     * Loads the existing journal when a store is supplied. Corrupt journals fail construction
     * rather than starting with an empty ledger and risking accidental history loss.
     */
    public PaperTradeLedger(Clock clock, PaperTradeJournalStore journalStore) throws IOException {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.journalStore = journalStore;
        if (journalStore != null) {
            for (Trade trade : journalStore.load()) {
                if (tradesBySignalId.putIfAbsent(trade.signalId(), trade) != null) {
                    throw new IOException("Paper journal contains duplicate signal IDs");
                }
            }
        }
    }

    public synchronized Trade open(Signal signal) {
        validateSignal(signal);
        Trade existing = tradesBySignalId.get(signal.signalId());
        if (existing != null) return existing;
        Trade trade = new Trade("PAPER-" + signal.signalId(), signal.signalId(), signal.strategyId(),
                signal.instrumentId(), signal.side(), signal.quantity(), signal.entryPrice(), signal.stopPrice(),
                signal.targetPrice(), Status.OPEN, clock.instant(), null, null, BigDecimal.ZERO, "PAPER_OPEN");
        persistCandidate(trade);
        tradesBySignalId.put(signal.signalId(), trade);
        return trade;
    }

    public synchronized Trade close(String signalId, BigDecimal exitPrice, String reason) {
        if (signalId == null || signalId.isBlank()) throw new IllegalArgumentException("Signal ID is required");
        if (exitPrice == null || exitPrice.signum() <= 0) throw new IllegalArgumentException("Exit price must be positive");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Close reason is required");
        Trade current = tradesBySignalId.get(signalId);
        if (current == null) throw new IllegalArgumentException("Unknown paper trade");
        if (current.status() == Status.CLOSED) return current;
        BigDecimal movement = exitPrice.subtract(current.entryPrice());
        BigDecimal signedMovement = current.side() == Side.BUY ? movement : movement.negate();
        BigDecimal pnl = signedMovement.multiply(BigDecimal.valueOf(current.quantity()));
        Trade closed = new Trade(current.tradeId(), current.signalId(), current.strategyId(), current.instrumentId(),
                current.side(), current.quantity(), current.entryPrice(), current.stopPrice(), current.targetPrice(),
                Status.CLOSED, current.openedAt(), clock.instant(), exitPrice, pnl, reason);
        persistCandidate(closed);
        tradesBySignalId.put(signalId, closed);
        return closed;
    }

    private void persistCandidate(Trade candidate) {
        if (journalStore == null) return;
        List<Trade> snapshot = new ArrayList<>(tradesBySignalId.values());
        if (tradesBySignalId.containsKey(candidate.signalId())) {
            snapshot.replaceAll(trade -> trade.signalId().equals(candidate.signalId()) ? candidate : trade);
        } else {
            snapshot.add(candidate);
        }
        try {
            journalStore.save(snapshot);
        } catch (IOException exception) {
            throw new IllegalStateException("Paper journal write failed; ledger change was not applied", exception);
        }
    }

    public synchronized Optional<Trade> findBySignalId(String signalId) {
        return Optional.ofNullable(tradesBySignalId.get(signalId));
    }

    public synchronized List<Trade> allTrades() {
        return List.copyOf(tradesBySignalId.values());
    }

    public static void validateSignal(Signal signal) {
        Objects.requireNonNull(signal, "signal");
        if (signal.signalId() == null || signal.signalId().isBlank()
                || signal.strategyId() == null || signal.strategyId().isBlank()
                || signal.instrumentId() == null || signal.instrumentId().isBlank()) {
            throw new IllegalArgumentException("Signal, strategy, and instrument IDs are required");
        }
        if (signal.side() == null || signal.observedAt() == null) {
            throw new IllegalArgumentException("Signal side and observation time are required");
        }
        if (signal.entryPrice() == null || signal.stopPrice() == null || signal.targetPrice() == null
                || signal.entryPrice().signum() <= 0 || signal.stopPrice().signum() <= 0
                || signal.targetPrice().signum() <= 0) {
            throw new IllegalArgumentException("Entry, stop, and target prices must be positive");
        }
        if (signal.quantity() < 1) throw new IllegalArgumentException("Quantity must be positive");
        boolean validStops = signal.side() == Side.BUY
                ? signal.stopPrice().compareTo(signal.entryPrice()) < 0 && signal.targetPrice().compareTo(signal.entryPrice()) > 0
                : signal.stopPrice().compareTo(signal.entryPrice()) > 0 && signal.targetPrice().compareTo(signal.entryPrice()) < 0;
        if (!validStops) throw new IllegalArgumentException("Stop and target must be consistent with trade side");
    }
}
