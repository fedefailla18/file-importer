package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.integration.binance.BinanceAccountResponse;
import com.importer.fileimporter.dto.integration.binance.BinanceExchangeInfoResponse;
import com.importer.fileimporter.dto.integration.binance.BinanceServerTimeResponse;
import com.importer.fileimporter.dto.integration.binance.BinanceTradeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Slf4j
public class BinanceApiService {

    private static final String BASE_URL = "https://api.binance.com";
    private static final String ACCOUNT_ENDPOINT = "/api/v3/account";
    private static final String EXCHANGE_INFO_ENDPOINT = "/api/v3/exchangeInfo";
    private static final String MY_TRADES_ENDPOINT = "/api/v3/myTrades";
    private static final int DEFAULT_TRADES_LIMIT = 1000;

    private final WebClient webClient;

    public BinanceApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(BASE_URL)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();
    }

    public long getServerTime() {
        BinanceServerTimeResponse response = webClient.get()
                .uri("/api/v3/time")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BinanceServerTimeResponse.class)
                .block();
        return response != null ? response.getServerTime() : System.currentTimeMillis();
    }

    public BinanceAccountResponse getAccountInfo(String apiKey, String secretKey) {
        long timestamp = System.currentTimeMillis();
        String queryString = "timestamp=" + timestamp + "&recvWindow=60000";
        String signature = hmacSha256(queryString, secretKey);

        return webClient.get()
                .uri(ACCOUNT_ENDPOINT + "?" + queryString + "&signature=" + signature)
                .header("X-MBX-APIKEY", apiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Binance account error - status: {}, body: {}", response.statusCode(), body))
                                .flatMap(body -> Mono.error(new RuntimeException("Binance API error: " + body))))
                .bodyToMono(BinanceAccountResponse.class)
                .block();
    }

    public BinanceExchangeInfoResponse getExchangeInfo() {
        return webClient.get()
                .uri(EXCHANGE_INFO_ENDPOINT)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BinanceExchangeInfoResponse.class)
                .block();
    }

    public List<BinanceTradeResponse> getMyTrades(String apiKey, String secretKey, String symbol, Long startTime) {
        return getMyTrades(apiKey, secretKey, symbol, startTime, DEFAULT_TRADES_LIMIT);
    }

    public List<BinanceTradeResponse> getMyTrades(
            String apiKey,
            String secretKey,
            String symbol,
            Long startTime,
            Integer limit
    ) {
        long timestamp = System.currentTimeMillis();
        StringBuilder queryStringBuilder = new StringBuilder();
        queryStringBuilder.append("symbol=").append(symbol);
        if (startTime != null) {
            queryStringBuilder.append("&startTime=").append(startTime);
        }
        if (limit != null) {
            queryStringBuilder.append("&limit=").append(limit);
        }
        queryStringBuilder.append("&timestamp=").append(timestamp).append("&recvWindow=60000");

        String queryString = queryStringBuilder.toString();
        String signature = hmacSha256(queryString, secretKey);

        return webClient.get()
                .uri(MY_TRADES_ENDPOINT + "?" + queryString + "&signature=" + signature)
                .header("X-MBX-APIKEY", apiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Binance trades error - status: {}, body: {}", response.statusCode(), body))
                                .flatMap(body -> Mono.error(new RuntimeException("Binance API error: " + body))))
                .bodyToMono(new ParameterizedTypeReference<List<BinanceTradeResponse>>() {})
                .block();
    }

    public List<BinanceTradeResponse> getAllMyTrades(String apiKey, String secretKey, String symbol) {
        List<BinanceTradeResponse> allTrades = new java.util.ArrayList<>();
        Long startTime = 0L;

        while (true) {
            List<BinanceTradeResponse> page = getMyTrades(apiKey, secretKey, symbol, startTime, DEFAULT_TRADES_LIMIT);
            if (page == null || page.isEmpty()) {
                break;
            }

            page.sort(java.util.Comparator
                    .comparing(BinanceTradeResponse::getTime, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                    .thenComparing(BinanceTradeResponse::getId, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));

            allTrades.addAll(page);

            if (page.size() < DEFAULT_TRADES_LIMIT) {
                break;
            }

            Long nextStartTime = page.stream()
                    .map(BinanceTradeResponse::getTime)
                    .filter(java.util.Objects::nonNull)
                    .max(Long::compareTo)
                    .orElse(null);

            if (nextStartTime == null || nextStartTime < startTime) {
                break;
            }

            startTime = nextStartTime + 1L;
        }

        return allTrades;
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating HMAC SHA256", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
