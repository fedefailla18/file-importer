package com.importer.fileimporter.entity;

import java.math.BigDecimal;
import java.util.UUID;

public interface RawOrder {
    UUID getId();
    User getUser();
    String getSymbol();
    Long getOrderId();
    String getClientOrderId();
    BigDecimal getPrice();
    BigDecimal getOrigQty();
    BigDecimal getExecutedQty();
    String getStatus();
    String getSide();
    String getType();
    Long getOrderTime();
    String getRawResponse();
}
