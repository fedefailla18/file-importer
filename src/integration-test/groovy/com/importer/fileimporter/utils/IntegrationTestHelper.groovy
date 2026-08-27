package com.importer.fileimporter.utils

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema

class IntegrationTestHelper {

    static List<Map<String, String>> readCsvFile() {
        File file = getFile()
        println "Reading file from path: ${file.getAbsolutePath()}"

        CsvMapper mapper = new CsvMapper()
        CsvSchema schema = CsvSchema.emptySchema().withHeader().withColumnSeparator((char)',').withQuoteChar((char)'"')
        
        return mapper.readerFor(Map.class).with(schema).readValues(file).readAll() as List<Map<String, String>>
    }

    static def getFile() {
        String path = '/integrationTest/bnb-sample_transactions.csv'
        URL resourceUrl = IntegrationTestHelper.class.getResource(path)

        if (resourceUrl != null) {
            return new File(resourceUrl.toURI())
        }

        // Fallback to project root relative path
        File file = new File("src/integration-test/resources" + path)
        if (file.exists()) {
            return file
        }

        println "File not found in classpath or project root: ${path}"
        throw new IllegalStateException("File not found: " + path)
    }
}
