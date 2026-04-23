package com.importer.fileimporter.repository

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Transaction
import org.springframework.data.domain.Pageable

import java.time.LocalDate
import java.time.LocalDateTime

class TransactionRepositoryIntegrationSpec extends BaseIntegrationSpec {

    def "find transactions by symbol and portfolio in date range"() {
        given:
        def symbol = "BAND"
        def portfolio = Portfolio.builder()
                .name("Test Repo Portfolio")
                .user(defaultUser)
                .creationDate(LocalDateTime.now())
                .build()
        portfolio = portfolioRepository.save(portfolio)
        def portfolioId = portfolio.getId()

        // Create some transactions
        def t1 = Transaction.builder()
                .symbol(symbol)
                .portfolio(portfolio)
                .dateUtc(LocalDateTime.of(2023, 1, 1, 10, 0))
                .side("BUY")
                .pair("BANDUSDT")
                .price(BigDecimal.ONE)
                .executed(BigDecimal.TEN)
                .build()
        transactionRepository.save(t1)

        def startDate = LocalDate.of(2022, 10, 20)
        def endDate = LocalDate.of(2023, 10, 20)
        def pageable = Pageable.unpaged()

        when:
        def result = transactionRepository.findBySymbolAndPortfolioAndDateRange(symbol, portfolioId, startDate, endDate, pageable)

        then:
        result.content.size() >= 1
        result.content*.symbol.every { it == "BAND" }
        result.content*.portfolio.id.every { it == portfolioId }
    }
}
