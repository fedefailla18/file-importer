package com.importer.fileimporter.dto


import spock.lang.Specification

class CoinInformationResponseSpec extends Specification {

    def "setAvgEntryPrice for buy transaction"() {
        given:
        CoinInformationResponse response = CoinInformationResponse.builder()
                .coinName("BTC")
                .stableTotalCost(BigDecimal.ZERO)
                .amount(BigDecimal.ZERO)
                .build()

        when:
        response.setAvgEntryPriceInUsdt(new BigDecimal("1000"), new BigDecimal("1"), true)

        then:
        response.getStableTotalCost() == new BigDecimal("1000")
        response.getAmount() == new BigDecimal("1")
    }

    def "setAvgEntryPrice for sell transaction"() {
        given:
        CoinInformationResponse response = CoinInformationResponse.builder()
                .coinName("BTC")
                .stableTotalCost(BigDecimal.ZERO)
                .amount(BigDecimal.ZERO)
                .build()

        when:
        response.setAvgEntryPriceInUsdt(new BigDecimal("1000"), new BigDecimal("1"), true) // Initial buy
        response.setAvgEntryPriceInUsdt(new BigDecimal("500"), new BigDecimal("0.5"), false) // Sell half

        then:
        response.getStableTotalCost() == new BigDecimal("750")
        response.getAmount() == new BigDecimal("0.5")
    }

    def "calculateAvgPrice"() {
        given:
        CoinInformationResponse response = CoinInformationResponse.builder()
                .coinName("BTC")
                .avgEntryPrice(new HashMap<>())
                .stableTotalCost(new BigDecimal("1500"))
                .totalExecuted(new BigDecimal("1"))
                .build()

        when:
        response.calculateAvgPrice()

        then:
        response.getTotalStablePosition() == new BigDecimal("1500.0000000000")
    }

    def "calculateAvgPrice with no executed amount"() {
        given:
        CoinInformationResponse response = CoinInformationResponse.builder()
                .coinName("BTC")
                .avgEntryPrice(new HashMap<>())
                .stableTotalCost(BigDecimal.ZERO)
                .totalExecuted(BigDecimal.ZERO)
                .totalStablePosition(BigDecimal.ZERO)
                .build()

        when:
        response.calculateAvgPrice()

        then:
        response.getTotalStablePosition() == BigDecimal.ZERO
    }

    def "addTotalAmountBought"() {
        given:
        def coinInfo = CoinInformationResponse.builder()
                .totalAmountBought(amountBought)
                .build()

        when:
        coinInfo.addTotalAmountBought(purchasedAmount, side)

        then:
        coinInfo != null
        coinInfo.totalAmountBought == expected

        where:
        purchasedAmount | amountBought    | side   || expected
        BigDecimal.ZERO | BigDecimal.ZERO | "BUY"  || BigDecimal.ZERO
        1               | 1               | "BUY"  || 2
        1               | BigDecimal.ZERO | "BUY"  || 1
        BigDecimal.ZERO | BigDecimal.ZERO | "SELL" || BigDecimal.ZERO
        1               | 1               | "SELL" || 1
        1               | BigDecimal.ZERO | "SELL" || 0
    }


}
