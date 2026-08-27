package com.importer.fileimporter.payload.response;

import com.importer.fileimporter.entity.ExchangeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExchangeConfigResponse {
    private ExchangeName exchangeName;
    private String apiKey;
    private Long lastSyncTimestamp;
}
