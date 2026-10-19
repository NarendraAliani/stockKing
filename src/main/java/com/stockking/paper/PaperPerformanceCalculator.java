package com.stockking.paper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Calculates descriptive performance statistics from the in-memory paper ledger only. */
public final class PaperPerformanceCalculator {
    public Summary summarize(List<PaperTradeLedger.Trade> trades) {
        Objects.requireNonNull(trades, "trades");
        List<PaperTradeLedger.Trade> closed = trades.stream()
                .filter(trade -> trade.status() == PaperTradeLedger.Status.CLOSED)
                .sorted(Comparator.comparing(PaperTradeLedger.Trade::closedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        int wins = 0;
        int losses = 0;
        BigDecimal netGrossPnl = BigDecimal.ZERO;
        BigDecimal cumulativePnl = BigDecimal.ZERO;
        BigDecimal highWaterMark = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (PaperTradeLedger.Trade trade : closed) {
            BigDecimal pnl = trade.realizedGrossPnl() == null
                    ? BigDecimal.ZERO : trade.realizedGrossPnl();
            if (pnl.signum() > 0) wins++;
            else if (pnl.signum() < 0) losses++;
            netGrossPnl = netGrossPnl.add(pnl);
            cumulativePnl = cumulativePnl.add(pnl);
            if (cumulativePnl.compareTo(highWaterMark) > 0) highWaterMark = cumulativePnl;
            BigDecimal drawdown = highWaterMark.subtract(cumulativePnl);
            if (drawdown.compareTo(maxDrawdown) > 0) maxDrawdown = drawdown;
        }

        BigDecimal winRatePercent = closed.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(wins).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(closed.size()), 2, RoundingMode.HALF_UP);

        return new Summary(closed.size(), wins, losses, closed.size() - wins - losses,
                winRatePercent, netGrossPnl, maxDrawdown);
    }

    public record Summary(
            int closedTrades,
            int winningTrades,
            int losingTrades,
            int breakevenTrades,
            BigDecimal winRatePercent,
            BigDecimal netRealizedGrossPnl,
            BigDecimal maxDrawdownGross) {}
}
