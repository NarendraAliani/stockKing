package com.stockking.web;

import com.stockking.paper.PaperTradeLedger;
import com.stockking.paper.PaperPerformanceCalculator;
import com.stockking.risk.PaperRiskGuard;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final PaperPerformanceCalculator performanceCalculator = new PaperPerformanceCalculator();

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

    @GetMapping("/performance")
    public PaperPerformanceCalculator.Summary performance() {
        return performanceCalculator.summarize(ledger.allTrades());
    }

    @GetMapping(value = "/trades/export.csv", produces = "text/csv")
    public ResponseEntity<String> exportTradesCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("tradeId,signalId,strategyId,instrumentId,side,quantity,entryPrice,stopPrice,targetPrice,status,openedAt,closedAt,exitPrice,realizedGrossPnl,lastReason\r\n");
        for (PaperTradeLedger.Trade trade : ledger.allTrades()) {
            appendCsvRow(csv, trade);
        }
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"paper-trades.csv\"")
                .body(csv.toString());
    }

    private static void appendCsvRow(StringBuilder csv, PaperTradeLedger.Trade trade) {
        appendCsvCell(csv, trade.tradeId(), true);
        appendCsvCell(csv, trade.signalId(), true);
        appendCsvCell(csv, trade.strategyId(), true);
        appendCsvCell(csv, trade.instrumentId(), true);
        appendCsvCell(csv, trade.side().name(), true);
        appendCsvCell(csv, Integer.toString(trade.quantity()), false);
        appendCsvCell(csv, trade.entryPrice().toPlainString(), false);
        appendCsvCell(csv, trade.stopPrice().toPlainString(), false);
        appendCsvCell(csv, trade.targetPrice().toPlainString(), false);
        appendCsvCell(csv, trade.status().name(), true);
        appendCsvCell(csv, trade.openedAt().toString(), true);
        appendCsvCell(csv, trade.closedAt() == null ? "" : trade.closedAt().toString(), true);
        appendCsvCell(csv, trade.exitPrice() == null ? "" : trade.exitPrice().toPlainString(), false);
        appendCsvCell(csv, trade.realizedGrossPnl() == null ? "" : trade.realizedGrossPnl().toPlainString(), false);
        appendCsvCell(csv, trade.lastReason() == null ? "" : trade.lastReason(), true);
        csv.append("\\r\\n");
    }

    private static void appendCsvCell(StringBuilder csv, String value, boolean untrustedText) {
        String safeValue = value == null ? "" : value;
        if (untrustedText && !safeValue.isEmpty()
                && "=+-@\\t\\r".indexOf(safeValue.charAt(0)) >= 0) {
            safeValue = "'" + safeValue;
        }
        csv.append('"').append(safeValue.replace("\"", "\"\"")).append('"').append(',');
    }

    @GetMapping("/trades/{signalId}")
    public ResponseEntity<PaperTradeLedger.Trade> tradeBySignalId(
            @org.springframework.web.bind.annotation.PathVariable String signalId) {
        return ledger.findBySignalId(signalId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/risk")
    public RiskSnapshot risk() {
        PaperRiskGuard.EntryDecision risk = riskGuard.snapshot();
        return new RiskSnapshot(risk.allowed(), risk.rejection().name(),
                risk.entriesUsed(), risk.dailyPnl(), false);
    }

    @GetMapping("/trades")
    public List<PaperTradeLedger.Trade> trades(
            @RequestParam(name = "status", required = false) PaperTradeLedger.Status status,
            @RequestParam(name = "side", required = false) PaperTradeLedger.Side side,
            @RequestParam(name = "instrument", required = false) String instrument) {
        String instrumentFilter = instrument == null ? null : instrument.trim();
        if (instrumentFilter != null && instrumentFilter.isEmpty()) instrumentFilter = null;
        final String requestedInstrument = instrumentFilter;
        return ledger.allTrades().stream()
                .filter(trade -> status == null || trade.status() == status)
                .filter(trade -> side == null || trade.side() == side)
                .filter(trade -> requestedInstrument == null
                        || trade.instrumentId().equalsIgnoreCase(requestedInstrument))
                .toList();
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
