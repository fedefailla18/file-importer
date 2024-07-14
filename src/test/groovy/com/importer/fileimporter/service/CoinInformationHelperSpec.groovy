package com.importer.fileimporter.service

import com.importer.fileimporter.dto.CoinInformationResponse
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.entity.TransactionId
import com.importer.fileimporter.utils.DateUtils
import spock.lang.Specification

import java.time.LocalDateTime

class CoinInformationHelperSpec extends Specification {

    GetSymbolHistoricPriceHelper getSymbolHistoricPriceService = Mock()
    CoinInformationHelper coinInformationHelper = new CoinInformationHelper(getSymbolHistoricPriceService)

//    def "should calculate average entry price for non-stable coin"() {
//        given:
//        def detail = CoinInformationResponse.createEmpty("TestCoin")
//        def date = LocalDateTime.parse("2023-06-15T10:15:30")
//        def transaction = new Transaction(transactionId: new TransactionId(dateUtc: date, side: "BUY", executed: new BigDecimal("2.5"), price: new BigDecimal("400")), payedWith: "BTC")
//        def transactions = [transaction]
//
//        when:
//        coinInformationHelper.calculateAvgEntryPrice(detail, transactions)
//
//        then:
//        detail.avgEntryPrice['USDT'] == 37500
//        1 * getSymbolHistoricPriceService.getPriceInUsdt("BTC", new BigDecimal("400"), date) >> new BigDecimal("15000")
//    }
//
//    def "should handle mixed buy and sell transactions"() {
//        given: "A CoinInformationResponse and a list of mixed buy and sell transactions"
//        def detail = CoinInformationResponse.createEmpty("TestCoin")
//
//        def localDateTime = DateUtils.getLocalDateTime("2023-06-15 10:15:30")
//
//        def localDateTime1 = DateUtils.getLocalDateTime("2023-06-16 10:15:30")
//        def transactions = [
//                new Transaction(transactionId: new TransactionId(dateUtc: localDateTime, side: "BUY", pair: "BTCUSD",
//                        price: 1000, executed: new BigDecimal("1.0")), payedWith: "USD"),
//                new Transaction(transactionId: new TransactionId(dateUtc: localDateTime1, side: "SELL", pair: "BTCUSD",
//                        price: 1500, executed: new BigDecimal("0.5")), payedWith: "USD")
//        ]
//
//        when: "calculateAvgEntryPrice is called"
//        coinInformationHelper.calculateAvgEntryPrice(detail, transactions)
//
//        then: "The average price is calculated correctly for mixed buy and sell transactions"
//        detail.avgEntryPrice["USD"] == 1750.0
//        detail.totalTransactions == 2
//    }
//
//    @Ignore
//    def "should calculate average price correctly for multiple transactions"() {
//        given: "A list of transactions with varying prices and amounts"
//        def detail = CoinInformationResponse.createEmpty("TestCoin")
//        def transactions = [
//                new Transaction(transactionId: new TransactionId(dateUtc: DateUtils.getLocalDateTime("2023-06-15 10:15:30"), side: "BUY", pair: "BTCUSD", price: new BigDecimal("1000"), executed: new BigDecimal("1.0")), payedWith: "USD"),
//                new Transaction(transactionId: new TransactionId(dateUtc: DateUtils.getLocalDateTime("2023-06-16 10:15:30"), side: "BUY", pair: "BTCUSD", price: new BigDecimal("2000"), executed: new BigDecimal("0.5")), payedWith: "USD"),
//                new Transaction(transactionId: new TransactionId(dateUtc: DateUtils.getLocalDateTime("2023-06-17 10:15:30"), side: "SELL", pair: "BTCUSD", price: new BigDecimal("1500"), executed: new BigDecimal("0.25")), payedWith: "USD"),
//        ]
//
//        when: "Calculate average entry price is called"
//        coinInformationHelper.calculateAvgEntryPrice(detail, transactions)
//
//        then: "The average price is calculated and set correctly"
//        detail.avgEntryPrice["USD"] == new BigDecimal("2000.0") // This will need to be calculated based on your logic
//        detail.totalTransactions == 5
//
//        and:
//        0 * getSymbolHistoricPriceService.getPriceInUsdt(_, _, _)
//    }
//
//
//
//    def "should calculate average entry price for stable coin transactions"() {
//        given: "A CoinInformationResponse and a list of transactions with stable coins"
//        def detail = CoinInformationResponse.createEmpty("BTC")
//        def transactions = [
//                new Transaction(transactionId: new TransactionId(dateUtc: getLocalDateTime(), side: "BUY", pair: "BTCUSDT", price: new BigDecimal("1000"), executed: new BigDecimal("1.0")), payedWith: "USDT"),
//                new Transaction(transactionId: new TransactionId(dateUtc: getLocalDateTime(), side: "BUY", pair: "BTCUSDC", price: new BigDecimal("1100"), executed: new BigDecimal("0.5")), payedWith: "USDC")
//        ]
//
//        when: "calculateAvgEntryPrice is called"
//        coinInformationHelper.calculateAvgEntryPrice(detail, transactions)
//
//        then: "The average price is calculated directly from the stable coin transactions"
//        detail.avgEntryPrice["USDT"] == new BigDecimal("1000.0")
//        detail.avgEntryPrice["USDC"] == new BigDecimal("550.0")
//        detail.totalTransactions == 2
//    }
//
//
//    def "should calculate average entry price for non-stable coin transactions by converting to USDT"() {
//        given: "A CoinInformationResponse and a list of transactions with non-stable coins"
//        def detail = CoinInformationResponse.createEmpty("BTC")
//        def transactions = [
//                new Transaction(transactionId: new TransactionId(dateUtc: DateUtils.getLocalDateTime("2023-06-15T10:15:30"), side: "BUY", pair: "BTC/ETH", price: new BigDecimal("10"), executed: new BigDecimal("1.0")), payedWith: "ETH"),
//                new Transaction(transactionId: new TransactionId(dateUtc: getLocalDateTime(), side: "BUY", pair: "BTC/BTC", price: new BigDecimal("0.5"), executed: new BigDecimal("0.5")), payedWith: "BTC")
//        ]
//
//        and: "Mocking GetSymbolHistoricPriceService to return converted prices"
//        getSymbolHistoricPriceService.getPriceInUsdt("ETH", new BigDecimal("10"), DateUtils.getLocalDateTime("2023-06-15T10:15:30")) >> new BigDecimal("2000")
//        getSymbolHistoricPriceService.getPriceInUsdt("BTC", new BigDecimal("0.5"), getLocalDateTime()) >> new BigDecimal("15000")
//
//        when: "calculateAvgEntryPrice is called"
//        coinInformationHelper.calculateAvgEntryPrice(detail, transactions)
//
//        then: "The average price is calculated by converting non-stable coin prices to USDT"
//        detail.avgEntryPrice["USDT"] == new BigDecimal("21500.0")
//        detail.totalTransactions == 2
//    }
//
//    def "should handle mixed transactions with stable and non-stable coins"() {
//        given: "A CoinInformationResponse and a list of mixed transactions"
//        def detail = CoinInformationResponse.createEmpty("BTC")
//        def transactions = [
//                new Transaction(transactionId: new TransactionId(dateUtc: DateUtils.getLocalDateTime("2023-06-15T10:15:30"), side: "BUY", pair: "BTCUSDT", price: new BigDecimal("1000"), executed: new BigDecimal("1.0")), payedWith: "USDT"),
//                new Transaction(transactionId: new TransactionId(dateUtc: getLocalDateTime(), side: "BUY", pair: "BTC/ETH", price: new BigDecimal("10"), executed: new BigDecimal("2.0")), payedWith: "ETH")
//        ]
//
//        and: "Mocking GetSymbolHistoricPriceService to return converted prices"
//        getSymbolHistoricPriceService.getPriceInUsdt("ETH", new BigDecimal("10"), getLocalDateTime()) >> new BigDecimal("2000")
//
//        when: "calculateAvgEntryPrice is called"
//        coinInformationHelper.calculateAvgEntryPrice(detail, transactions)
//
//        then: "The average price is calculated using both stable and non-stable transactions"
//        detail.avgEntryPrice["USDT"] == new BigDecimal("5000.0")
//        detail.totalTransactions == 2
//    }
//
//    def "should handle empty transaction list"() {
//        given: "A CoinInformationResponse and an empty list of transactions"
//        def detail = CoinInformationResponse.createEmpty("BTC")
//        def transactions = []
//
//        when: "calculateAvgEntryPrice is called"
//        coinInformationHelper.calculateAvgEntryPrice(detail, transactions)
//
//        then: "The method handles the empty list gracefully"
//        detail.avgEntryPrice.isEmpty()
//        detail.totalTransactions == 0
//    }
//
//    def "should log warning for invalid non-stable coin conversion"() {
//        given: "A CoinInformationResponse and a transaction with a non-stable coin that fails conversion"
//        def detail = CoinInformationResponse.createEmpty("BTC")
//        def transaction = new Transaction(transactionId: new TransactionId(dateUtc: DateUtils.getLocalDateTime("2023-06-15T10:15:30"), side: "BUY", pair: "BTC/XYZ", price: new BigDecimal("100"), executed: new BigDecimal("1.0")), payedWith: "XYZ")
//
//        and: "Mocking GetSymbolHistoricPriceService to return an invalid price"
//        getSymbolHistoricPriceService.getPriceInUsdt("XYZ", new BigDecimal("100"), DateUtils.getLocalDateTime("2023-06-15T10:15:30")) >> new BigDecimal("-1")
//
//        when: "calculateAvgEntryPrice is called"
//        coinInformationHelper.calculateAvgEntryPrice(detail, [transaction])
//
//        then: "A warning is logged and no price is set"
//        detail.avgEntryPrice.isEmpty()
//        detail.totalTransactions == 1
//    }
//
//
//    // new tests
//    def "calculateAvgEntryPrice for stable coin"() {
//        given:
//        CoinInformationResponse response = CoinInformationResponse.createEmpty("BTC")
//        List<Transaction> transactions = [
//                new Transaction(new TransactionId("BTCUSDT", "BUY", "BTC", DateUtils.getLocalDateTime("2023-10-01T00:44:01"), new BigDecimal("26964.2"), new BigDecimal("0.005"), "USDT"), "USDT", new BigDecimal("134.821"), null, null),
//                new Transaction(new TransactionId("BTCUSDT", "BUY", "BTC", DateUtils.getLocalDateTime("2023-09-11T20:17:48"), new BigDecimal("27915.83"), new BigDecimal("0.036"), "USDT"), "USDT", new BigDecimal("1004.96988"), null, null)
//        ]
//
//        when:
//        coinInformationHelper.calculateAvgEntryPrice(response, transactions)
//
//        then:
//        response.getTotalStable() == new BigDecimal("27555.9024")
//    }
//
//    def "calculateAvgEntryPrice for non-stable coin"() {
//        given:
//        getSymbolHistoricPriceService.getPriceInUsdt(_, _, _) >> new BigDecimal("100.00")
//        CoinInformationResponse response = CoinInformationResponse.createEmpty("ETH")
//        List<Transaction> transactions = [
//                new Transaction(new TransactionId("ETHBTC", "SELL", "ETH", DateUtils.getLocalDateTime("2023-06-30T21:58:00"), new BigDecimal("0.06341"), new BigDecimal("0.071"), "BTC"), "BTC", new BigDecimal("0.0045"), null, null)
//        ]
//
//        when:
//        coinInformationHelper.calculateAvgEntryPrice(response, transactions)
//
//        then:
//        response.getTotalStable() == new BigDecimal("100.00")
//    }
//
//    def "calculateAvgEntryPrice for mixed transactions"() {
//        given:
//        getSymbolHistoricPriceService.getPriceInUsdt(_, _, _) >> new BigDecimal("100.00")
//        CoinInformationResponse response = CoinInformationResponse.createEmpty("ETH")
//
//        def transactionId1 = TransactionId.builder()
//                .pair("BTCUSDT").side("BUY").dateUtc(DateUtils.getLocalDateTime("2023-10-01 00:44:01")).price(26964.2).executed(0.005).build()
//        List<Transaction> transactions = [
//                Transaction.builder()
//                .transactionId(transactionId1)
//                        .symbol("BTC")
//                        .payedWith("USDT")
//                        .payedAmount(134.821)
//                        .build(),
//                new Transaction(new TransactionId("ETHBTC", "SELL", "ETH", DateUtils.getLocalDateTime("2023-06-30T21:58:00"), new BigDecimal("0.06341"), new BigDecimal("0.071"), "BTC"), "BTC", new BigDecimal("0.0045"), null, null)
//        ]
//
//        when:
//        coinInformationHelper.calculateAvgEntryPrice(response, transactions)
//
//        then:
//        response.getTotalStable() == new BigDecimal("13534.1024")
//    }
//
//    def "calculateAvgEntryPrice with no transactions"() {
//        given:
//        CoinInformationResponse response = CoinInformationResponse.createEmpty("ETH")
//
//        when:
//        coinInformationHelper.calculateAvgEntryPrice(response, [])
//
//        then:
//        response.getTotalStable() == BigDecimal.ZERO
//    }

