package com.importer.fileimporter.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_utc", nullable = false)
    private LocalDateTime dateUtc;

    @Column(name = "side", nullable = false, length = 12)
    private String side;

    @Column(name = "pair", nullable = false, length = 12)
    private String pair;

    @Column(name = "price", nullable = false, precision = 13, scale = 13)
    private BigDecimal price;

    @Column(name = "executed", nullable = false, precision = 20, scale = 20)
    private BigDecimal executed;

    @Column(name = "symbol", length = 8)
    private String symbol;

    @Column(name = "paid_with", length = 12)
    private String paidWith;

    @Column(name = "paid_amount", precision = 13, scale = 13)
    private BigDecimal paidAmount;

    @Column(name = "fee", length = 12)
    private String fee;

//    @Column(name = "fee_amount", precision = 13, scale = 13)
    private BigDecimal feeAmount;

    @Column(name = "fee_symbol", length = 8)
    private String feeSymbol;

    @Column(name = "created")
    private LocalDateTime created;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "modified")
    private LocalDateTime modified;

    @Column(name = "modified_by")
    private String modifiedBy;

    @ManyToOne
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    // New fields for transaction processing
    //@Column(name = "processed", nullable = false)
    private boolean processed = false;

    //@Column(name = "last_processed_at")
    private LocalDateTime lastProcessedAt;

    @Override
    public String toString() {
        return "Transaction{" +
                "date=" + dateUtc +
                ", symbol='" + symbol + '\'' +
                ", side='" + side + '\'' +
                ", pair='" + pair + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return dateUtc.equals(that.dateUtc) &&
                side.equals(that.side) &&
                pair.equals(that.pair) &&
                price.equals(that.price) &&
                executed.equals(that.executed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dateUtc, side, pair, price, executed);
    }
}
