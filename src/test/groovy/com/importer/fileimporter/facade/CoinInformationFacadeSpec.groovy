package com.importer.fileimporter.facade

import com.importer.fileimporter.dto.CoinInformationResponse
import com.importer.fileimporter.entity.Holding
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.service.CoinInformationService
import com.importer.fileimporter.service.HoldingService
import com.importer.fileimporter.service.PortfolioService
import com.importer.fileimporter.service.TransactionService
import com.importer.fileimporter.service.usecase.CalculateAmountSpent
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class CoinInformationFacadeSpec extends Specification {

    def pricingFacade = Mock(PricingFacade)
    def holdingService = Mock(HoldingService)
    def calculateAmountSpent = Mock(CalculateAmountSpent)
    def transactionService = Mock(TransactionService)

    def coinInformationService = new CoinInformationService(pricingFacade, holdingService, calculateAmountSpent,
            transactionService)

    def portfolioService = Mock(PortfolioService)

    @Subject
    def coinInformationFacade = new CoinInformationFacade(transactionService, portfolioService, coinInformationService)

    def "should handle no transactions"() {
        given:
        String symbol = "RLC"
        List<Transaction> transactions = []

        transactionService.getAllBySymbol(symbol) >> transactions

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        response.coinName == symbol
        response.amount == BigDecimal.ZERO
        response.totalAmountBought == BigDecimal.ZERO
        response.totalAmountSold == BigDecimal.ZERO
        response.realizedProfit == BigDecimal.ZERO
        response.unrealizedProfit == BigDecimal.ZERO
        response.currentPositionInUsdt == BigDecimal.ZERO
        response.avgEntryPrice.isEmpty()
    }

    def "should calculate transaction information for a given symbol with buy and sell transactions"() {
        given: "A list of transactions and a symbol"
        def symbol = "RLC"
        def transactions = getTestTransactions()
        def portfolio = new Portfolio()
        def holding = Holding.builder()
                .portfolio(portfolio)
                .symbol(symbol)
                .amount(BigDecimal.ZERO)
                .amountInUsdt(BigDecimal.ZERO)
                .totalAmountSold(BigDecimal.ZERO)
                .totalAmountBought(BigDecimal.ZERO)
                .totalRealizedProfitUsdt(BigDecimal.ZERO)
                .build()

        when: "The getTransactionsInformation method is called"
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then: "The response should contain correct transaction information"
        response.coinName == "RLC"
        response.amount == 80
        response.totalAmountBought == new BigDecimal("200")
        response.totalAmountSold == new BigDecimal("120")
        response.currentPositionInUsdt == new BigDecimal("160") // 80 * 2

        and: "holding has copied response fields"
        checkCopiedFields(holding, response)

        and:
        1 * transactionService.getAllBySymbol(symbol) >> transactions
        1 * portfolioService.getByName("Binance") >> Optional.of(portfolio)
        1 * pricingFacade.getCurrentMarketPrice(symbol) >> new BigDecimal("2")
        1 * holdingService.getOrCreateByPortfolioAndSymbol(null, symbol)
        1 * holdingService.save(_ as Holding) >> holding
        
        transactionService.getAllBySymbol(symbol) >> transactions
        calculateAmountSpent.getAmountSpentInUsdt(_ as Transaction, _ as CoinInformationResponse, null) >> {
            Transaction transaction1, CoinInformationResponse response1, Portfolio portfolio1 ->
                if (transaction1.side == "BUY") {
                    return transaction1.paidAmount
                }
                return BigDecimal.ZERO
        }
    }

    def "should calculate transaction information for a given symbol"() {
        given: "A list of transactions and a symbol"
        def symbol = "RLC"
        def transactions = getTestTransactions()
        def portfolio = new Portfolio()
        def holding = new Holding().tap {
            it.symbol = symbol
            it.amount = 0
            it.totalRealizedProfitUsdt = 0
        }
        and: "holding calls"
        transactionService.getAllBySymbol(symbol) >> transactions
        portfolioService.getByName("Binance") >> Optional.of(portfolio)
        holdingService.getOrCreateByPortfolioAndSymbol(_ as Portfolio, symbol) >> holding
        holdingService.save(_ as Holding) >> holding
        and:
        calculateAmountSpent.execute(symbol, transactions, _ as CoinInformationResponse) >> new BigDecimal("599.9950000000")
        pricingFacade.getCurrentMarketPrice(symbol) >> 2

        when: "The getTransactionsInformation method is called"
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then: "The response should contain correct transaction information"
        response.coinName == "RLC"
        response.amount == 80
        response.totalAmountBought == 200
        response.totalAmountSold == 120
        response.currentPositionInUsdt == 160
        // realized and unrealized profit cannot be tested here cause we need calculateAmountSpent

        and:
        calculateAmountSpent.getAmountSpentInUsdt(_ as Transaction, _ as CoinInformationResponse, null) >> 200
    }

    def "should calculate transaction information for realized and unrealized profit with multiple sell transactions"() {
        given:
        String symbol = "RLC"
        List<Transaction> transactions = getTestTransactions()
        def currentMarketPrice = 2
        def portfolio = new Portfolio()
        def holding = new Holding().tap {
            it.symbol = symbol
            it.amount = 0
            it.totalRealizedProfitUsdt = 0
        }
        and: "holding calls"
        transactionService.getAllBySymbol(symbol) >> transactions
        portfolioService.getByName("Binance") >> Optional.of(portfolio)
        holdingService.getOrCreateByPortfolioAndSymbol(_ as Portfolio, symbol) >> holding
        holdingService.save(_ as Holding) >> holding

        and:
        transactionService.getAllBySymbol(symbol) >> transactions
        calculateAmountSpent.getAmountSpentInUsdt(_ as Transaction, _ as CoinInformationResponse, null) >> { Transaction transaction, CoinInformationResponse response ->
            return transaction.paidAmount
        }
        pricingFacade.getCurrentMarketPrice(symbol) >> currentMarketPrice

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        response.coinName == symbol
        response.amount == new BigDecimal("80") // 200 bought - 120 sold
        response.totalAmountBought == new BigDecimal("200")
        response.totalAmountSold == new BigDecimal("120")
        response.currentPrice == 2
        response.realizedProfit == new BigDecimal("-230.5") // 2.15*70 + 1.60*50 - 200
        response.unrealizedProfit == response.amount * currentMarketPrice
        response.currentPositionInUsdt == 160 // 80*2
        response.unrealizedTotalProfitMinusTotalCost == 190.5
    }

    def "should calculate transaction information for a single PURCHASE transaction"() {
        given:
        String symbol = "RLC"
        List<Transaction> transactions = [
                new Transaction(
                        side: "BUY", pair: "RLCUSDT", price: 500, executed: 1,
                        symbol: "RLC",
                        paidWith: "USDT",
                        paidAmount: new BigDecimal("500"),
                )
        ]
        def portfolio = new Portfolio()
        def holding = new Holding().tap {
            it.symbol = symbol
            it.amount = 0
            it.totalRealizedProfitUsdt = 0
        }
        and: "holding calls"
        transactionService.getAllBySymbol(symbol) >> transactions
        portfolioService.getByName("Binance") >> Optional.of(portfolio)
        holdingService.getOrCreateByPortfolioAndSymbol(_ as Portfolio, symbol) >> holding
        holdingService.save(_ as Holding) >> holding

        and:
        transactionService.getAllBySymbol(symbol) >> transactions
        calculateAmountSpent.getAmountSpentInUsdt(_ as Transaction, _ as CoinInformationResponse, null) >> 500
        pricingFacade.getCurrentMarketPrice(symbol) >> 500

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        response.coinName == "RLC"
        response.amount == 1
        response.realizedProfit == BigDecimal.ZERO
        response.unrealizedProfit == 500
        response.currentPositionInUsdt == new BigDecimal("500")
    }


    def "should calculate transaction information for a single SELL transaction"() {
        given:
        String symbol = "RLC"
        List<Transaction> transactions = [
                new Transaction(
                        side: "BUY", pair: "RLCUSDT", price: 500, executed: 2,
                        symbol: "RLC",
                        paidWith: "USDT",
                        paidAmount: 1000,
                ),
                new Transaction(
                        side: "SELL", pair: "RLCUSDT", price: 250, executed: 1,
                        symbol: "RLC",
                        paidWith: "USDT",
                        paidAmount: 250,
                )
        ]
        def portfolio = new Portfolio()
        def holding = new Holding().tap {
            it.symbol = symbol
            it.amount = 0
            it.totalRealizedProfitUsdt = 0
        }
        and: "holding calls"
        transactionService.getAllBySymbol(symbol) >> transactions
        portfolioService.getByName("Binance") >> Optional.of(portfolio)
        holdingService.getOrCreateByPortfolioAndSymbol(_ as Portfolio, symbol) >> holding
        holdingService.save(_ as Holding) >> holding

        and:
        transactionService.getAllBySymbol(symbol) >> transactions
        calculateAmountSpent.getAmountSpentInUsdt(_ as Transaction, _ as CoinInformationResponse, null) >> 500
        pricingFacade.getCurrentMarketPrice(symbol) >> 2000

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        response.coinName == "RLC"
        response.amount == 1
    }

    def "should calculate transaction information for multiple transactions on a single account"() {
        given:
        String symbol = "RLC"
        List<Transaction> transactions = [
                new Transaction(
                        side: "BUY", pair: "RLCUSDT", price: 500, executed: 1,
                        symbol: "RLC",
                        paidWith: "USDT",
                        paidAmount: 500
                ),
                new Transaction(
                        side: "SELL", pair: "RLCUSDT", price: 200, executed: 1,
                        symbol: "RLC",
                        paidWith: "USDT",
                        paidAmount: 200
                )
        ]
        def portfolio = new Portfolio()
        def holding = new Holding().tap {
            it.symbol = symbol
            it.amount = 0
            it.totalRealizedProfitUsdt = 0
        }
        and: "holding calls"
        transactionService.getAllBySymbol(symbol) >> transactions
        portfolioService.getByName("Binance") >> Optional.of(portfolio)
        holdingService.getOrCreateByPortfolioAndSymbol(_ as Portfolio, symbol) >> holding
        holdingService.save(_ as Holding) >> holding

        and:
        transactionService.getAllBySymbol(symbol) >> transactions
        calculateAmountSpent.getAmountSpentInUsdt(_ as Transaction, _ as CoinInformationResponse, null) >> 500
        pricingFacade.getCurrentMarketPrice(symbol) >> 1000

        when:
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then:
        response.coinName == "RLC"
        response.amount == 0
    }

    def List<Transaction> getTestTransactions() {
        return [
                new Transaction(side: "BUY",
                                pair: "RLCUSDT",
                                price: 1,
                                executed: 200,
                        symbol: "RLC",
                        paidWith: "USDT",
                        paidAmount: 200,
                        feeAmount: 0.2,
                ),
                new Transaction(side: "SELL",
                                pair: "RLCUSDT",
                                price: 2.15,
                                executed: 70,
                        symbol: "RLC",
                        paidWith: "USDT",
                        paidAmount: 150.5,
                        feeAmount: 0.15,
                ),
                new Transaction(side: "SELL",
                                pair: "RLCUSDT",
                                price: 1.60,
                                executed: 50,
                        symbol: "RLC",
                        paidWith: "USDT",
                        paidAmount: 80,
                        feeAmount: 0.091371
                )
        ]
    }

    static ArrayList<Transaction> getTransactions(String symbol) {
        return [
                createTransaction(symbol, "BTC", "BUY", new BigDecimal("82.5"), new BigDecimal("0.0000618000")),
                createTransaction(symbol, "BTC", "BUY", new BigDecimal("55.0"), new BigDecimal("0.0000936")),
                createTransaction(symbol, "BTC", "SELL", new BigDecimal("76.2"), new BigDecimal("0.0001311000")),
                createTransaction(symbol, "USDT", "BUY", new BigDecimal("23.6"), new BigDecimal("1.209")),
                createTransaction(symbol, "USDT", "SELL", new BigDecimal("56.0"), new BigDecimal("1.79")),
                createTransaction(symbol, "USDT", "BUY", new BigDecimal("45.6"), new BigDecimal("1.209"))
        ]
    }

    static Transaction createTransaction(String symbol, String payedWith, String side, BigDecimal executed, BigDecimal price) {
        BigDecimal payedAmount = executed.multiply(price)
        return new Transaction(
                executed: executed,
                side: side,
                dateUtc: LocalDateTime.now(),
                price: price,
                symbol: symbol,
                paidWith: payedWith,
                paidAmount: payedAmount
        )
    }

    def checkCopiedFields(holding, response) {
        holding.amount == response.amount
        holding.totalAmountBought == response.totalAmountBought
        holding.totalAmountSold == response.totalAmountSold
        holding.stableTotalCost == response.stableTotalCost
        holding.currentPositionInUsdt == response.currentPositionInUsdt
        holding.totalRealizedProfitUsdt == response.realizedProfit
        holding.amountInUsdt == response.currentPositionInUsdt
    }
}
