package com.stockking.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaperDashboardPageIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void rendersReadOnlyPaperDashboardTemplate() throws Exception {
        mockMvc.perform(get("/paper"))
                .andExpect(status().isOk())
                .andExpect(view().name("paper-dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PAPER MODE · LIVE OFF")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No paper trades yet")));
    }

    @Test
    void paperDashboardApiStaysReadOnlyAndReportsLiveTradingDisabled() throws Exception {
        mockMvc.perform(get("/api/paper/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"liveTradingEnabled\":false")));
    }
}
