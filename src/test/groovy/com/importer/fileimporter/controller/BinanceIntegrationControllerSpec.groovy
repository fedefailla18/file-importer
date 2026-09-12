package com.importer.fileimporter.controller

import com.importer.fileimporter.dto.integration.binance.BinanceOrderResponse
import com.importer.fileimporter.dto.integration.binance.BinanceTradeResponse
import com.importer.fileimporter.entity.ExchangeName
import com.importer.fileimporter.entity.User
import com.importer.fileimporter.entity.UserExchangeConfig
import com.importer.fileimporter.repository.BinanceRawOrderRepository
import com.importer.fileimporter.repository.BinanceSyncProgressRepository
import com.importer.fileimporter.repository.UserExchangeConfigRepository
import com.importer.fileimporter.service.BinanceApiService
import com.importer.fileimporter.service.BinanceAsyncSyncService
import com.importer.fileimporter.service.EncryptionService
import spock.lang.Specification

/**
 * Regression coverage for the 2026-09-12 incident: getMyTradesProxy/getAllOrdersProxy used to
 * window a multi-year default range into 180-day (SIX_MONTHS_MS) chunks and call
 * BinanceApiService with BOTH startTime and endTime set on every chunk. Binance's real API
 * 400s /myTrades and /allOrders whenever both timestamps are set more than 24h apart — this
 * had zero test coverage, so it went unnoticed until a real user hit it with a real 2020-2026
 * default range. These tests pin the fixed behavior: never call with both timestamps set,
 * page by id instead, and filter the requested window client-side.
 */
class BinanceIntegrationControllerSpec extends Specification {

    def binanceApiService = Mock(BinanceApiService)
    def userExchangeConfigRepository = Mock(UserExchangeConfigRepository)
    def encryptionService = Mock(EncryptionService)
    def binanceAsyncSyncService = Mock(BinanceAsyncSyncService)
    def rawOrderRepository = Mock(BinanceRawOrderRepository)
    def syncProgressRepository = Mock(BinanceSyncProgressRepository)

    def controller = new BinanceIntegrationController(
            binanceApiService,
            userExchangeConfigRepository,
            encryptionService,
            binanceAsyncSyncService,
            rawOrderRepository,
            syncProgressRepository
    )

    def user = Mock(User)

    def config() {
        Mock(UserExchangeConfig) {
            getApiKey() >> "api-key"
            getApiSecret() >> "encrypted-secret"
        }
    }

    def trade(Long id, String symbol, long time) {
        Mock(BinanceTradeResponse) {
            getId() >> id
            getSymbol() >> symbol
            getTime() >> time
        }
    }

    def order(Long orderId, long time) {
        Mock(BinanceOrderResponse) {
            getOrderId() >> orderId
            getTime() >> time
        }
    }

    def setup() {
        userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.BINANCE) >> Optional.of(config())
        encryptionService.decrypt("encrypted-secret") >> "plain-secret"
    }

    def "getMyTradesProxy never sends both startTime and endTime to Binance"() {
        given:
        binanceApiService.getMyTrades("api-key", "plain-secret", "BTCUSDT", _ as Long, null, null) >>
                [trade(1L, "BTCUSDT", 1600000000000L)]
        binanceApiService.getMyTrades("api-key", "plain-secret", "BTCUSDT", null, null, 2L) >> []

        when:
        def response = controller.getMyTradesProxy(user, "BTCUSDT", "2020-01-01", null)

        then:
        // The regression: this must NEVER be invoked with both startTime and endTime non-null.
        0 * binanceApiService.getMyTrades("api-key", "plain-secret", "BTCUSDT", _ as Long, _ as Long, _)
        response.statusCode.is2xxSuccessful()
        response.body.size() == 1
    }

    def "getMyTradesProxy filters results to the requested window"() {
        given:
        def inWindow = trade(1L, "BTCUSDT", 1_600_000_000_000L)
        def outOfWindow = trade(2L, "BTCUSDT", 1_700_000_000_000L)
        binanceApiService.getMyTrades("api-key", "plain-secret", "BTCUSDT", _ as Long, null, null) >>
                [inWindow, outOfWindow]
        binanceApiService.getMyTrades("api-key", "plain-secret", "BTCUSDT", null, null, _) >> []

        when:
        def response = controller.getMyTradesProxy(user, "BTCUSDT", "2020-01-01", 1_650_000_000_000L)

        then:
        response.body*.getId() == [1L]
    }

    def "getAllOrdersProxy never sends both startTime and endTime to Binance"() {
        given:
        binanceApiService.getAllOrders("api-key", "plain-secret", "BTCUSDT", _ as Long, null, null) >>
                [order(1L, 1600000000000L)]
        binanceApiService.getAllOrders("api-key", "plain-secret", "BTCUSDT", null, null, 2L) >> []

        when:
        def response = controller.getAllOrdersProxy(user, "BTCUSDT", "2020-01-01", null)

        then:
        0 * binanceApiService.getAllOrders("api-key", "plain-secret", "BTCUSDT", _ as Long, _ as Long, _)
        response.body.size() == 1
    }
}
