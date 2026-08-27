package com.importer.fileimporter.service

import com.importer.fileimporter.dto.integration.binance.BinanceAccountResponse
import com.importer.fileimporter.dto.integration.binance.BinanceTradeResponse
import com.importer.fileimporter.entity.ExchangeName
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.User
import com.importer.fileimporter.entity.UserExchangeConfig
import com.importer.fileimporter.repository.UserExchangeConfigRepository
import spock.lang.Specification

import java.math.BigDecimal

class BinanceSyncServiceSpec extends Specification {

    def binanceApiService = Mock(BinanceApiService)
    def userExchangeConfigRepository = Mock(UserExchangeConfigRepository)
    def encryptionService = Mock(EncryptionService)
    def transactionProcessor = Mock(TransactionProcessor)
    def portfolioService = Mock(PortfolioService)

    def service = new BinanceSyncService(
            binanceApiService,
            userExchangeConfigRepository,
            encryptionService,
            transactionProcessor,
            portfolioService
    )

    def user = Mock(User) { getUsername() >> "testuser" }
    def portfolio = new Portfolio(name: "TestPortfolio")

    def setup() {
        service.rateLimitDelayMs = 0L
    }

    private UserExchangeConfig configFor(String apiKey, Long lastSync = null) {
        Mock(UserExchangeConfig) {
            getApiKey() >> apiKey
            getApiSecret() >> "encrypted-secret"
            getLastSyncTimestamp() >> lastSync
        }
    }

    private BinanceAccountResponse.AssetBalance balance(String asset, double free) {
        Mock(BinanceAccountResponse.AssetBalance) {
            getAsset() >> asset
            getFree() >> new BigDecimal(free)
            getLocked() >> BigDecimal.ZERO
        }
    }

    private BinanceAccountResponse accountWith(List<String> investmentAssets) {
        def balances = investmentAssets.collect { balance(it, 1.0) }
        Mock(BinanceAccountResponse) {
            getBalances() >> balances
        }
    }

    private BinanceTradeResponse trade(String symbol, boolean isBuyer = true) {
        Mock(BinanceTradeResponse) {
            getSymbol() >> symbol
            getIsBuyer() >> isBuyer
            getTime() >> System.currentTimeMillis()
            getPrice() >> new BigDecimal("1.0")
            getQty() >> new BigDecimal("10.0")
            getQuoteQty() >> new BigDecimal("10.0")
            getCommission() >> new BigDecimal("0.01")
            getCommissionAsset() >> "BNB"
        }
    }

    def "should throw when no Binance config exists for user"() {
        given:
        userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.BINANCE) >> Optional.empty()

        when:
        service.sync(user, "TestPortfolio")

        then:
        thrown(IllegalArgumentException)
        0 * binanceApiService.getMyTrades(*_)
    }

    def "should fetch trades for investment asset pairs and process them"() {
        given:
        def config = configFor("api-key")
        userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.BINANCE) >> Optional.of(config)
        encryptionService.decrypt("encrypted-secret") >> "plain-secret"
        portfolioService.findOrSave("TestPortfolio") >> portfolio
        binanceApiService.getServerTime() >> System.currentTimeMillis()
        binanceApiService.getAccountInfo("api-key", "plain-secret") >> accountWith(["FET"])

        // FETUSDT exists and has one trade; other FET pairs return empty
        binanceApiService.getMyTrades("api-key", "plain-secret", "FETUSDT", null) >> [trade("FETUSDT")]
        binanceApiService.getMyTrades("api-key", "plain-secret", _, null) >> []

        when:
        service.sync(user, "TestPortfolio")

        then:
        1 * transactionProcessor.process(_)
        1 * config.setLastSyncTimestamp(_ as Long)
        1 * userExchangeConfigRepository.save(config)
    }

    def "should not fetch any trades when all assets are quote currencies"() {
        given:
        def config = configFor("api-key")
        userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.BINANCE) >> Optional.of(config)
        encryptionService.decrypt("encrypted-secret") >> "plain-secret"
        portfolioService.findOrSave("TestPortfolio") >> portfolio
        binanceApiService.getServerTime() >> System.currentTimeMillis()
        // User holds only USDT and BTC — both are quote currencies, 0 investment assets
        binanceApiService.getAccountInfo("api-key", "plain-secret") >> accountWith(["USDT", "BTC"])

        when:
        service.sync(user, "TestPortfolio")

        then:
        0 * binanceApiService.getMyTrades(*_)
        1 * config.setLastSyncTimestamp(_ as Long)
    }

    def "should skip -1121 invalid symbol errors silently and continue with other pairs"() {
        given:
        def config = configFor("api-key")
        userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.BINANCE) >> Optional.of(config)
        encryptionService.decrypt("encrypted-secret") >> "plain-secret"
        portfolioService.findOrSave("TestPortfolio") >> portfolio
        binanceApiService.getServerTime() >> System.currentTimeMillis()
        binanceApiService.getAccountInfo("api-key", "plain-secret") >> accountWith(["RLC"])

        // All pairs for RLC except RLCBTC throw -1121; RLCBTC has a trade
        binanceApiService.getMyTrades("api-key", "plain-secret", "RLCUSDT", null) >>
                { throw new RuntimeException("Binance API error: {\"code\":-1121,\"msg\":\"Invalid symbol.\"}") }
        binanceApiService.getMyTrades("api-key", "plain-secret", "RLCBTC", null) >> [trade("RLCBTC")]
        binanceApiService.getMyTrades("api-key", "plain-secret", _, null) >> []

        when:
        service.sync(user, "TestPortfolio")

        then:
        noExceptionThrown()
        1 * transactionProcessor.process(_)
    }

    def "should pass lastSyncTimestamp to getMyTrades on incremental sync"() {
        given:
        long previousSync = 1_000_000L
        def config = configFor("api-key", previousSync)
        userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.BINANCE) >> Optional.of(config)
        encryptionService.decrypt("encrypted-secret") >> "plain-secret"
        portfolioService.findOrSave("TestPortfolio") >> portfolio
        binanceApiService.getServerTime() >> System.currentTimeMillis()
        binanceApiService.getAccountInfo("api-key", "plain-secret") >> accountWith(["DOT"])
        binanceApiService.getMyTrades("api-key", "plain-secret", _, _) >> []

        when:
        service.sync(user, "TestPortfolio")

        then:
        // Every getMyTrades call must use the previousSync timestamp, not null
        (1.._) * binanceApiService.getMyTrades("api-key", "plain-secret", _, previousSync)
    }

    def "should check all 7 quote currencies for each investment asset"() {
        given:
        def config = configFor("api-key")
        userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.BINANCE) >> Optional.of(config)
        encryptionService.decrypt("encrypted-secret") >> "plain-secret"
        portfolioService.findOrSave("TestPortfolio") >> portfolio
        binanceApiService.getServerTime() >> System.currentTimeMillis()
        binanceApiService.getAccountInfo("api-key", "plain-secret") >> accountWith(["NEAR"])
        binanceApiService.getMyTrades(*_) >> []

        when:
        service.sync(user, "TestPortfolio")

        then:
        // NEAR × 7 quote currencies = 7 calls
        7 * binanceApiService.getMyTrades("api-key", "plain-secret", _, null)
    }
}
