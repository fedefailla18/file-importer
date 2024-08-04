package com.importer.fileimporter.facade

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.service.HoldingService
import com.importer.fileimporter.service.PortfolioService
import com.importer.fileimporter.service.usecase.CalculateAmountSpent
import com.importer.fileimporter.utils.OperationUtils
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Subject

class CoinInformationFacadeIntegrationSpec extends BaseIntegrationSpec {

    @Autowired
    CalculateAmountSpent calculateAmountSpent

    @Autowired
    PricingFacade pricingFacade
    @Autowired
    HoldingService holdingService
    @Autowired
    PortfolioService portfolioService

    @Autowired
    @Subject
    CoinInformationFacade coinInformationFacade

    def setup() {
        portfolioService.findOrSave("Binance")
    }

    def "test getTransactionsInformation with predefined transactions"() {
        given: "a set of predefined transactions"
        def symbol = "BAND"
        def transactions = transactionService.getAllBySymbol(symbol)
        assert !transactions.isEmpty() : "No transactions found for symbol: ${symbol}"

        and:
        def currentMarketPrice = 2

        when: "getTransactionsInformation is called"
        def response = coinInformationFacade.getTransactionsInformation(symbol)

        then: "the response contains the correct information"
        println("Response: $response")

        response.coinName == symbol
        response.amount == transactions.stream()
                .map { t -> OperationUtils.sumAmount(BigDecimal.ZERO, t.getTransactionId().getExecuted(), t.getTransactionId().getSide()) }
                .reduce(BigDecimal.ZERO, BigDecimal::add)

        and:
        def totalSpent = transactions.stream()
                .map { t -> calculateAmountSpent.getAmountSpentInUsdtPerTransaction(symbol, t, response) }
                .reduce(BigDecimal.ZERO, BigDecimal::add)

        and:
        response.stableTotalCost == totalSpent
        response.spent['BTC'] == 0.0050965 + 0.005148
        response.spent['USDT'] == 28.5624 + 55.1544
        response.sold['BTC'] == 0.00998322
        response.sold['USDT'] == 100.24
//        response.currentPositionInUsdt == currentMarketPrice * response.amount - seecryptoCompareProxy.getData

//        and:
//        1 * cryptoCompareProxy.getData(symbol, _) >> new HashMap<String, Double>().tap {
//            it.put('USDT', currentMarketPrice)
//            it.put('BTC', 0.0005)
//        } // this is not working
//        cryptoCompareProxy.getHistoricalData(symbol, _,_) >> new CryptoCompareResponse().tap {
//            it.response = 'Success'
//            it.data = new CryptoCompareResponse.Data().tap {
//                it.chartDataList = [new CryptoCompareResponse.ChartData().tap {
//                    it.high = 50
//                }]
//            }

    }
}
