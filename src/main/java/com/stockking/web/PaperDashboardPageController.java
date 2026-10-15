package com.stockking.web;

import com.stockking.paper.PaperTradeLedger;
import com.stockking.risk.PaperRiskGuard;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Server-rendered, read-only paper trading dashboard. */
@Controller
public class PaperDashboardPageController {
    private final PaperTradeLedger ledger;
    private final PaperRiskGuard riskGuard;

    public PaperDashboardPageController(PaperTradeLedger ledger, PaperRiskGuard riskGuard) {
        this.ledger = ledger;
        this.riskGuard = riskGuard;
    }

    @GetMapping("/paper")
    public String dashboard(Model model) {
        List<PaperTradeLedger.Trade> trades = ledger.allTrades();
        long openTrades = trades.stream().filter(t -> t.status() == PaperTradeLedger.Status.OPEN).count();
        var risk = riskGuard.snapshot();
        model.addAttribute("trades", trades);
        model.addAttribute("totalTrades", trades.size());
        model.addAttribute("openTrades", openTrades);
        model.addAttribute("closedTrades", trades.size() - openTrades);
        model.addAttribute("entriesUsedToday", risk.entriesUsed());
        model.addAttribute("dailyPnl", risk.dailyPnl());
        model.addAttribute("entriesAllowed", risk.allowed());
        model.addAttribute("riskStatus", risk.rejection().name());
        model.addAttribute("tradingMode", "PAPER");
        model.addAttribute("liveTradingEnabled", false);
        return "paper-dashboard";
    }
}
