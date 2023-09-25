package com.importer.fileimporter.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
public class FileInformationResponse {

    private int amount;

//    private Map<String, AtomicInteger> each;

    private Collection<CoinInformationResponse> coinInformationResponse;

//    private List<Map<?, ?>> rows;

}
