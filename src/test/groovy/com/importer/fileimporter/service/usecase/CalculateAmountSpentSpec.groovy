package com.importer.fileimporter.service.usecase

import com.importer.fileimporter.dto.CoinInformationResponse
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.facade.PricingFacade
import com.importer.fileimporter.service.HoldingService
import spock.lang.Specification

import java.time.LocalDateTime

class CalculateAmountSpentSpec extends Specification {

    def pricingFacade = Mock(PricingFacade)
    def holdingService = Mock(HoldingService)
    def sut = new CalculateAmountSpent(pricingFacade, holdingService)

    def "GetAmountSpentInUsdtPerTransaction. Single BUY transaction"() {
        given:
        def transaction = Transaction.builder()
            .symbol("ETH")
            .pair("ETHUSDT")
            .side("BUY")
            .paidWith("USDT")
            .executed(new BigDecimal("1.0"))
            .price(new BigDecimal("4000"))
            .paidAmount(new BigDecimal("4000"))
            .dateUtc(LocalDateTime.now())
            .build()
        def response = CoinInformationResponse.createEmpty('ETH')

        when:
        def result = sut.getAmountSpentInUsdt(transaction, response, null)

        then:
        result == 4000
        response.getStableTotalCost() == 4000
        response.getSpent().get("USDT") == 4000
    }

    def "GetAmountSpentInUsdtPerTransaction. Single SELL transaction"() {
        given:
        def transaction = new Transaction(
                        side: "SELL",
                        pair: "RLCUSDT",
                        price: 2.15,
                        executed: 70,
                symbol: "RLC",
                paidWith: "USDT",
                paidAmount: 150.78
        )
        def response = CoinInformationResponse.createEmpty('RLC')

        when:
        def result = sut.getAmountSpentInUsdt(transaction, response, null)

        then:
        result == BigDecimal.valueOf(-150.78)
        response.totalRealizedProfitUsdt == BigDecimal.valueOf(150.78)
    }

    def "GetAmountSpentInUsdtPerTransaction. Multiple transactions"() {
        given:
        def transactions = [
                new Transaction(side: "BUY",
                                pair: "RLCUSDT",
                                price: 1,
                                executed: 200,
                        symbol: "RLC",
                        paidWith: "USDT",
                        paidAmount: 200
                ),
                new Transaction(side: "SELL",
                                pair: "RLCUSDT",
                                price: 2.15,
                                executed: 70,
                        symbol: "RLC",
                        paidWith: "USDT",
                        paidAmount: 150.78
                ),
                new Transaction(side: "SELL",
                                pair: "RLCUSDT",
                                price: 1.60,
                                executed: 50,
                        symbol: "RLC",
                        paidWith: "USDT",
                        paidAmount: 91
                )
        ]
        def response = CoinInformationResponse.createEmpty('RLC')

        when:
        transactions.each { tx ->
            sut.getAmountSpentInUsdt(tx, response, null)
        }

        then:
        response.spent.get("USDT") == 200
        response.stableTotalCost == 200
        response.totalRealizedProfitUsdt == 241.78
    }

    def "should return amount spent in USDT for a buy transaction paid with stable coin"() {
        given:
        def symbol = "RLC"
        def transaction = createTransaction(symbol, "USDT", "BUY", new BigDecimal("1"), new BigDecimal("500"))
        def response = CoinInformationResponse.createEmpty(symbol)

        when:
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response, null)

