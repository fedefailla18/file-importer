package com.importer.fileimporter.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Collection;

@Data
@Builder
public class FileInformationResponse {

    private int amount;

    private BigDecimal totalSpent;

//    private Map<String, AtomicInteger> each;

    private Collection<CoinInformationResponse> coinInformationResponse;

//    private List<Map<?, ?>> rows;

}
