package com.importer.fileimporter.service

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.dto.BinanceTransactionAdapter
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.utils.IntegrationTestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Subject

import java.util.stream.Collectors

class ProcessFileV2IntegrationSpec extends BaseIntegrationSpec {

    @Autowired
    @Subject
    ProcessFileV2 processFile

    List<Map<String, String>> mockRows
    def symbolsInRows

    def setup() {
        mockRows = IntegrationTestHelper.readCsvFile()
        // Use the adapter pattern to extract symbols
        symbolsInRows = mockRows.stream()
                .map( { row -> 
                    def adapter = new BinanceTransactionAdapter(row)
                    return adapter.getSymbol()
                } )
                .filter( { it != null } )
                .map( { it.trim() } )
                .collect(Collectors.toSet())
    }

    def "should process file with portfolio and return file information response"() {
        given: "a multipart file with transaction data and predefined symbols"
        def multipartFile = getFile()
        def portfolioName = "Binance"

        when: "processFile is called with a portfolio"
        def response = processFile.processFile(multipartFile, null, portfolioName, "BINANCE")
        println "[DEBUG_LOG] response: $response"
        println "[DEBUG_LOG] mockRows size: ${mockRows.size()}"
        println "[DEBUG_LOG] mockRows[0]: ${mockRows[0]}"
        println "[DEBUG_LOG] symbolsInRows: $symbolsInRows"

        then: "file information response should contain the correct number of rows and transactions"
        println "[DEBUG_LOG] response.coinInformationResponse.size(): ${response.coinInformationResponse.size()}"
        println "[DEBUG_LOG] symbolsInRows.size(): ${symbolsInRows.size()}"
        println "[DEBUG_LOG] symbolsInRows: $symbolsInRows"
        println "[DEBUG_LOG] response.coinInformationResponse.coinName: ${response.coinInformationResponse.collect { it.coinName }}"
        response.portfolio == portfolioName
        response.amount == mockRows.size()
        response.coinInformationResponse.size() > 0
        response.coinInformationResponse.each { coinInfo ->
            assert coinInfo.coinName in symbolsInRows
        }

        and: "the portfolio should be created"
        def portfolioCreated = portfolioRepository.findAllByName(portfolioName)
        !portfolioCreated.isEmpty()

        and: "transactions should be stored for the specific portfolio"
        List<Transaction> transactions = transactionService.findByPortfolio(portfolioCreated.get(0))
        transactions.size() > 0
    }

    def "should process file with custom symbols and portfolio and return file information response"() {
        given: "a multipart file with transaction data and custom symbols"
        def file = getFile()
        def portfolioName = "Binance"

        when: "processFile is called with custom symbols and a portfolio"
        def response = processFile.processFile(file, null, portfolioName, "BINANCE")

        then: "file information response should contain the correct number of rows and transactions"
        println "[DEBUG_LOG] response.coinInformationResponse.size(): ${response.coinInformationResponse.size()}"
        println "[DEBUG_LOG] symbolsInRows.size(): ${symbolsInRows.size()}"
        println "[DEBUG_LOG] symbolsInRows: $symbolsInRows"
        println "[DEBUG_LOG] response.coinInformationResponse.coinName: ${response.coinInformationResponse.collect { it.coinName }}"
        response.portfolio == portfolioName
        response.amount == mockRows.size()
        response.coinInformationResponse.size() > 0
        response.coinInformationResponse.each { coinInfo ->
            assert coinInfo.coinName in symbolsInRows
        }

        and: "the portfolio should be created"
        def portfolioCreated = portfolioRepository.findAllByName(portfolioName)
        !portfolioCreated.isEmpty()

        and: "transactions should be stored for the specific portfolio"
        List<Transaction> transactions = transactionService.findByPortfolio(portfolioCreated.get(0))
        transactions.size() > 0
    }

    MultipartFile getFile() throws IOException {
        return new MockMultipartFile("test.csv", new FileInputStream(IntegrationTestHelper.getFile()))
    }
}
