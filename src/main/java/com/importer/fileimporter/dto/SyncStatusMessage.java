package com.importer.fileimporter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyncStatusMessage {
    private String portfolioName;
    /** "COMPLETED" or "FAILED" */
    private String status;
    private String message;
}
