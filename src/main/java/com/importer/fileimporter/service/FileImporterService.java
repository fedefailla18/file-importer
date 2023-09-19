package com.importer.fileimporter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.base.Strings;
import liquibase.repackaged.com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileImporterService {

    public List<Map<?, ?>> downloadAndParseCsv(String url) {

        if (Strings.isNullOrEmpty(url)) {
            return null;
        }

        try {
            File input = File.createTempFile(UUID.randomUUID().toString(), ".csv");
            FileUtils.copyURLToFile(new URL(url), input);
            List<Map<?, ?>> list = getRows(input);
            input.delete();
            return list;
        } catch (IOException e) {
            log.error("Error parsing file at the url '{}'' to CSV. {}", url, e.getMessage());
        }

        return null;
    }

    public List<Map<?, ?>> getRows(File input) throws IOException {
        CsvSchema csv = CsvSchema.emptySchema().withHeader();
        CsvMapper csvMapper = new CsvMapper();
        MappingIterator<Map<?, ?>> mappingIterator = csvMapper.reader().forType(Map.class).with(csv)
                .readValues(input);
        List<Map<?, ?>> list = mappingIterator.readAll();
        return list;
    }

    public List<Map<?, ?>> getRows(MultipartFile file) throws IOException {
        File tempCsvFile = File.createTempFile("temp", ".csv");

        try (OutputStream os = new FileOutputStream(tempCsvFile)) {
            os.write(file.getBytes());
        }
        return getRows(tempCsvFile);
    }

    public <T> List<T> downloadAndParseCsvFile(String url, Class<T> clazz) {

        if (Strings.isNullOrEmpty(url)) {
            return Collections.emptyList();
        }

        try {
            File input = File.createTempFile(UUID.randomUUID().toString(), ".csv");
            FileUtils.copyURLToFile(new URL(url), input);
            List<T> list = getParsedObjects(input, clazz);
            input.delete();
            return list;
        } catch (IOException e) {
            log.error("Error parsing file at the url '{}'' to CSV. {}", url, e.getMessage());
        }

        return Collections.emptyList();
    }

    private <T> List<T> getParsedObjects(File file, Class<T> clazz) throws IOException {
        CsvMapper csvMapper = new CsvMapper();
        CsvSchema csv = CsvSchema.emptySchema().withHeader();
        MappingIterator<T> mappingIterator = csvMapper.reader()
                .forType(clazz).with(csv)
                .readValues(file);
        return mappingIterator.readAll();
    }

    public List<Map<?, ?>> parseCsv(String csv) {

        if (Strings.isNullOrEmpty(csv)) {
            return null;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(csv, new TypeReference<List<Map<?, ?>>>() {
            });
        } catch (Exception e) {
            log.error("Error parsing string to CSV. {}", e.getMessage());
        }

        return null;
    }

//    public String uploadSanitizedPmCsvToS3(String originalUrl, List<Map<?, ?>> sanitizedRows)
//            throws IOException {
//        return uploadSanitizedPmCsvToS3(originalUrl, extractHeaders(sanitizedRows), sanitizedRows);
//    }

//    public String uploadSanitizedPmCsvToS3(String originalUrl, List<String> headers, List<Map<?, ?>> sanitizedRows)
//            throws IOException {
//        URL aURL = new URL(originalUrl);
//        String path = aURL.getPath();
//
//        List<String> pathParts = Arrays.stream(path.split("/")).filter(t -> !Strings.isNullOrEmpty(t))
//                .collect(Collectors.toList());
//        int indexToRemove = pathParts.size() - 1;
//        String fileName = pathParts.get(indexToRemove);
//        pathParts.remove(indexToRemove);
//        String host = aURL.getHost();
//        String contentType = "text/csv";
//        String bucket = host.split("\\.")[0];
//
//        String[] filenameParts = fileName.split("\\.");
//        String fileKey = String.format("%s/%s-sanitized.%s", String.join("/", pathParts), filenameParts[0], filenameParts[1]);
//        Path tempFile = Files.createTempFile(null, null);
//
//        byte[] byteContent = getByteContent(headers, sanitizedRows, bucket, fileKey, tempFile);
//        MultipartFile multipartFile = new MockMultipartFile(fileName, fileName, contentType, byteContent);
//        Files.delete(tempFile);
//        return host + "/" + fileKey;
//    }

    public File writeToCsvAndDownload(List<Map<?, ?>> data) {
        // Create a new CSV file
        File csvOutputFile = null;
        try {
            csvOutputFile = File.createTempFile("output", ".csv");

            try (CSVWriter writer = new CSVWriter(new FileWriter(csvOutputFile))) {
                // Write CSV header
                if (!data.isEmpty()) {
                    Map<?, ?> firstRow = data.get(0);
                    String[] header = firstRow.keySet().toArray(new String[0]);
                    writer.writeNext(header);
                }

                // Write CSV data
                for (Map<?, ?> row : data) {
                    String[] rowData = row.values().toArray(new String[0]);
                    writer.writeNext(rowData);
                }
            }
        } catch (IOException e) {
            if (csvOutputFile != null && csvOutputFile.exists()) {
                csvOutputFile.delete(); // Delete the temp file if an exception occurs
            }
            e.printStackTrace();
        }

        return csvOutputFile;
    }

        private List<String> extractHeaders(List<Map<?, ?>> rows) {
        return rows.get(0).keySet().stream()
                .map(key -> (String) key)
                .collect(Collectors.toList());
    }

//    private byte[] getByteContent(List<String> headers, List<Map<?, ?>> sanitizedRows, String bucket, String fileKey, Path tempFile) throws IOException {
//        byte[] byteContent;
//        try(InputStream inputStream = documentManagerService.downloadFile(bucket, fileKey)) {
//            byte[] buffer = new byte[inputStream.available()];
//            inputStream.read(buffer);
//            Files.write(tempFile, buffer);
//            if(tempFile.toFile().length() > 0) {
//                byteContent = writeToCsv(headers, sanitizedRows, tempFile, true);
//            } else {
//                byteContent = writeToCsv(headers, sanitizedRows, tempFile, false);
//            }
//        } catch (Exception e) {
//            log.warn("The file wasn't found for the fileKey " + fileKey + ". Creating the document from scratch. ", e);
//            byteContent = writeToCsv(headers, sanitizedRows, tempFile, false);
//        }
//        return byteContent;
//    }
//
//    private byte[] writeToCsv(List<String> headers, List<Map<?, ?>> sanitizedRows, Path tempFile, boolean update) throws IOException {
//        CsvSchema.Builder schemaBuilder = getSchemaBuilder(headers);
//        CsvSchema schema;
//        if(update && isNotDotBitInspection(headers)) {
//            schema = schemaBuilder.build().withLineSeparator(System.lineSeparator()).withoutHeader();
//        } else {
//            schema = schemaBuilder.build().withLineSeparator(System.lineSeparator()).withHeader();
//        }
//        FileWriter writer = new FileWriter(tempFile.toFile(), update);
//        CsvMapper csvMapper = new CsvMapper();
//        csvMapper.writer(schema).writeValues(writer).writeAll(sanitizedRows);
//        writer.flush();
//        return Files.readAllBytes(tempFile);
//    }

    private CsvSchema.Builder getSchemaBuilder(List<String> headers) {
        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        if (!CollectionUtils.isEmpty(headers)) {
            for (String col : headers) {
                schemaBuilder.addColumn(col);
            }
        }
        return schemaBuilder;
    }

}
