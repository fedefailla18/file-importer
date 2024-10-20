package com.importer.fileimporter.payload.request;

import lombok.Data;

import java.util.List;

@Data
public class AddHoldingsRequest {
    private List<AddHoldingRequest> holdings;
}
