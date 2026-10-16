package com.stockking.risk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** In-memory paper-only entry and realized-loss guard; never routes orders. */
public final class PaperRiskGuard {
    public enum Rejection { NONE, DAILY_ENTRY_LIMIT, DAILY_LOSS_LIMIT, CONSECUTIVE_LOSS_LIMIT }
    public record EntryDecision(boolean allowed, Rejection rejection, int entriesUsed, BigDecimal dailyPnl) {}

    private final RiskPolicy policy;
    private final BigDecimal startingCapital;
    private final Clock clock;
    private final ZoneId tradingZone;
    private LocalDate tradingDate;
    private int entries;
    private int consecutiveLosses;
    private BigDecimal dailyRealizedPnl = BigDecimal.ZERO;

    public PaperRiskGuard(RiskPolicy policy, BigDecimal startingCapital, Clock clock, ZoneId tradingZone) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.tradingZone = Objects.requireNonNull(tradingZone, "tradingZone");
        if (startingCapital == null || startingCapital.signum() <= 0) {
            throw new IllegalArgumentException("Starting capital must be positive");
        }
        this.startingCapital = startingCapital;
        this.tradingDate = LocalDate.now(clock.withZone(tradingZone));
    }


    /** Reconstructs today's risk counters from persisted paper trades during startup. */
    public synchronized void restoreFromTrades(List<com.stockking.paper.PaperTradeLedger.Trade> trades) {
        rollTradingDayIfNeeded();
        entries = 0;
        consecutiveLosses = 0;
        dailyRealizedPnl = BigDecimal.ZERO;
        LocalDate today = LocalDate.now(clock.withZone(tradingZone));
        entries = (int) trades.stream()
                .filter(trade -> LocalDate.ofInstant(trade.openedAt(), tradingZone).equals(today))
                .count();
        trades.stream()
                .filter(trade -> trade.status() == com.stockking.paper.PaperTradeLedger.Status.CLOSED
                        && trade.closedAt() != null
                        && LocalDate.ofInstant(trade.closedAt(), tradingZone).equals(today))
                .sorted(Comparator.comparing(com.stockking.paper.PaperTradeLedger.Trade::closedAt))
                .forEach(trade -> {
                    BigDecimal pnl = trade.realizedGrossPnl();
                    if (pnl != null) {
                        dailyRealizedPnl = dailyRealizedPnl.add(pnl);
                        if (pnl.signum() < 0) consecutiveLosses++;
                        else if (pnl.signum() > 0) consecutiveLosses = 0;
                    }
                });
    }

    public synchronized EntryDecision authorizeEntry() {
        rollTradingDayIfNeeded();
        if (entries >= policy.maxEntriesPerDay()) return decision(false, Rejection.DAILY_ENTRY_LIMIT);
        BigDecimal maxLoss = startingCapital.multiply(policy.maxDailyLossPercent())
                .divide(new BigDecimal("100"), 8, RoundingMode.DOWN);
        if (dailyRealizedPnl.compareTo(maxLoss.negate()) <= 0) return decision(false, Rejection.DAILY_LOSS_LIMIT);
        if (consecutiveLosses >= policy.maxConsecutiveLosses()) return decision(false, Rejection.CONSECUTIVE_LOSS_LIMIT);
        entries++;
        return decision(true, Rejection.NONE);
    }

    public synchronized void recordClosedTrade(BigDecimal realizedPnl) {
        rollTradingDayIfNeeded();
        if (realizedPnl == null) throw new IllegalArgumentException("Realized P&L is required");
        dailyRealizedPnl = dailyRealizedPnl.add(realizedPnl);
        if (realizedPnl.signum() < 0) consecutiveLosses++;
        else if (realizedPnl.signum() > 0) consecutiveLosses = 0;
    }

    public synchronized EntryDecision snapshot() {
        rollTradingDayIfNeeded();
        if (entries >= policy.maxEntriesPerDay()) return decision(false, Rejection.DAILY_ENTRY_LIMIT);
        BigDecimal maxLoss = startingCapital.multiply(policy.maxDailyLossPercent())
                .divide(new BigDecimal("100"), 8, RoundingMode.DOWN);
        if (dailyRealizedPnl.compareTo(maxLoss.negate()) <= 0) return decision(false, Rejection.DAILY_LOSS_LIMIT);
        if (consecutiveLosses >= policy.maxConsecutiveLosses()) return decision(false, Rejection.CONSECUTIVE_LOSS_LIMIT);
        return decision(true, Rejection.NONE);
    }

    private EntryDecision decision(boolean allowed, Rejection rejection) {
        return new EntryDecision(allowed, rejection, entries, dailyRealizedPnl);
    }

    private void rollTradingDayIfNeeded() {
        LocalDate today = LocalDate.now(clock.withZone(tradingZone));
        if (!today.equals(tradingDate)) {
            tradingDate = today;
            entries = 0;
            consecutiveLosses = 0;
            dailyRealizedPnl = BigDecimal.ZERO;
        }
    }
}
