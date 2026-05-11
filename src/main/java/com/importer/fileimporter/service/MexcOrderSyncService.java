package com.importer.fileimporter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.importer.fileimporter.dto.integration.mexc.MexcOrderResponse;
import com.importer.fileimporter.entity.MexcRawOrder;
import com.importer.fileimporter.entity.MexcSyncProgress;
import com.importer.fileimporter.entity.BinanceSyncStatus;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.repository.MexcRawOrderRepository;
import com.importer.fileimporter.repository.MexcSyncProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MexcOrderSyncService {

    private final MexcApiService mexcApiService;
    private final MexcSyncProgressRepository syncProgressRepository;
    private final MexcRawOrderRepository rawOrderRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void syncOrdersForSymbol(User user, String symbol, String apiKey, String secretKey) {
        log.info("Starting MexC order sync for {} - user: {}", symbol, user.getUsername());
        
        MexcSyncProgress progress = syncProgressRepository.findByUserAndSymbol(user, symbol)
                .orElse(MexcSyncProgress.builder()
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
                List<MexcOrderResponse> orders;
                if (lastOrderId == null) {
                    orders = mexcApiService.getAllOrders(apiKey, secretKey, symbol, null, null, null);
                } else {
                    orders = mexcApiService.getAllOrders(apiKey, secretKey, symbol, null, null, lastOrderId + 1);
                }

                if (orders == null || orders.isEmpty()) {
                    hasMore = false;
                } else {
                    log.info("Persisting {} MexC orders for {}", orders.size(), symbol);
                    for (MexcOrderResponse order : orders) {
                        saveRawOrder(user, symbol, order);
                        lastOrderId = order.getOrderId();
                    }
                    
                    progress.setLastSyncedOrderId(lastOrderId);
                    syncProgressRepository.save(progress);

                    if (orders.size() < 1000) {
                        hasMore = false;
                    }
                }
                Thread.sleep(200); 
            }

            progress.setStatus(BinanceSyncStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Error syncing MexC orders for {}: {}", symbol, e.getMessage());
            progress.setStatus(BinanceSyncStatus.FAILED);
        } finally {
            syncProgressRepository.save(progress);
        }
    }

    private void saveRawOrder(User user, String symbol, MexcOrderResponse order) {
        if (rawOrderRepository.findByUserAndSymbolAndOrderId(user, symbol, order.getOrderId()).isPresent()) {
            return;
        }

        try {
            MexcRawOrder rawOrder = MexcRawOrder.builder()
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
            log.error("Failed to save MexC raw order {}: {}", order.getOrderId(), e.getMessage());
        }
    }
}
