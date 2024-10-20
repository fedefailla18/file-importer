package com.importer.fileimporter.specification;

import com.importer.fileimporter.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
public class TransactionSpecifications {

    private static final String PAID_AMOUNT = "paidAmount";

    public static Specification<Transaction> getSpecWithFilters(String symbol, String portfolioName, String side, String paidWith, String paidAmountOperator, BigDecimal paidAmount, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return Specification.where(TransactionSpecifications.hasSymbol(symbol))
                .and(TransactionSpecifications.hasPortfolioName(portfolioName))
                .and(TransactionSpecifications.hasSide(side))
                .and(TransactionSpecifications.hasPaidWith(paidWith))
                .and(TransactionSpecifications.inDateRange(null, null))
                .and(TransactionSpecifications.paidAmountCondition(paidAmountOperator, paidAmount));
    }

    public static Specification<Transaction> hasSymbol(String symbol) {
        return (root, query, criteriaBuilder) ->
                Optional.ofNullable(symbol)
                        .map(s -> {
                            log.info("Applying symbol filter: {}", symbol);
                            return criteriaBuilder.equal(root.get("symbol"), symbol);
                        })
                        .orElse(criteriaBuilder.conjunction());
    }

    public static Specification<Transaction> hasPortfolioName(String portfolioName) {
        return (root, query, criteriaBuilder) ->
                Optional.ofNullable(portfolioName)
                        .map(name -> {
                            log.info("Applying symbol filter: {}", name);
                            return criteriaBuilder.equal(root.get("portfolio").get("name"), name);
                        })
                        .orElse(criteriaBuilder.conjunction());
    }

    public static Specification<Transaction> hasSide(String side) {
        return (root, query, criteriaBuilder) ->
                Optional.ofNullable(side)
                        .map(s -> {
                            log.info("Applying symbol filter: {}", s);
                            return criteriaBuilder.equal(root.get("side"), s);
                        })
                        .orElse(criteriaBuilder.conjunction());
    }

    public static Specification<Transaction> hasPaidWith(String paidWith) {
        return (root, query, criteriaBuilder) ->
                Optional.ofNullable(paidWith)
                        .map(p -> criteriaBuilder.equal(root.get("paidWith"), p))
                        .orElse(criteriaBuilder.conjunction());
    }

    public static Specification<Transaction> inDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            Predicate startPredicate = Optional.ofNullable(startDate)
                    .map(start -> criteriaBuilder.greaterThanOrEqualTo(root.get("dateUtc"), start))
                    .orElse(criteriaBuilder.conjunction());
            Predicate endPredicate = Optional.ofNullable(endDate)
                    .map(end -> criteriaBuilder.lessThanOrEqualTo(root.get("dateUtc"), end))
                    .orElse(criteriaBuilder.conjunction());
            return criteriaBuilder.and(startPredicate, endPredicate);
        };
    }

    public static Specification<Transaction> paidAmountCondition(String operator, BigDecimal paidAmount) {
        return (root, query, criteriaBuilder) -> {
            if (operator == null || paidAmount == null) {
                return criteriaBuilder.conjunction();
            }
            Path<BigDecimal> path = root.get(PAID_AMOUNT);
            switch (operator) {
                case ">":
                    return criteriaBuilder.greaterThan(path, paidAmount);
                case "=":
                    return criteriaBuilder.equal(path, paidAmount);
                case "<":
                    return criteriaBuilder.lessThan(path, paidAmount);
                default:
                    return criteriaBuilder.conjunction();
            }
        };
    }
}
