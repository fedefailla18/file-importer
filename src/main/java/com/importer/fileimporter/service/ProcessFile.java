package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.dto.TransactionData;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.importer.fileimporter.utils.OperationUtils.STABLE;

@Service
@AllArgsConstructor
public abstract class ProcessFile {

    private final FileImporterService fileImporterService;
    protected final TransactionAdapterFactory transactionAdapterFactory;

    protected List<Map<?, ?>> getRows(MultipartFile file) throws IOException {
        return fileImporterService.getRows(file);
    }

    protected TransactionData getAdapter(Map<?, ?> row, String fileType) {
        final String type = fileType == null ? "Binance" : fileType;
        return transactionAdapterFactory.createAdapter(row, type);
    }

}
