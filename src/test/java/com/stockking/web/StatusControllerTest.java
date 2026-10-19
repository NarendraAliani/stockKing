package com.stockking.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class StatusControllerTest {
    @Test
    void statusReportsApplicationHealthyAndLiveTradingDisabled() {
        StatusController controller = new StatusController();

        Map<String, String> status = controller.status();

        assertEquals("StockKing", status.get("application"));
        assertEquals("ok", status.get("status"));
        assertEquals("paper", status.get("tradingMode"));
        assertEquals("disabled", status.get("liveTrading"));
    }
}