    def "calculateAvgEntryPrice for BTC transactions"() {
        given:
        CoinInformationResponse response = CoinInformationResponse.builder()
                .coinName("BTC")
                .avgEntryPrice(new HashMap<>())
                .stableTotalCost(BigDecimal.ZERO)
                .amount(BigDecimal.ZERO)
                .build()

        List<Transaction> transactions = [
                Transaction.builder()
                        .transactionId(TransactionId.builder()
                                .pair("BTCUSDT")
                                .side("BUY")
                                .dateUtc(DateUtils.getLocalDateTime("2023-10-01 00:44:01"))
                                .price(new BigDecimal("26964.2"))
                                .executed(new BigDecimal("0.005"))
                                .build())
                        .symbol("BTC")
                        .payedWith("USDT")
                        .payedAmount(new BigDecimal("134.821"))
                        .build(),
                Transaction.builder()
                        .transactionId(TransactionId.builder()
                                .pair("BTCUSDT")
                                .side("SELL")
                                .dateUtc(DateUtils.getLocalDateTime("2023-08-29 20:28:28"))
                                .price(new BigDecimal("27915.83"))
                                .executed(new BigDecimal("0.0036"))
                                .build())
                        .symbol("BTC")
                        .payedWith("USDT")
                        .payedAmount(new BigDecimal("1004.96988"))
                        .build()
        ]

        response.totalExecuted = 0.005 - 0.0036

        when:
        coinInformationHelper.calculateAvgEntryPriceInStable(response, transactions)

        then:
        response.getAmount() == new BigDecimal("0.005").setScale(10, BigDecimal.ROUND_HALF_UP)
        response.getStableTotalCost() == new BigDecimal("27555.9024")
        response.getAvgEntryPrice().get("AVG") == new BigDecimal("5511180.4800")
    }

