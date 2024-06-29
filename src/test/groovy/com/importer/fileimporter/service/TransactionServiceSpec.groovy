package com.importer.fileimporter.service

import com.importer.fileimporter.dto.CoinInformationResponse
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.entity.TransactionId
import com.importer.fileimporter.repository.TransactionRepository
import com.importer.fileimporter.service.usecase.CalculateAmountSpent
import com.importer.fileimporter.utils.DateUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalDateTime

class TransactionServiceSpec extends Specification {

    TransactionRepository transactionRepository = Mock(TransactionRepository)
    CalculateAmountSpent calculateAmountSpent = Mock(CalculateAmountSpent)
    CoinInformationHelper coinInformationService = Mock(CoinInformationHelper)
    TransactionService transactionService = new TransactionService(transactionRepository, calculateAmountSpent, coinInformationService)

    def "test getTransactionsByRangeDate - symbol only"() {
        given:
        String symbol = "BTC"
        Pageable pageable = Pageable.unpaged()
        Page<Transaction> page = new PageImpl<>([])

        when:
        def result = transactionService.getTransactionsByRangeDate(symbol, null, null, pageable)

        then:
        1 * transactionRepository.findAllBySymbol(symbol, pageable) >> page
        result == page
    }

    def "test getTransactionsByRangeDate - date range"() {
        given:
        String symbol = "BTC"
        LocalDate startDate = LocalDate.of(2023, 1, 1)
        LocalDate endDate = LocalDate.of(2023, 1, 31)
        Pageable pageable = Pageable.unpaged()
        Page<Transaction> page = new PageImpl<>([])
        def startDateTime = startDate.atStartOfDay()
        def endDateTime = endDate.plusDays(1L).atStartOfDay().minusSeconds(1L)

        when:
        def result = transactionService.getTransactionsByRangeDate(symbol, startDate, endDate, pageable)

        then:
        1 * transactionRepository.findAllBySymbolOrSymbolIsNullAndTransactionIdDateUtcBetween(symbol, startDateTime, endDateTime, pageable) >> page
        result == page
    }

    def "test getAllBySymbol"() {
        given:
        String symbol = "ETH"
        Pageable pageable = Pageable.unpaged()
        Page<Transaction> page = new PageImpl<>([])

        when:
        def result = transactionService.getAllBySymbol(symbol, pageable)

        then:
        1 * transactionRepository.findAllBySymbol(symbol, pageable) >> page
        result == page
    }

    def "test getAll"() {
        given:
        List<Transaction> transactions = []

        when:
        def result = transactionService.getAll()

        then:
        1 * transactionRepository.findAll() >> transactions
        result == transactions
    }

    def "test getTransactionsInformation"() {
        given:
        String symbol = "BTC"
        LocalDate startDate = LocalDate.of(2023, 1, 1)
        LocalDate endDate = LocalDate.of(2023, 1, 31)
        Pageable pageable = Pageable.unpaged()
        List<Transaction> transactions = []
        BigDecimal amountSpent = BigDecimal.valueOf(100)
        CoinInformationResponse response = new CoinInformationResponse()

        when:
        def result = transactionService.getTransactionsInformation(symbol, startDate, endDate, pageable)

        then:
        1 * transactionRepository.findAllBySymbolOrSymbolIsNullAndTransactionIdDateUtcBetween(symbol, startDate.atStartOfDay(), endDate.plusDays(1L).atStartOfDay().minusSeconds(1L), pageable) >> new PageImpl<>(transactions)
        1 * calculateAmountSpent.execute(symbol, transactions) >> amountSpent
        1 * coinInformationService.calculateAvgEntryPrice(_, _) >> { CoinInformationResponse resp, List<Transaction> trans -> }
        result != null
        result.coinName == symbol
        result.usdSpent == amountSpent
        result.totalTransactions == transactions.size()
    }

    def "test saveTransaction"() {
        given:
        String coinName = "BTC"
        String symbolPair = "USD"
        String date = "2023-01-01 00:00:00"
        String pair = "BTCUSD"
        String side = "BUY"
        BigDecimal price = BigDecimal.valueOf(20000)
        BigDecimal executed = BigDecimal.valueOf(1)
        BigDecimal amount = BigDecimal.valueOf(20000)
        BigDecimal fee = BigDecimal.valueOf(10)
        String origin = "test-origin"

        LocalDateTime dateTime = DateUtils.getLocalDateTime(date)
        TransactionId transactionId = TransactionId.builder()
                .dateUtc(dateTime)
                .pair(pair)
                .executed(executed)
                .side(side)
                .price(price)
                .build()

        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .symbol(coinName)
                .payedWith(symbolPair)
                .payedAmount(amount)
                .created(LocalDateTime.now())
                .createdBy(origin)
                .feeAmount(fee)
                .modified(LocalDateTime.now())
                .modifiedBy(origin)
                .build()

        when:
        def result = transactionService.saveTransaction(coinName, symbolPair, date, pair, side, price, executed, amount, fee, origin)

        then:
        1 * transactionRepository.save(_) >> transaction
        result == transaction
        result.transactionId == transactionId
        result.symbol == coinName
        result.payedWith == symbolPair
        result.payedAmount == amount
        result.feeAmount == fee
    }
}
