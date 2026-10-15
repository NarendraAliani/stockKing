package com.stockking.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockking.paper.PaperTradeLedger;
import com.stockking.risk.PaperRiskGuard;
import com.stockking.risk.RiskPolicy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

class PaperDashboardPageControllerTest {
    @Test
    void pageShowsPaperOnlyEmptySessionAndReturnsTemplateName() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T07:00:00Z"), ZoneOffset.UTC);
        var ledger = new PaperTradeLedger(clock);
        var guard = new PaperRiskGuard(RiskPolicy.initialDefaults(),
                new BigDecimal("100000"), clock, ZoneId.of("Asia/Kolkata"));
        var controller = new PaperDashboardPageController(ledger, guard);
        var model = new ExtendedModelMap();

        String view = controller.dashboard(model);

        assertEquals("paper-dashboard", view);
        assertEquals("PAPER", model.getAttribute("tradingMode"));
        assertEquals(Boolean.FALSE, model.getAttribute("liveTradingEnabled"));
        assertEquals(0, model.getAttribute("totalTrades"));
        assertEquals(0L, model.getAttribute("openTrades"));
        assertEquals(0L, model.getAttribute("closedTrades"));
        assertEquals(0, model.getAttribute("entriesUsedToday"));
        assertEquals(BigDecimal.ZERO, model.getAttribute("dailyPnl"));
        assertTrue((Boolean) model.getAttribute("entriesAllowed"));
        assertFalse(((java.util.List<?>) model.getAttribute("trades")).iterator().hasNext());
    }
}
