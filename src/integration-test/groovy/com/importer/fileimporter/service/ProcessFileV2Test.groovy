package com.importer.fileimporter.service

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.entity.Transaction
import com.importer.fileimporter.utils.IntegrationTestHelper
import com.importer.fileimporter.utils.ProcessFileServiceUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Subject

import java.util.stream.Collectors

class ProcessFileV2Test extends BaseIntegrationSpec {

    @Autowired
    @Subject
    ProcessFileV2 processFile

    List<Map<String, String>> mockRows
    def symbolsInRows

    def setup() {
        mockRows = IntegrationTestHelper.readCsvFile()
        symbolsInRows = mockRows.stream()
                .map( { ProcessFileServiceUtils.getSymbolFromExecuted(it, null)} )
                .collect(Collectors.toSet())
    }

    def "should process file with portfolio and return file information response"() {
        given: "a multipart file with transaction data and predefined symbols"
        def multipartFile = getFile()
        def portfolioName = "default-portfolio"

        when: "processFile is called with a portfolio"
        def response = processFile.processFile(multipartFile, null, portfolioName)

        then: "file information response should contain the correct number of rows and transactions"
        response.amount == mockRows.size()
//        response.coinInformationResponse.size() == 12
        response.coinInformationResponse.each { coinInfo ->
            assert coinInfo.coinName in symbolsInRows
            assert coinInfo.totalTransactions > 0
            // Additional assertions for each coin's information
        }

        and: "the portfolio should be created"
        def portfolioCreated = portfolioService.getByName(portfolioName)
        portfolioCreated.isPresent()

        and: "transactions should be stored for the specific portfolio"
        List<Transaction> transactions = transactionService.findByPortfolio(portfolioCreated.get())
        transactions.size() == 26 // TODO: this is wrong until we fix transactionId
    }

    def "should process file with custom symbols and portfolio and return file information response"() {
        given: "a multipart file with transaction data and custom symbols"
        def file = getFile()
//        List<String> customSymbols = ["FET", "WAVES"]
        def portfolioName = "custom-portfolio"

        when: "processFile is called with custom symbols and a portfolio"
        def response = processFile.processFile(file, null, portfolioName)

        then: "file information response should contain the correct number of rows and transactions"
        response.amount == mockRows.size()
//        response.coinInformationResponse.size() == 2  // TODO: this is wrong as not only custom symbols are processed
        response.coinInformationResponse.each { coinInfo ->
            assert coinInfo.coinName in symbolsInRows
            assert coinInfo.totalTransactions > 0
            // Additional assertions for each coin's information
        }

        and: "the portfolio should be created"
        def portfolioCreated = portfolioService.getByName(portfolioName)
        portfolioCreated.isPresent()

        and: "transactions should be stored for the specific portfolio"
        List<Transaction> transactions = transactionService.findByPortfolio(portfolioCreated.get())
        transactions.size() == 26 // TODO: this is wrong until we fix transactionId
    }

    MultipartFile getFile() throws IOException {
        return new MockMultipartFile("test.csv", new FileInputStream(IntegrationTestHelper.getFile()))
    }
}
