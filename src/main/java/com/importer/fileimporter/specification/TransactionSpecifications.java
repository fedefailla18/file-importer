package com.importer.fileimporter.specification;

import com.importer.fileimporter.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class TransactionSpecifications {

    private static final String PAID_AMOUNT = "paidAmount";

    public static Specification<Transaction> getSpecWithFilters(String symbol, String portfolioName, String side, String paidWith, String paidAmountOperator, BigDecimal paidAmount, LocalDate startDate, LocalDate endDate, UUID userId) {
        return Specification.where(hasSymbol(symbol))
                .and(hasPortfolioName(portfolioName))
                .and(hasSide(side))
                .and(hasPaidWith(paidWith))
                .and(inDateRange(startDate, endDate))
                .and(paidAmountCondition(paidAmountOperator, paidAmount))
                .and(hasUserId(userId));
    }

    public static Specification<Transaction> hasUserId(UUID userId) {
        return (root, query, criteriaBuilder) ->
                Optional.ofNullable(userId)
                        .map(id ->
                            criteriaBuilder.equal(root.get("portfolio").get("user").get("id"), id)
                        )
                        .orElse(criteriaBuilder.conjunction());
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
                            return criteriaBuilder.equal(
                                    criteriaBuilder.lower(root.get("portfolio").get("name")), name.toLowerCase(Locale.ROOT));
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

    public static Specification<Transaction> inDateRange(LocalDate startDate, LocalDate endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return criteriaBuilder.conjunction();
            }

            Expression<LocalDateTime> dateUtcExpression = root.get("dateUtc");

            if (startDate != null && endDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
                return criteriaBuilder.between(dateUtcExpression, startDateTime, endDateTime);
            } else if (startDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                return criteriaBuilder.greaterThanOrEqualTo(dateUtcExpression, startDateTime);
            } else {
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
                return criteriaBuilder.lessThan(dateUtcExpression, endDateTime);
            }
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
