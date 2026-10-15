package com.stockking.web;

import com.stockking.paper.PaperTradeLedger;
import com.stockking.risk.PaperRiskGuard;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only paper-trading dashboard API. No endpoints here can open or close trades. */
@RestController
@RequestMapping("/api/paper")
public class PaperDashboardController {
    private final PaperTradeLedger ledger;
    private final PaperRiskGuard riskGuard;

    public PaperDashboardController(PaperTradeLedger ledger, PaperRiskGuard riskGuard) {
        this.ledger = ledger;
        this.riskGuard = riskGuard;
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard() {
        List<PaperTradeLedger.Trade> trades = ledger.allTrades();
        long openTrades = trades.stream().filter(t -> t.status() == PaperTradeLedger.Status.OPEN).count();
        long closedTrades = trades.size() - openTrades;
        PaperRiskGuard.EntryDecision risk = riskGuard.snapshot();
        return new Dashboard("paper", false, trades.size(), openTrades, closedTrades,
                risk.entriesUsed(), risk.dailyPnl(), risk.allowed(), risk.rejection().name());
    }

    @GetMapping("/risk")
    public RiskSnapshot risk() {
        PaperRiskGuard.EntryDecision risk = riskGuard.snapshot();
        return new RiskSnapshot(risk.allowed(), risk.rejection().name(),
                risk.entriesUsed(), risk.dailyPnl(), false);
    }

    @GetMapping("/trades")
    public List<PaperTradeLedger.Trade> trades(
            @RequestParam(name = "status", required = false) PaperTradeLedger.Status status) {
        List<PaperTradeLedger.Trade> trades = ledger.allTrades();
        if (status == null) return trades;
        return trades.stream().filter(trade -> trade.status() == status).toList();
    }

    public record Dashboard(
            String tradingMode,
            boolean liveTradingEnabled,
            int totalTrades,
            long openTrades,
            long closedTrades,
            int entriesUsedToday,
            BigDecimal dailyRealizedGrossPnl,
            boolean entriesAllowed,
            String riskStatus) {}

    /** Read-only risk state; live trading is always reported as disabled. */
    public record RiskSnapshot(
            boolean entriesAllowed,
            String riskStatus,
            int entriesUsedToday,
            BigDecimal dailyRealizedGrossPnl,
            boolean liveTradingEnabled) {}
}
