package com.importer.fileimporter.service

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.dto.FileInformationResponse
import com.importer.fileimporter.utils.IntegrationTestHelper
import com.importer.fileimporter.utils.OperationUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Subject

class ProcessFileIntegrationSpec extends BaseIntegrationSpec {

    List<String> allSymbols = 'SOL,WAVES,AEVO,FET,IMX,XRP,ETH,BAND,NEAR,TIA,FTM,ADA'.split(',')

    @Autowired
    @Subject
    ProcessFile processFile

    @Autowired
    FileImporterService fileImporterService

    @Autowired
    TransactionService transactionService

    @Autowired
    SymbolService symbolService

    def "should process file and return file information response"() {
        given: "a multipart file with transaction data and predefined symbols"
        MultipartFile multipartFile = getFile()

        List<Map<String, String>>  mockRows = IntegrationTestHelper.readCsvFile()
        List<String> symbols = OperationUtils.SYMBOL

//        and: "the file has been uploaded"
//        fileImporterService.getRows(multipartFile) >> mockRows

        when: "processFile is called"
        FileInformationResponse response = processFile.processFile(multipartFile)

        then: "file information response should contain the correct number of rows and transactions"
        response.amount == mockRows.size()
        response.coinInformationResponse.size() == 12
        response.coinInformationResponse.each { coinInfo ->
            assert coinInfo.coinName in allSymbols
            assert coinInfo.totalTransactions > 0
            // Additional assertions for each coin's information
        }
    }

    def "should process file with custom symbols and return file information response"() {
        given: "a multipart file with transaction data and custom symbols"
        MultipartFile file = getFile()
        List<Map<String, String>> mockRows = IntegrationTestHelper.readCsvFile()
        List<String> customSymbols = ["FET", "WAVES"]

//        and: "the file has been uploaded"
//        fileImporterService.getRows(file) >> mockRows

        when: "processFile is called with custom symbols"
        FileInformationResponse response = processFile.processFile(file, customSymbols)

        then: "file information response should contain the correct number of rows and transactions"
        response.amount == mockRows.size()
        response.coinInformationResponse.size() == 12
        response.coinInformationResponse.each { coinInfo ->
            assert coinInfo.coinName in allSymbols
            assert coinInfo.totalTransactions > 0
            // Additional assertions for each coin's information
        }
    }

    MultipartFile getFile() {
        new MockMultipartFile("test.csv", new FileInputStream(IntegrationTestHelper.getFile()));
    }
}
