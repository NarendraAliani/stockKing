package com.stockking.paper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Thread-safe in-memory paper-trade ledger. It deliberately has no broker,
 * database, or network integration and is not a live execution adapter.
 */
public final class PaperTradeLedger {
    public enum Side { BUY, SELL }
    public enum Status { OPEN, CLOSED }

    public record Signal(
            String signalId,
            String strategyId,
            String instrumentId,
            Side side,
            Instant observedAt,
            BigDecimal entryPrice,
            BigDecimal stopPrice,
            BigDecimal targetPrice,
            int quantity) {}

    public record Trade(
            String tradeId,
            String signalId,
            String strategyId,
            String instrumentId,
            Side side,
            int quantity,
            BigDecimal entryPrice,
            BigDecimal stopPrice,
            BigDecimal targetPrice,
            Status status,
            Instant openedAt,
            Instant closedAt,
            BigDecimal exitPrice,
            BigDecimal realizedGrossPnl,
            String lastReason) {}

    private final Clock clock;
    private final Map<String, Trade> tradesBySignalId = new LinkedHashMap<>();

    public PaperTradeLedger(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Opens a paper trade at the supplied reference price. Repeated signal IDs
     * return the original trade, preventing duplicate opens.
     */
    public synchronized Trade open(Signal signal) {
        validate(signal);
        Trade existing = tradesBySignalId.get(signal.signalId());
        if (existing != null) {
            return existing;
        }
        Instant now = clock.instant();
        Trade trade = new Trade(
                "PAPER-" + signal.signalId(),
                signal.signalId(),
                signal.strategyId(),
                signal.instrumentId(),
                signal.side(),
                signal.quantity(),
                signal.entryPrice(),
                signal.stopPrice(),
                signal.targetPrice(),
                Status.OPEN,
                now,
                null,
                null,
                BigDecimal.ZERO,
                "PAPER_OPEN");
        tradesBySignalId.put(signal.signalId(), trade);
        return trade;
    }

    /**
     * Closes an open trade at an explicitly supplied simulated exit price.
     * Duplicate close requests return the already-closed trade unchanged.
     */
    public synchronized Trade close(String signalId, BigDecimal exitPrice, String reason) {
        if (signalId == null || signalId.isBlank()) {
            throw new IllegalArgumentException("Signal ID is required");
        }
        if (exitPrice == null || exitPrice.signum() <= 0) {
            throw new IllegalArgumentException("Exit price must be positive");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Close reason is required");
        }
        Trade current = tradesBySignalId.get(signalId);
        if (current == null) {
            throw new IllegalArgumentException("Unknown paper trade");
        }
        if (current.status() == Status.CLOSED) {
            return current;
        }
        BigDecimal movement = exitPrice.subtract(current.entryPrice());
        BigDecimal signedMovement = current.side() == Side.BUY ? movement : movement.negate();
        BigDecimal pnl = signedMovement.multiply(BigDecimal.valueOf(current.quantity()));
        Trade closed = new Trade(
                current.tradeId(), current.signalId(), current.strategyId(), current.instrumentId(),
                current.side(), current.quantity(), current.entryPrice(), current.stopPrice(),
                current.targetPrice(), Status.CLOSED, current.openedAt(), clock.instant(),
                exitPrice, pnl, reason);
        tradesBySignalId.put(signalId, closed);
        return closed;
    }

    public synchronized Optional<Trade> findBySignalId(String signalId) {
        return Optional.ofNullable(tradesBySignalId.get(signalId));
    }

    public synchronized List<Trade> allTrades() {
        return List.copyOf(tradesBySignalId.values());
    }

    private void validate(Signal signal) {
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
        if (signal.quantity() < 1) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        boolean validStops = signal.side() == Side.BUY
                ? signal.stopPrice().compareTo(signal.entryPrice()) < 0
                    && signal.targetPrice().compareTo(signal.entryPrice()) > 0
                : signal.stopPrice().compareTo(signal.entryPrice()) > 0
                    && signal.targetPrice().compareTo(signal.entryPrice()) < 0;
        if (!validStops) {
            throw new IllegalArgumentException("Stop and target must be consistent with trade side");
        }
    }
}
