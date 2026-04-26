package com.importer.fileimporter.service

import com.importer.fileimporter.dto.integration.binance.BinanceAccountResponse
import com.importer.fileimporter.dto.integration.binance.BinanceTradeResponse
import com.importer.fileimporter.entity.ExchangeName
import com.importer.fileimporter.entity.User
import com.importer.fileimporter.entity.UserExchangeConfig
import com.importer.fileimporter.repository.UserExchangeConfigRepository
import spock.lang.Specification

import java.math.BigDecimal

class BinanceSpotActivityServiceSpec extends Specification {

    def binanceApiService = Mock(BinanceApiService)
    def userExchangeConfigRepository = Mock(UserExchangeConfigRepository)
    def encryptionService = Mock(EncryptionService)

    def service = new BinanceSpotActivityService(
            binanceApiService,
            userExchangeConfigRepository,
            encryptionService
    )

    def user = Mock(User)

    private UserExchangeConfig configWith(Long lastSyncTimestamp = 123456L) {
        Mock(UserExchangeConfig) {
            getApiKey() >> 'api-key'
            getApiSecret() >> 'encrypted-secret'
            getLastSyncTimestamp() >> lastSyncTimestamp
        }
    }

    private BinanceAccountResponse.AssetBalance balance(String asset, String free, String locked = '0') {
        Stub(BinanceAccountResponse.AssetBalance) {
            getAsset() >> asset
            getFree() >> new BigDecimal(free)
            getLocked() >> new BigDecimal(locked)
        }
    }

    private BinanceTradeResponse trade(
            String symbol,
            long id,
            boolean buyer,
            String qty,
            String quoteQty,
            long time
    ) {
        Stub(BinanceTradeResponse) {
            getSymbol() >> symbol
            getId() >> id
            getOrderId() >> (1000L + id)
            getOrderListId() >> -1L
            getPrice() >> new BigDecimal('2.5')
            getQty() >> new BigDecimal(qty)
            getQuoteQty() >> new BigDecimal(quoteQty)
            getCommission() >> new BigDecimal('0.01')
            getCommissionAsset() >> 'BNB'
            getTime() >> time
            getIsBuyer() >> buyer
            getIsMaker() >> false
            getIsBestMatch() >> true
        }
    }

    def "should return fresh balances and raw spot trade summary"() {
        given:
        userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.BINANCE) >> Optional.of(configWith())
        encryptionService.decrypt('encrypted-secret') >> 'plain-secret'
        def accountInfo = Stub(BinanceAccountResponse) {
            getBalances() >> [
                    balance('FET', '12.5'),
                    balance('USDT', '100'),
                    balance('BTC', '0')
            ]
        }
        binanceApiService.getAccountInfo('api-key', 'plain-secret') >> accountInfo
        binanceApiService.getAllMyTrades('api-key', 'plain-secret', 'FETUSDT') >> [
                trade('FETUSDT', 1L, true, '10', '25', 1_700_000_000_000L),
                trade('FETUSDT', 2L, false, '2', '6', 1_700_000_100_000L)
        ]
        binanceApiService.getAllMyTrades('api-key', 'plain-secret', 'FETBTC') >> []
        binanceApiService.getAllMyTrades('api-key', 'plain-secret', 'FETETH') >> []
        binanceApiService.getAllMyTrades('api-key', 'plain-secret', 'FETBNB') >> []
        binanceApiService.getAllMyTrades('api-key', 'plain-secret', 'FETBUSD') >> []
        binanceApiService.getAllMyTrades('api-key', 'plain-secret', 'FETUSDC') >> []
        binanceApiService.getAllMyTrades('api-key', 'plain-secret', 'FETFDUSD') >> []

        when:
        def response = service.getSpotActivity(user)

        then:
        response.balances*.asset == ['USDT', 'FET']
        response.summary.activeAssetCount == 2
        response.summary.totalTradeCount == 2
        response.summary.buyTradeCount == 1
        response.summary.sellTradeCount == 1
        response.summary.symbolCountWithTrades == 1
        response.summary.grossBuyQuoteQty == new BigDecimal('25')
        response.summary.grossSellQuoteQty == new BigDecimal('6')
        response.summary.lastSyncTimestamp == 123456L
        response.trades[0].tradeId == 2L
        response.trades[0].side == 'SELL'
    }
}
