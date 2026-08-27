package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.integration.mexc.*;
import com.importer.fileimporter.dto.integration.binance.BinanceServerTimeResponse;
import com.importer.fileimporter.dto.integration.binance.BinanceExchangeInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class MexcApiService {

    private static final String BASE_URL = "https://api.mexc.com";
    private static final String ACCOUNT_ENDPOINT = "/api/v3/account";
    private static final String EXCHANGE_INFO_ENDPOINT = "/api/v3/exchangeInfo";
    private static final String MY_TRADES_ENDPOINT = "/api/v3/myTrades";
    private static final String ALL_ORDERS_ENDPOINT = "/api/v3/allOrders";
    private static final String SERVER_TIME_ENDPOINT = "/api/v3/time";
    private static final String DEPOSIT_HISTORY_ENDPOINT = "/api/v3/capital/deposit/hisrec";
    private static final String WITHDRAW_HISTORY_ENDPOINT = "/api/v3/capital/withdraw/history";
    
    private static final int DEFAULT_LIMIT = 1000;
    private static final int WEIGHT_LIMIT_THRESHOLD = 1000;

    private final WebClient webClient;
    private final ApiLoggingService apiLoggingService;

    public MexcApiService(WebClient.Builder webClientBuilder, ApiLoggingService apiLoggingService) {
        this.webClient = webClientBuilder
                .baseUrl(BASE_URL)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();
        this.apiLoggingService = apiLoggingService;
    }

    private <T> T executeRequest(String endpoint, String method, String queryString, String apiKey, ParameterizedTypeReference<T> typeReference) {
        return executeRequest(endpoint, method, queryString, apiKey, typeReference, 1);
    }

    private <T> T executeRequest(String endpoint, String method, String queryString, String apiKey, ParameterizedTypeReference<T> typeReference, int retryCount) {
        String uri = endpoint + (queryString != null && !queryString.isEmpty() ? "?" + queryString : "");
        
        try {
            ResponseEntity<T> response = webClient.get()
                    .uri(uri)
                    .header("X-MEXC-APIKEY", apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.equals(HttpStatus.TOO_MANY_REQUESTS), resp -> {
                        log.warn("Rate limit exceeded (429) for MexC API");
                        return Mono.error(new RuntimeException("RATE_LIMIT_EXCEEDED"));
                    })
                    .toEntity(typeReference)
                    .block();

            Integer weight = extractWeight(response);
            checkWeightAndSleep(weight);

            T body = Objects.requireNonNull(response).getBody();
            apiLoggingService.log("MEXC", endpoint, method, queryString, 
                    response.getStatusCodeValue(), Objects.toString(body), weight);

            return body;
        } catch (RuntimeException e) {
            if ("RATE_LIMIT_EXCEEDED".equals(e.getMessage()) && retryCount < 3) {
                long sleepTime = 10000L * retryCount;
                log.info("Sleeping for {}ms due to rate limit", sleepTime);
                try { Thread.sleep(sleepTime); } catch (InterruptedException ignored) {}
                return executeRequest(endpoint, method, queryString, apiKey, typeReference, retryCount + 1);
            }
            
            apiLoggingService.log("MEXC", endpoint, method, queryString, 
                    500, e.getMessage(), null);
            throw e;
        }
    }

    private Integer extractWeight(ResponseEntity<?> response) {
        if (response == null) return null;
        // MexC uses similar headers, but might vary. Using a generic one if found.
        String weightHeader = response.getHeaders().getFirst("X-MEXC-USED-WEIGHT-1M");
        if (weightHeader != null) {
            try { return Integer.parseInt(weightHeader); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private void checkWeightAndSleep(Integer usedWeight) {
        if (usedWeight != null && usedWeight > WEIGHT_LIMIT_THRESHOLD) {
            log.warn("MexC API used weight is high: {}. Slowing down...", usedWeight);
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
        } else {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
    }

    public long getServerTime() {
        BinanceServerTimeResponse response = webClient.get()
                .uri(SERVER_TIME_ENDPOINT)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BinanceServerTimeResponse.class)
                .block();
        return response != null ? response.getServerTime() : System.currentTimeMillis();
    }

    public MexcAccountResponse getAccountInfo(String apiKey, String secretKey) {
        long timestamp = System.currentTimeMillis();
        String queryString = "timestamp=" + timestamp + "&recvWindow=60000";
        String signature = hmacSha256(queryString, secretKey);
        String qsWithSig = queryString + "&signature=" + signature;

        return executeRequest(ACCOUNT_ENDPOINT, "GET", qsWithSig, apiKey, new ParameterizedTypeReference<MexcAccountResponse>() {});
    }

    public BinanceExchangeInfoResponse getExchangeInfo() {
        return webClient.get()
                .uri(EXCHANGE_INFO_ENDPOINT)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BinanceExchangeInfoResponse.class)
                .block();
    }

    public List<MexcTradeResponse> getMyTrades(String apiKey, String secretKey, String symbol, Long startTime, Long endTime, Long fromId) {
        long timestamp = System.currentTimeMillis();
        StringBuilder qs = new StringBuilder();
        qs.append("symbol=").append(symbol);
        
        if (fromId != null) {
            qs.append("&fromId=").append(fromId);
        } else {
            if (startTime != null) qs.append("&startTime=").append(startTime);
            if (endTime != null) qs.append("&endTime=").append(endTime);
        }
        
        qs.append("&limit=").append(DEFAULT_LIMIT);
        qs.append("&timestamp=").append(timestamp).append("&recvWindow=60000");

        String signature = hmacSha256(qs.toString(), secretKey);
        String qsWithSig = qs.toString() + "&signature=" + signature;

        return executeRequest(MY_TRADES_ENDPOINT, "GET", qsWithSig, apiKey, new ParameterizedTypeReference<List<MexcTradeResponse>>() {});
    }

    public List<MexcOrderResponse> getAllOrders(String apiKey, String secretKey, String symbol, Long startTime, Long endTime, Long orderId) {
        long timestamp = System.currentTimeMillis();
        StringBuilder qs = new StringBuilder();
        qs.append("symbol=").append(symbol);

        if (startTime != null) qs.append("&startTime=").append(startTime);
        if (endTime != null) qs.append("&endTime=").append(endTime);
        if (orderId != null) qs.append("&orderId=").append(orderId);

        qs.append("&limit=").append(DEFAULT_LIMIT);
        qs.append("&timestamp=").append(timestamp).append("&recvWindow=60000");

        String signature = hmacSha256(qs.toString(), secretKey);
        String qsWithSig = qs.toString() + "&signature=" + signature;

        return executeRequest(ALL_ORDERS_ENDPOINT, "GET", qsWithSig, apiKey, new ParameterizedTypeReference<List<MexcOrderResponse>>() {});
    }

    public List<MexcDepositResponse> getDepositHistory(String apiKey, String secretKey, Long startTime, Long endTime) {
        long timestamp = System.currentTimeMillis();
        StringBuilder qs = new StringBuilder("timestamp=" + timestamp + "&recvWindow=60000");
        if (startTime != null) qs.append("&startTime=").append(startTime);
        if (endTime != null) qs.append("&endTime=").append(endTime);
        
        String signature = hmacSha256(qs.toString(), secretKey);
        String qsWithSig = qs.toString() + "&signature=" + signature;

        return executeRequest(DEPOSIT_HISTORY_ENDPOINT, "GET", qsWithSig, apiKey, new ParameterizedTypeReference<List<MexcDepositResponse>>() {});
    }

    public List<MexcWithdrawResponse> getWithdrawHistory(String apiKey, String secretKey, Long startTime, Long endTime) {
        long timestamp = System.currentTimeMillis();
        StringBuilder qs = new StringBuilder("timestamp=" + timestamp + "&recvWindow=60000");
        if (startTime != null) qs.append("&startTime=").append(startTime);
        if (endTime != null) qs.append("&endTime=").append(endTime);
        
        String signature = hmacSha256(qs.toString(), secretKey);
        String qsWithSig = qs.toString() + "&signature=" + signature;

        return executeRequest(WITHDRAW_HISTORY_ENDPOINT, "GET", qsWithSig, apiKey, new ParameterizedTypeReference<List<MexcWithdrawResponse>>() {});
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
