package com.importer.fileimporter.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class TransactionId implements Serializable {

    @Column(name = "date_utc", nullable = false)
    private LocalDateTime dateUtc;

    @Column(name = "side", nullable = false, length = 12)
    private String side;

    @Column(name = "pair", nullable = false, length = 12)
    private String pair;

//    @Column(name = "price", nullable = false, precision = 13, scale = 13)
    private BigDecimal price;

//    @Column(name = "executed", nullable = false, precision = 20, scale = 20)
    private BigDecimal executed;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionId that = (TransactionId) o;
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
