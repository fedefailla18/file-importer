package com.importer.fileimporter.repository

import com.importer.fileimporter.BaseIntegrationSpec
import org.springframework.data.domain.Pageable

import java.time.LocalDate

class TransactionRepositoryIntegrationSpec extends BaseIntegrationSpec {

    def "find transactions by symbol and portfolio in date range"() {
        given:
        def symbol = "BAND"
        def portfolioId = UUID.fromString("1c56e6eb-c0eb-46a6-a4a6-acd770d08c88")
        def startDate = LocalDate.of(2022, 10, 20)
        def endDate = LocalDate.of(2023, 10, 20)
        def pageable = Pageable.unpaged()

        when:
        def result = transactionRepository.findBySymbolAndPortfolioAndDateRange(symbol, portfolioId, startDate, endDate, pageable)

        then:
        result.size() == 6
        result*.symbol.every { it == "BAND" }
        result*.portfolio.id.every { it == portfolioId }
    }

//    def "filter transactions by symbol, side, and date range"() {
//        given:
//        def symbol = "BAND"
//        def portfolioName = "Test"
//        def side = "BUY"
//        def startDate = LocalDate.of(2022, 10, 20)
//        def endDate = LocalDate.of(2023, 10, 20)
//        def pageable = Pageable.unpaged()
//
//        when:
//        def result = transactionRepository.filterTransactions(symbol, portfolioName, side, null, startDate, endDate, null, null, pageable)
//
//        then:
//        result.size() == 3
//        result*.side.every { it == "BUY" }
//        result*.symbol.every { it == "BAND" }
//    }
//
//    def "filter transactions by paid amount greater than a value"() {
//        given:
//        def symbol = "BAND"
//        def portfolioName = "Test"
//        def paidAmountOperator = ">"
//        def paidAmount = new BigDecimal("200")
//        def startDate = LocalDate.of(2022, 10, 20)
//        def endDate = LocalDate.of(2023, 10, 20)
//        def pageable = Pageable.unpaged()
//
//        when:
//        def result = transactionRepository.filterTransactions(symbol, portfolioName, null, null, startDate, endDate, paidAmountOperator, paidAmount, pageable)
//
//        then:
//        result.size() == 2
//        result*.paidAmount.every { it > paidAmount }
//    }
//
//    def "filter transactions by side and symbol"() {
//        given:
//        def symbol = "BAND"
//        def portfolioName = "Test"
//        def side = "SELL"
//        def pageable = Pageable.unpaged()
//
//        when:
//        def result = transactionRepository.filterTransactions(symbol, portfolioName, side, null, null, null, null, null, pageable)
//
//        then:
//        result.size() == 2
//        result*.side.every { it == "SELL" }
//    }
}