        then:
        amountSpent == new BigDecimal("500")
        response.getSpent().get("USDT") == new BigDecimal("500")
        response.getStableTotalCost() == new BigDecimal("500")
    }

    def "should return amount spent in USDT for a buy transaction paid with non-stable coin"() {
        given:
        def symbol = "RLC"
        def transaction = createTransaction(symbol, "BTC", "BUY", new BigDecimal("1"), new BigDecimal("0.1"))
        def response = CoinInformationResponse.createEmpty(symbol)

        and:
        pricingFacade.getPriceInUsdt(symbol, _) >> 1000

        when:
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response, null)

        then:
        amountSpent == 1000
        response.getSpent().get("BTC") == new BigDecimal("0.1")
        response.getStableTotalCost() == 1000
    }

    def "should return amount earned in USDT for a sell transaction"() {
        given:
        def symbol = "RLC"
        def transaction = createTransaction(symbol, "USDT", "SELL", new BigDecimal("1"), new BigDecimal("500"))
        def response = CoinInformationResponse.createEmpty(symbol)

        when:
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response, null)

        then:
        amountSpent == new BigDecimal("-500")
        response.getSpent().isEmpty() // Spent should not include sell transactions
        response.getTotalRealizedProfitUsdt() == new BigDecimal("500")
    }

    def "should return amount earned in USDT for a sell transaction paid with non-stable coin"() {
        given:
        def symbol = "RLC"
        def transaction = createTransaction(symbol, "BTC", "SELL", new BigDecimal("1"), new BigDecimal("0.1"))
        def response = CoinInformationResponse.createEmpty(symbol)

        and:
        pricingFacade.getPriceInUsdt(symbol, _) >> 1000

        when:
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response, null)

        then:
        amountSpent == -1000
        response.getSpent().isEmpty() // Spent should not include sell transactions
        response.totalRealizedProfitUsdt == 1000
    }

    private static Transaction createTransaction(String symbol, String paidWith, String side, BigDecimal executed, BigDecimal price) {
        BigDecimal paidAmount = executed.multiply(price)
        return new Transaction(
                symbol: symbol,
                paidWith: paidWith,
                executed: executed,
                side: side,
                price: price,
                paidAmount: paidAmount
        )
    }
    def "should update paid with holding for non-stable coin buy transaction"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "TestPortfolio")
        def transaction = createTransaction(symbol, "BTC", "BUY", new BigDecimal("1"), new BigDecimal("0.1"))
        def response = CoinInformationResponse.createEmpty(symbol)

        and:
        pricingFacade.getPriceInUsdt(symbol, _) >> 1000

        when:
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response, portfolio)

        then:
        amountSpent == 1000
        response.getSpent().get("BTC") == new BigDecimal("0.1")
        response.getStableTotalCost() == 1000

        and: "updatePaidWithHolding should be called with correct parameters"
        1 * holdingService.updatePaidWithHolding(true, "BTC", new BigDecimal("0.1"), portfolio, new BigDecimal("1"), 1000)
    }

    def "should update paid with holding for non-stable coin sell transaction"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "TestPortfolio")
        def transaction = createTransaction(symbol, "BTC", "SELL", new BigDecimal("1"), new BigDecimal("0.1"))
        def response = CoinInformationResponse.createEmpty(symbol)

        and:
        pricingFacade.getPriceInUsdt(symbol, _) >> 1000

        when:
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response, portfolio)

        then:
        amountSpent == -1000
        response.getSpent().isEmpty()
        response.totalRealizedProfitUsdt == 1000

        and: "updatePaidWithHolding should be called with correct parameters"
        1 * holdingService.updatePaidWithHolding(false, "BTC", new BigDecimal("0.1"), portfolio, new BigDecimal("1"), 1000)
    }

    def "should not update paid with holding for stable coin transactions"() {
        given:
        def symbol = "RLC"
        def portfolio = new Portfolio(name: "TestPortfolio")
        def transaction = createTransaction(symbol, "USDT", "BUY", new BigDecimal("1"), new BigDecimal("500"))
        def response = CoinInformationResponse.createEmpty(symbol)

        when:
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response, portfolio)

        then:
        amountSpent == new BigDecimal("500")
        response.getSpent().get("USDT") == new BigDecimal("500")
        response.getStableTotalCost() == new BigDecimal("500")

        and: "updatePaidWithHolding should not be called for stable coins"
        0 * holdingService.updatePaidWithHolding(_, _, _, _, _, _)
    }

    def "should return ZERO when paidAmount is null"() {
        given:
        def transaction = Transaction.builder()
            .symbol("ETH")
            .pair("ETHUSDT")
            .side("BUY")
            .paidWith("USDT")
            .executed(new BigDecimal("1.0"))
            .price(new BigDecimal("4000"))
            // paidAmount is null
            .dateUtc(LocalDateTime.now())
            .build()
        def response = CoinInformationResponse.createEmpty('ETH')

        when:
        def result = sut.getAmountSpentInUsdt(transaction, response, null)

        then:
        result == BigDecimal.ZERO
    }

    def "should return ZERO when executed is null"() {
        given:
        def transaction = Transaction.builder()
            .symbol("ETH")
            .pair("ETHUSDT")
            .side("BUY")
            .paidWith("USDT")
            // executed is null
            .price(new BigDecimal("4000"))
            .paidAmount(new BigDecimal("4000"))
            .dateUtc(LocalDateTime.now())
            .build()
        def response = CoinInformationResponse.createEmpty('ETH')

        when:
        def result = sut.getAmountSpentInUsdt(transaction, response, null)

        then:
        result == BigDecimal.ZERO
    }
}
