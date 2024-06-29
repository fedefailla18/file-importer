package com.importer.fileimporter.dto


import spock.lang.Specification

class CoinInformationResponseSpec extends Specification {

    def "setAvgEntryPrice for buy transaction"() {
        given:
        CoinInformationResponse response = CoinInformationResponse.builder()
                .coinName("BTC")
                .usdSpent(BigDecimal.ZERO)
                .amount(BigDecimal.ZERO)
                .build()

        when:
        response.setAvgEntryPrice("USDT", new BigDecimal("1000"), new BigDecimal("1"), true)

        then:
        response.getUsdSpent() == new BigDecimal("1000")
        response.getAmount() == new BigDecimal("1")
    }

    def "setAvgEntryPrice for sell transaction"() {
        given:
        CoinInformationResponse response = CoinInformationResponse.builder()
                .coinName("BTC")
                .usdSpent(BigDecimal.ZERO)
                .amount(BigDecimal.ZERO)
                .build()

        when:
        response.setAvgEntryPrice("USDT", new BigDecimal("1000"), new BigDecimal("1"), true) // Initial buy
        response.setAvgEntryPrice("USDT", new BigDecimal("500"), new BigDecimal("0.5"), false) // Sell half

        then:
        response.getUsdSpent() == new BigDecimal("750")
        response.getAmount() == new BigDecimal("0.5")
    }

    def "calculateAvgPrice"() {
        given:
        CoinInformationResponse response = CoinInformationResponse.builder()
                .coinName("BTC")
                .avgEntryPrice(new HashMap<>())
                .usdSpent(new BigDecimal("1500"))
                .totalExecuted(new BigDecimal("1"))
                .build()

        when:
        response.calculateAvgPrice()

        then:
        response.getTotalStable() == new BigDecimal("1500.0000000000")
    }

    def "calculateAvgPrice with no executed amount"() {
        given:
        CoinInformationResponse response = CoinInformationResponse.builder()
                .coinName("BTC")
                .avgEntryPrice(new HashMap<>())
                .usdSpent(BigDecimal.ZERO)
                .totalExecuted(BigDecimal.ZERO)
                .totalStable(BigDecimal.ZERO)
                .build()

        when:
        response.calculateAvgPrice()

        then:
        response.getTotalStable() == BigDecimal.ZERO
    }


}
