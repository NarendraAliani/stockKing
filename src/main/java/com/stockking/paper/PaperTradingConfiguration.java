package com.stockking.paper;

import com.stockking.risk.PaperRiskGuard;
import com.stockking.risk.RiskPolicy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;
import java.io.IOException;
import java.nio.file.Path;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaperTradingConfiguration {
    @Bean
    Clock stockKingClock() {
        return Clock.systemUTC();
    }

    @Bean
    PaperTradeLedger paperTradeLedger(
            Clock stockKingClock,
            ObjectMapper objectMapper,
            @Value("${stockking.paper.persistence.enabled:false}") boolean persistenceEnabled,
            @Value("${stockking.paper.persistence.path:./data/paper-journal.json}") String journalPath) throws IOException {
        if (!persistenceEnabled) return new PaperTradeLedger(stockKingClock);
        return new PaperTradeLedger(stockKingClock,
                new PaperTradeJournalStore(Path.of(journalPath), objectMapper));
    }

    @Bean
    PaperRiskGuard paperRiskGuard(
            Clock stockKingClock,
            @Value("${stockking.paper.starting-capital:100000}") BigDecimal startingCapital,
            @Value("${stockking.paper.trading-zone:Asia/Kolkata}") String tradingZone) {
        return new PaperRiskGuard(RiskPolicy.initialDefaults(), startingCapital, stockKingClock,
                ZoneId.of(tradingZone));
    }

    @Bean
    PaperTradingService paperTradingService(PaperTradeLedger ledger, PaperRiskGuard riskGuard) {
        riskGuard.restoreFromTrades(ledger.allTrades());
        return new PaperTradingService(ledger, riskGuard);
    }
}
