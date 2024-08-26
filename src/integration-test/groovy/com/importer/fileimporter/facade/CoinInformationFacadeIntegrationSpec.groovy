package com.importer.fileimporter.facade

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.utils.OperationUtils
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Subject

import java.math.RoundingMode
import java.time.LocalDateTime

class CoinInformationFacadeIntegrationSpec extends BaseIntegrationSpec {

    @Autowired
    @Subject
    CoinInformationFacade coinInformationFacade

    def portfolio1 = Portfolio.builder()
            .name("Binance")
            .creationDate(LocalDateTime.now())
            .build()
    def portfolio2 = Portfolio.builder()
            .name("Other")
            .creationDate(LocalDateTime.now().minusMonths(6L))
            .build()

    def setup() {
        portfolioRepository.save(portfolio1)
        portfolioRepository.save(portfolio2)
    }

    def "test getTransactionsInformation with predefined transactions"() {
        given: "a set of predefined transactions"
        def symbol = "BAND"
        def transactions = transactionService.getAllBySymbol(symbol)
        assert !transactions.isEmpty() : "No transactions found for symbol: ${symbol}"

        and:
        def currentMarketPrice = 2

        when: "getTransactionsInformation is called"
        def response = coinInformationFacade.getTransactionsInformationBySymbol(symbol)

        then: "the response contains the correct information"
        println("Response: $response")

        response.coinName == symbol
        response.amount == transactions.stream()
                .map { t -> OperationUtils.sumAmount(BigDecimal.ZERO, t.getTransactionId().getExecuted(), t.getTransactionId().getSide()) }
                .reduce(BigDecimal.ZERO, BigDecimal::add)

        and:
        def totalSpent = new BigDecimal(247.5+165+28.5624+55.1544).setScale(8, RoundingMode.UP)

        and:
        response.stableTotalCost == totalSpent
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
