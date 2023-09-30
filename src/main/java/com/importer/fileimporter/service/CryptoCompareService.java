package com.importer.fileimporter.service;

import com.importer.fileimporter.config.integration.CryptoCompareConfig;
import com.importer.fileimporter.dto.integration.CryptoCompareResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CryptoCompareService {

    private static final String BASE_URL = "https://min-api.cryptocompare.com/data/v2/histohour";

    private static final String FROM_SYMBOL = "fsym";
    private static final String TO_SYMBOL = "tsym";
    private static final String EXCHANGE = "e";
    private static final String TO_TIMESTAMP = "toTs";
    // fsym=BTC&tsym=USDT&e=Binance&toTs=1632625200

    private final WebClient webClient;
    private final CryptoCompareConfig config;

    public CryptoCompareService(WebClient.Builder webClientBuilder, CryptoCompareConfig config) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
        this.config = config;
    }

    public CryptoCompareResponse getHistoricalData(String fromSymbol, String toSymbol, long toTs) {

        Mono<CryptoCompareResponse> response1 = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam(FROM_SYMBOL, fromSymbol)
                        .queryParam(TO_SYMBOL, toSymbol)
                        .queryParam(EXCHANGE, "Binance")
                        .queryParam("extraParams", "file-importer")
                        .queryParam(TO_TIMESTAMP, toTs)
                        .queryParam("limit", "10")
                        .queryParam("explainPath", "true")
                        .queryParam("api_key", config.getApiKey()) // Use the API key from config
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CryptoCompareResponse>() {})
                .onErrorResume(e -> Mono.empty()).log();
        return response1.block();
    }

}
