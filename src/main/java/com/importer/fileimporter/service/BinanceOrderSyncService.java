package com.importer.fileimporter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.importer.fileimporter.dto.integration.binance.BinanceOrderResponse;
import com.importer.fileimporter.entity.BinanceRawOrder;
import com.importer.fileimporter.entity.BinanceSyncProgress;
import com.importer.fileimporter.entity.BinanceSyncStatus;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.repository.BinanceRawOrderRepository;
import com.importer.fileimporter.repository.BinanceSyncProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BinanceOrderSyncService {

    private final BinanceApiService binanceApiService;
    private final BinanceSyncProgressRepository syncProgressRepository;
    private final BinanceRawOrderRepository rawOrderRepository;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void syncOrdersForSymbol(User user, String symbol, String apiKey, String secretKey) {
        log.info("Starting order sync for {} - user: {}", symbol, user.getUsername());
        
        BinanceSyncProgress progress = syncProgressRepository.findByUserAndSymbol(user, symbol)
                .orElse(BinanceSyncProgress.builder()
                        .user(user)
                        .symbol(symbol)
                        .status(BinanceSyncStatus.PENDING)
                        .build());

        progress.setStatus(BinanceSyncStatus.IN_PROGRESS);
        progress.setLastSyncTime(LocalDateTime.now());
        syncProgressRepository.save(progress);

        try {
            boolean hasMore = true;
            Long lastOrderId = progress.getLastSyncedOrderId();

            while (hasMore) {
                // If lastOrderId is null, we start from the beginning (startTime=0)
                // Otherwise we use orderId = lastOrderId + 1
                List<BinanceOrderResponse> orders;
                if (lastOrderId == null) {
                    orders = binanceApiService.getAllOrders(apiKey, secretKey, symbol, 0L, null, null);
                } else {
                    orders = binanceApiService.getAllOrders(apiKey, secretKey, symbol, null, null, lastOrderId + 1);
                }

                if (orders == null || orders.isEmpty()) {
                    hasMore = false;
                } else {
                    log.info("Persisting {} orders for {}", orders.size(), symbol);
                    for (BinanceOrderResponse order : orders) {
                        saveRawOrder(user, symbol, order);
                        lastOrderId = order.getOrderId();
                    }
                    
                    progress.setLastSyncedOrderId(lastOrderId);
                    syncProgressRepository.save(progress);

                    // Binance returns up to 1000 orders. If we got less, we are done.
                    if (orders.size() < 1000) {
                        hasMore = false;
                    }
                }
                
                // Rate limit protection: small delay between batches
                Thread.sleep(200); 
            }

            progress.setStatus(BinanceSyncStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Error syncing orders for {}: {}", symbol, e.getMessage());
            progress.setStatus(BinanceSyncStatus.FAILED);
        } finally {
            syncProgressRepository.save(progress);
        }
    }

    private void saveRawOrder(User user, String symbol, BinanceOrderResponse order) {
        // Check if already exists to avoid duplicates
        if (rawOrderRepository.findByUserAndSymbolAndOrderId(user, symbol, order.getOrderId()).isPresent()) {
            return;
        }

        try {
            BinanceRawOrder rawOrder = BinanceRawOrder.builder()
                    .user(user)
                    .symbol(symbol)
                    .orderId(order.getOrderId())
                    .clientOrderId(order.getClientOrderId())
                    .price(order.getPrice())
                    .origQty(order.getOrigQty())
                    .executedQty(order.getExecutedQty())
                    .status(order.getStatus())
                    .side(order.getSide())
                    .type(order.getType())
                    .orderTime(order.getTime())
                    .rawResponse(objectMapper.writeValueAsString(order))
                    .build();

            rawOrderRepository.save(rawOrder);
        } catch (Exception e) {
            log.error("Failed to save raw order {}: {}", order.getOrderId(), e.getMessage());
        }
    }
}
