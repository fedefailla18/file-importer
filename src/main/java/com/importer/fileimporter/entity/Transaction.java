package com.importer.fileimporter.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {

    @EmbeddedId
    private TransactionId transactionId;

    @Column(name = "symbol", length = 8)
    private String symbol;

    @Column(name = "payed_with", length = 12)
    private String payedWith;

//    @Column(name = "payed_amount", precision = 13, scale = 13)
    private BigDecimal payedAmount;

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

    @Override
    public String toString() {
        return "Transaction{" +
                "date=" + transactionId.getDateUtc() +
                ", symbol='" + symbol + '\'' +
                '}';
    }
}