    def "calculateAvgEntryPrice for ETH transactions"() {
        given:
        CoinInformationResponse response = CoinInformationResponse.builder()
                .coinName("ETH")
                .avgEntryPrice(new HashMap<>())
                .stableTotalCost(BigDecimal.ZERO)
                .amount(BigDecimal.ZERO)
                .build()

        List<Transaction> transactions = [
                Transaction.builder()
                        .transactionId(TransactionId.builder()
                                .pair("ETHUSDT")
                                .side("BUY")
                                .dateUtc(DateUtils.getLocalDateTime("2023-09-11 20:17:48"))
                                .price(new BigDecimal("1553.21"))
                                .executed(new BigDecimal("0.100"))
                                .build())
                        .symbol("ETH")
                        .payedWith("USDT")
                        .payedAmount(new BigDecimal("155.321"))
                        .build(),
                Transaction.builder()
                        .transactionId(TransactionId.builder()
                                .pair("ETHBTC")
                                .side("SELL")
                                .dateUtc(DateUtils.getLocalDateTime("2023-06-30 21:58:00"))
                                .price(new BigDecimal("0.06341"))
                                .executed(new BigDecimal("0.071"))
                                .build())
                        .symbol("ETH")
                        .payedWith("BTC")
                        .payedAmount(new BigDecimal("0.0045"))
                        .build()
        ]

        when:
        coinInformationHelper.calculateAvgEntryPriceInStable(response, transactions)

        then:
        response.getAmount() == new BigDecimal("0.0295").setScale(10, BigDecimal.ROUND_HALF_UP)
        response.getStableTotalCost() == new BigDecimal("472.53571").setScale(10, BigDecimal.ROUND_HALF_UP)
        response.getAvgEntryPrice().get("AVG") == new BigDecimal("16005.7369491537")
    }

    def "calculateAvgEntryPrice with no transactions"() {
        given:
        CoinInformationResponse response = CoinInformationResponse.builder()
                .coinName("BTC")
                .avgEntryPrice(new HashMap<>())
                .stableTotalCost(BigDecimal.ZERO)
                .amount(BigDecimal.ZERO)
                .build()

        List<Transaction> transactions = []

        when:
        coinInformationHelper.calculateAvgEntryPriceInStable(response, transactions)

        then:
        response.getAmount() == BigDecimal.ZERO
        response.getStableTotalCost() == BigDecimal.ZERO
        response.getAvgEntryPrice().isEmpty()
    }

    private static LocalDateTime getLocalDateTime() {
        DateUtils.getLocalDateTime("2023-06-15 10:15:30")
    }
}
