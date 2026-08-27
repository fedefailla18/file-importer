package com.importer.fileimporter.payload.request;

import com.importer.fileimporter.entity.ExchangeName;
import lombok.Data;

@Data
public class ExchangeConfigRequest {
    private ExchangeName exchangeName;
    private String apiKey;
    private String apiSecret;
}
