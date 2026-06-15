package com.importer.fileimporter.facade

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.entity.User
import com.importer.fileimporter.utils.IntegrationTestHelper
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class CoinInformationFacadeIntegrationSpec extends BaseIntegrationSpec {


    Portfolio portfolio1
    Portfolio portfolio2
    List<Map<String, String>> mockRows

    def setup() {
        portfolio1 = Portfolio.builder()
                .name("Binance")
                .creationDate(LocalDateTime.now())
                .build()
        portfolio2 = Portfolio.builder()
                .name("Other")
                .creationDate(LocalDateTime.now().minusMonths(6L))
                .build()

        portfolioRepository.save(portfolio1)
        portfolioRepository.save(portfolio2)

        mockRows = IntegrationTestHelper.readCsvFile()
    }

    def "test getTransactionsInformation with predefined transactions"() {
        given: "a set of predefined transactions"
        def symbol = "BAND"

        def testUser = userRepository.save(new User(username: "testuser", email: "test@user.com", password: "password"))

        def portfolio = portfolioRepository.save(Portfolio.builder()
                .name("Binance")
                .creationDate(LocalDateTime.now())
                .user(testUser)
                .build())

        // Create test transactions for BAND
        def transaction1 = transactionService.save(
            new Transaction(
                symbol: symbol,
                side: "BUY",
                executed: new BigDecimal("100"),
                paidWith: "USDT",
                paidAmount: new BigDecimal("200"),
                portfolio: portfolio,
                dateUtc: LocalDateTime.now().minus(1, ChronoUnit.DAYS),
                pair: "BANDUSDT",
                price: new BigDecimal("2")
            )
        )

        def transaction2 = transactionService.save(
            new Transaction(
                symbol: symbol,
                side: "SELL",
                executed: new BigDecimal("50"),
                paidWith: "USDT",
                paidAmount: new BigDecimal("100"),
                portfolio: portfolio,
                dateUtc: LocalDateTime.now(),
                pair: "BANDUSDT",
                price: new BigDecimal("2")
            )
        )

        def transactions = transactionService.getAllBySymbol(symbol)
        assert !transactions.isEmpty() : "No transactions found for symbol: ${symbol}"

        and: "Mock the pricing facade"
        pricingFacade.getCurrentMarketPrice(_) >> 2.0
        pricingFacade.getPriceInUsdt(_, _) >> 2.0

        when: "getTransactionsInformation is called"
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then: "the response contains the correct information"
        println("Response: $response")

        response.coinName == symbol
        response.amount == transactions.stream()
                .filter { t -> !t.isProcessed() }
                .map { t -> OperationUtils.sumAmount(BigDecimal.ZERO, t.getExecuted(), t.getSide()) }
                .reduce(BigDecimal.ZERO, BigDecimal::add)

        and: "The stable total cost is correct"
        response.stableTotalCost > BigDecimal.ZERO

        and: "The transaction is marked as processed"
        transactions.every { t -> 
            transactionService.findById(t.id).get().processed 
        }

    }
}
