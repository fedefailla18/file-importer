package com.importer.fileimporter.service.usecase

import com.importer.fileimporter.dto.CoinInformationResponse
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.entity.TransactionId
import com.importer.fileimporter.facade.PricingFacade
import spock.lang.Specification

class CalculateAmountSpentSpec extends Specification {

    def pricingFacade = Mock(PricingFacade)
    def sut = new CalculateAmountSpent(pricingFacade)

    def "GetAmountSpentInUsdtPerTransaction. Single BUY transaction"() {
        given:
        def transaction = new Transaction(
                transactionId: new TransactionId(
                        side: "BUY",
                        pair: "RLCUSDT",
                        price: 1,
                        executed: 200
                ),
                symbol: "RLC",
                setPaidWith: "USDT",
                setPaidAmount: 200,
                feeAmount: 0.2
        )

        def response = CoinInformationResponse.createEmpty('RLC')

        when:
        def result = sut.getAmountSpentInUsdtPerTransaction(transaction.symbol, transaction, response)

        then:
        result == 200
        response.getStableTotalCost() == 200
        response.getSpent().get("USDT") == 200
    }

    def "GetAmountSpentInUsdtPerTransaction. Single SELL transaction"() {
        given:
        def transaction = new Transaction(
                transactionId: new TransactionId(
                        side: "SELL",
                        pair: "RLCUSDT",
                        price: 2.15,
                        executed: 70
                ),
                symbol: "RLC",
                setPaidWith: "USDT",
                setPaidAmount: 150.78
        )
        def response = CoinInformationResponse.createEmpty('RLC')

        when:
        def result = sut.getAmountSpentInUsdtPerTransaction(transaction.symbol, transaction, response)

        then:
        result == BigDecimal.valueOf(-150.78)
        response.totalRealizedProfitUsdt == BigDecimal.valueOf(150.78)
    }

    def "GetAmountSpentInUsdtPerTransaction. Multiple transactions"() {
        given:
        def transactions = [
                new Transaction(
                        transactionId: new TransactionId(
                                side: "BUY",
                                pair: "RLCUSDT",
                                price: 1,
                                executed: 200
                        ),
                        symbol: "RLC",
                        setPaidWith: "USDT",
                        setPaidAmount: 200
                ),
                new Transaction(
                        transactionId: new TransactionId(
                                side: "SELL",
                                pair: "RLCUSDT",
                                price: 2.15,
                                executed: 70
                        ),
                        symbol: "RLC",
                        setPaidWith: "USDT",
                        setPaidAmount: 150.78
                ),
                new Transaction(
                        transactionId: new TransactionId(
                                side: "SELL",
                                pair: "RLCUSDT",
                                price: 1.60,
                                executed: 50
                        ),
                        symbol: "RLC",
                        setPaidWith: "USDT",
                        setPaidAmount: 91
                )
        ]
        def response = CoinInformationResponse.createEmpty('RLC')

        when:
        transactions.each { tx ->
            sut.getAmountSpentInUsdtPerTransaction(tx.symbol, tx, response)
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
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response)

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
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response)

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
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response)

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
        def amountSpent = sut.getAmountSpentInUsdt(transaction, response)

        then:
        amountSpent == -1000
        response.getSpent().isEmpty() // Spent should not include sell transactions
        response.totalRealizedProfitUsdt == 1000
    }

    private static Transaction createTransaction(String symbol, String payedWith, String side, BigDecimal executed, BigDecimal price) {
        BigDecimal payedAmount = executed.multiply(price)
        TransactionId transactionId = new TransactionId(
                executed: executed,
                side: side,
                price: price
        )
        return new Transaction(
                symbol: symbol,
                setPaidWith: payedWith,
                transactionId: transactionId,
                setPaidAmount: payedAmount
        )
    }
}
