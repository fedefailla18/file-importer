package com.importer.fileimporter.dto.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.importer.fileimporter.deserializer.LocalDateTimeEpochDeserializer;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class CryptoCompareResponse {

    @JsonProperty("Response")
    private String response;

    @JsonProperty("Message")
    private String message;

    @JsonProperty("HasWarning")
    private boolean hasWarning;

    @JsonProperty("Type")
    private int type;

    @JsonProperty("rateLimit")
    private RateLimit RateLimit;

    @JsonProperty("Data")
    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RateLimit {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data {

        @JsonProperty("Aggregated")
        private boolean aggregated;

        @JsonProperty("TimeFrom")
        private long timeFrom;

        @JsonProperty("TimeTo")
        private long timeTo;

        @JsonProperty("Data")
        private List<ChartData> chartDataList;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ChartData {

        @JsonDeserialize(using = LocalDateTimeEpochDeserializer.class)
        private LocalDateTime time;

        private BigDecimal close;

        private BigDecimal high;

        private BigDecimal low;

        private BigDecimal open;

        private BigDecimal volumefrom;

        private BigDecimal volumeto;

        private String conversionType;

        private String conversionSymbol;
    }
}
