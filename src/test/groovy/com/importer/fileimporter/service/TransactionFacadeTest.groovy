package com.importer.fileimporter.service

import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.entity.TransactionId
import com.importer.fileimporter.service.usecase.CalculateAmountSpent
import spock.lang.Specification

import java.time.LocalDateTime

class TransactionFacadeTest extends Specification {

    def transactionService = Mock(TransactionService)
    def calculateAmountSpent = Mock(CalculateAmountSpent)
    def coinInformationService = Mock(CoinInformationService)
    def symbolService = Mock(SymbolService)
    def portfolioService = Mock(PortfolioService)
    def getSymbolHistoricPriceService = Mock(GetSymbolHistoricPriceService)

    def sut = new TransactionFacade(transactionService, calculateAmountSpent,
            coinInformationService, symbolService, portfolioService, getSymbolHistoricPriceService)

    def "BuildPortfolio"() {
    }

    def "GetTotalAmount"() {
        given:
        def transactions = createMockTransactions()

        when:
        def totalAmount = sut.getTotalAmount(transactions).get()

        then:
        totalAmount == BigDecimal.valueOf(50)
    }

    public static List<Transaction> createMockTransactions() {
        List<Transaction> transactions = new ArrayList<>();

        def transactionId1 = TransactionId.builder()
                .dateUtc(LocalDateTime.now())
                .side('BUY').pair("BTCUSD")
                .executed(BigDecimal.valueOf(100))
                .price(BigDecimal.valueOf(50))
                .build()
        // Create mock transactions
        Transaction transaction1 = Transaction.builder()
                .id(1L)
                .transactionId(transactionId1)
                .symbol("BTC")
                .payedWith("USD")
                .payedAmount(BigDecimal.valueOf(5000))
                .fee("5USD")
                .feeAmount(BigDecimal.valueOf(5))
                .feeSymbol("USD")
                .created(LocalDateTime.now())
                .createdBy("user1")
                .modified(LocalDateTime.now())
                .modifiedBy("user1")
                .build();

        def transactionId2 = TransactionId.builder()
                .dateUtc(LocalDateTime.now())
                .side('SELL')
                .pair("BTCUSD")
                .executed(BigDecimal.valueOf(50))
                .price(BigDecimal.valueOf(100))
                .build()
        Transaction transaction2 = Transaction.builder()
                .id(2L)
                .transactionId(transactionId2)
                .symbol("BTC")
                .payedWith("USD")
                .payedAmount(BigDecimal.valueOf(5000))
                .fee("5USD")
                .feeAmount(BigDecimal.valueOf(5))
                .feeSymbol("USD")
                .created(LocalDateTime.now())
                .createdBy("user2")
                .modified(LocalDateTime.now())
                .modifiedBy("user2")
                .build();

        // Add transactions to the list
        transactions.add(transaction1);
        transactions.add(transaction2);

        return transactions;
    }
}
