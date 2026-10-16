package com.stockking.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaperTradeApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void tradesEndpointReturnsJsonArrayWhenLedgerIsEmpty() throws Exception {
        mockMvc.perform(get("/api/paper/trades"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json("[]"));
    }

    @Test
    void unknownSignalReturnsNotFoundOverHttp() throws Exception {
        mockMvc.perform(get("/api/paper/trades/{signalId}", "missing-signal-for-test"))
                .andExpect(status().isNotFound());
    }

    @Test
    void csvExportReturnsDownloadWithHeaderWhenLedgerIsEmpty() throws Exception {
        mockMvc.perform(get("/api/paper/trades/export.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\\"paper-trades.csv\\""))
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("tradeId,signalId,strategyId")))
                .andExpect(content().string(org.hamcrest.Matchers.endsWith("\\r\\n")));
    }

    @Test
    void statusEndpointReportsPaperModeAndLiveTradingDisabledOverHttp() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("StockKing"))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.tradingMode").value("paper"))
                .andExpect(jsonPath("$.liveTrading").value("disabled"));
    }
}
