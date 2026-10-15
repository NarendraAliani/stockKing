package com.stockking.paper;

import com.stockking.risk.RiskPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Pure position-sizing calculation for paper trading. This class has no broker,
 * persistence, clock, or network dependencies.
 */
public final class PaperRiskSizer {
    private PaperRiskSizer() {}

    public record Request(
            BigDecimal paperCapital,
            BigDecimal entryPrice,
            BigDecimal stopPrice,
            BigDecimal riskPerTradePercent,
            BigDecimal maxCapitalAllocationPercent,
            int unitsPerLot) {}

    public record Decision(
            boolean accepted,
            int quantity,
            BigDecimal estimatedRisk,
            BigDecimal capitalRequired,
            String reason) {
        static Decision reject(String reason) {
            return new Decision(false, 0, BigDecimal.ZERO, BigDecimal.ZERO, reason);
        }
    }

    public static Decision size(Request request, RiskPolicy policy) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(policy, "policy");

        BigDecimal capital = request.paperCapital();
        BigDecimal entry = request.entryPrice();
        BigDecimal stop = request.stopPrice();
        BigDecimal riskPercent = request.riskPerTradePercent();
        BigDecimal allocationPercent = request.maxCapitalAllocationPercent();

        if (capital == null || entry == null || stop == null
                || riskPercent == null || allocationPercent == null) {
            return Decision.reject("MISSING_INPUT");
        }
        if (capital.signum() <= 0 || entry.signum() <= 0 || stop.signum() <= 0) {
            return Decision.reject("NON_POSITIVE_PRICE_OR_CAPITAL");
        }
        if (request.unitsPerLot() < 1) {
            return Decision.reject("INVALID_LOT_SIZE");
        }
        if (riskPercent.signum() <= 0
                || riskPercent.compareTo(policy.riskPerTradePercent()) > 0) {
            return Decision.reject("RISK_PERCENT_OUT_OF_POLICY");
        }
        if (allocationPercent.signum() <= 0
                || allocationPercent.compareTo(new BigDecimal("100")) > 0) {
            return Decision.reject("INVALID_CAPITAL_ALLOCATION");
        }

        BigDecimal perUnitRisk = entry.subtract(stop).abs();
        if (perUnitRisk.signum() <= 0) {
            return Decision.reject("STOP_EQUALS_ENTRY");
        }

        BigDecimal riskBudget = capital.multiply(riskPercent)
                .divide(new BigDecimal("100"), 8, RoundingMode.DOWN);
        BigDecimal allocationBudget = capital.multiply(allocationPercent)
                .divide(new BigDecimal("100"), 8, RoundingMode.DOWN);

        BigDecimal riskLimitedUnits = riskBudget.divide(perUnitRisk, 0, RoundingMode.DOWN);
        BigDecimal capitalLimitedUnits = allocationBudget.divide(entry, 0, RoundingMode.DOWN);
        int lot = request.unitsPerLot();
        BigDecimal permittedUnits = riskLimitedUnits.min(capitalLimitedUnits);
        int quantity = permittedUnits.divide(BigDecimal.valueOf(lot), 0, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(lot)).intValue();

        if (quantity < 1) {
            return Decision.reject("RISK_OR_CAPITAL_BUDGET_TOO_SMALL");
        }

        BigDecimal qty = BigDecimal.valueOf(quantity);
        BigDecimal estimatedRisk = perUnitRisk.multiply(qty);
        BigDecimal capitalRequired = entry.multiply(qty);
        return new Decision(true, quantity, estimatedRisk, capitalRequired, "ACCEPTED");
    }
}
