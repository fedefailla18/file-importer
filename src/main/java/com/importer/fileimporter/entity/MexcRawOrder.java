package com.importer.fileimporter.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mexc_raw_order")
public class MexcRawOrder implements RawOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Type(type = "pg-uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "client_order_id")
    private String clientOrderId;

    @Column(precision = 20, scale = 10)
    private BigDecimal price;

    @Column(name = "orig_qty", precision = 20, scale = 10)
    private BigDecimal origQty;

    @Column(name = "executed_qty", precision = 20, scale = 10)
    private BigDecimal executedQty;

    private String status;

    private String side;

    private String type;

    @Column(name = "order_time")
    private Long orderTime;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;
}
