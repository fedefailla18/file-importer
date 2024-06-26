package com.importer.fileimporter.service

import com.importer.fileimporter.dto.FileInformationResponse
import com.importer.fileimporter.utils.OperationUtils
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification
import spock.lang.Subject

import static com.importer.fileimporter.utils.TestHelper.readCsvFile

class ProcessFileSpec extends Specification {

    List<String> allSymbols = 'SOL,WAVES,AEVO,FET,IMX,XRP,ETH,BAND,NEAR,TIA,FTM,ADA'.split(',')

    @Subject
    ProcessFile processFile

    FileImporterService fileImporterService = Mock()
    TransactionService transactionService = Mock()
    SymbolService symbolService = Mock()

    def setup() {
        processFile = new ProcessFile(fileImporterService, transactionService, symbolService)
    }

    def "should process file and return file information response"() {
        given: "a multipart file with transaction data and predefined symbols"
        MultipartFile file = Mock(MultipartFile)
        List<Map<String, String>> mockRows = readCsvFile()
        List<String> symbols = OperationUtils.SYMBOL // this is not used anymore

        and: "file importer service returns rows"
        fileImporterService.getRows(file) >> mockRows

        and: "symbol service returns default symbols if symbols are not provided"
        symbolService.getAllSymbols() >> symbols

        when: "processFile is called"
        FileInformationResponse response = processFile.processFile(file)

        then: "file information response should contain the correct number of rows and transactions"
        response.amount == mockRows.size()
        response.coinInformationResponse.size() == allSymbols.size()
        response.coinInformationResponse.each { coinInfo ->
            assert coinInfo.coinName in allSymbols
            assert coinInfo.totalTransactions > 0
            // Additional assertions for each coin's information
        }
    }

    def "should process file with custom symbols and return file information response"() {
        given: "a multipart file with transaction data and custom symbols"
        MultipartFile file = Mock(MultipartFile)
        List<Map<String, String>> mockRows = readCsvFile()
        List<String> customSymbols = ["FET", "WAVES"]

        and: "file importer service returns rows"
        fileImporterService.getRows(file) >> mockRows

        when: "processFile is called with custom symbols"
        FileInformationResponse response = processFile.processFile(file, customSymbols)

        then: "file information response should contain the correct number of rows and transactions"
        response.amount == mockRows.size()
        response.coinInformationResponse.size() == allSymbols.size()
        response.coinInformationResponse.each { coinInfo ->
            assert coinInfo.coinName in allSymbols
            assert coinInfo.totalTransactions > 0
            // Additional assertions for each coin's information
        }
    }
}
