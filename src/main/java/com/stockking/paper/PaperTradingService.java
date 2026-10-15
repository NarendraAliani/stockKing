package com.stockking.paper;

import com.stockking.risk.PaperRiskGuard;
import java.math.BigDecimal;
import java.util.Objects;

/** Coordinates paper-only trade lifecycle and risk checks without external I/O. */
public final class PaperTradingService {
    private final PaperTradeLedger ledger;
    private final PaperRiskGuard riskGuard;

    public PaperTradingService(PaperTradeLedger ledger, PaperRiskGuard riskGuard) {
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.riskGuard = Objects.requireNonNull(riskGuard, "riskGuard");
    }

    public synchronized PaperTradeLedger.Trade open(PaperTradeLedger.Signal signal) {
        PaperTradeLedger.validateSignal(signal);
        var existing = ledger.findBySignalId(signal.signalId());
        if (existing.isPresent()) return existing.get();

        var decision = riskGuard.authorizeEntry();
        if (!decision.allowed()) throw new RiskRejectedException(decision.rejection().name());
        return ledger.open(signal);
    }

    public synchronized PaperTradeLedger.Trade close(String signalId, BigDecimal exitPrice, String reason) {
        var before = ledger.findBySignalId(signalId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown paper trade"));
        if (before.status() == PaperTradeLedger.Status.CLOSED) return before;
        var closed = ledger.close(signalId, exitPrice, reason);
        riskGuard.recordClosedTrade(closed.realizedGrossPnl());
        return closed;
    }

    public PaperRiskGuard.EntryDecision riskSnapshot() {
        return riskGuard.snapshot();
    }

    public static final class RiskRejectedException extends IllegalStateException {
        public RiskRejectedException(String reason) {
            super("Paper trade rejected by risk guard: " + reason);
        }
    }
}
