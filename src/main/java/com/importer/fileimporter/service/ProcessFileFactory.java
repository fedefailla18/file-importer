package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.FileInformationResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@AllArgsConstructor
@Service
@Slf4j
public class ProcessFileFactory {

    private final ProcessFileV2 processFileV2;


    public FileInformationResponse processFile(MultipartFile file) throws IOException {
        return processFileV2.processFile(file, null, "Binance", "Binance");
    }

    public FileInformationResponse processFile(MultipartFile file, List<String> symbols) throws IOException {
        return processFileV2.processFile(file, symbols, "Binance", "Binance");
    }

    public FileInformationResponse processFile(MultipartFile file, List<String> symbols, String portfolio) throws IOException {
        return processFileV2.processFile(file, symbols, portfolio, "Binance");
    }

    public FileInformationResponse processFile(MultipartFile file, List<String> symbols, String portfolio, String fileType) throws IOException {
        return processFileV2.processFile(file, symbols, portfolio, fileType);
    }
}
