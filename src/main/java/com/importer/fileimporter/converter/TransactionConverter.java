package com.importer.fileimporter.converter;

import com.importer.fileimporter.dto.TransactionDto;
import com.importer.fileimporter.entity.Transaction;

import java.util.List;
import java.util.stream.Collectors;

public class TransactionConverter implements GenericConverter<TransactionDto, Transaction> {

    @Override
    public TransactionDto createFrom(Transaction source) {
        return TransactionDto.builder()
                .id(source.getId())
                .dateUtc(source.getDateUtc())
                .side(source.getSide())
                .pair(source.getPair())
                .price(source.getPrice())
                .executed(source.getExecuted())
                .symbol(source.getSymbol())
                .payedWith(source.getPaidWith())
                .payedAmount(source.getPaidAmount())
                .fee(source.getFee())
                .feeAmount(source.getFeeAmount())
                .feeSymbol(source.getFeeSymbol())
                .created(source.getCreated())
                .createdBy(source.getCreatedBy())
                .modified(source.getModified())
                .modifiedBy(source.getModifiedBy())
                .processed(source.isProcessed())
                .lastProcessedAt(source.getLastProcessedAt())
                .build();
    }

    @Override
    public Transaction createTo(TransactionDto dto) {
        return Transaction.builder()
                .id(dto.getId())
                .dateUtc(dto.getDateUtc())
                .side(dto.getSide())
                .pair(dto.getPair())
                .price(dto.getPrice())
                .executed(dto.getExecuted())
                .symbol(dto.getSymbol())
                .paidWith(dto.getPayedWith())
                .paidAmount(dto.getPayedAmount())
                .fee(dto.getFee())
                .feeAmount(dto.getFeeAmount())
                .feeSymbol(dto.getFeeSymbol())
                .created(dto.getCreated())
                .createdBy(dto.getCreatedBy())
                .modified(dto.getModified())
                .modifiedBy(dto.getModifiedBy())
                .processed(dto.isProcessed())
                .lastProcessedAt(dto.getLastProcessedAt())
                .build();
    }

    @Override
    public List<TransactionDto> createFromEntities(List<Transaction> transactions) {
        return transactions.stream()
                .map(this::createFrom)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> createToEntities(List<TransactionDto> transactionDtos) {
        return transactionDtos.stream()
                .map(this::createTo)
                .collect(Collectors.toList());
    }

    public static class Mapper {
        public static final TransactionConverter mapper = new TransactionConverter();

        public static TransactionDto createFrom(Transaction transaction) {
            return mapper.createFrom(transaction);
        }

        public static Transaction createTo(TransactionDto dto) {
            return mapper.createTo(dto);
        }

        public static List<TransactionDto> createFromEntities(List<Transaction> transactions) {
            return mapper.createFromEntities(transactions);
        }

        public static List<Transaction> createToEntities(List<TransactionDto> dtos) {
            return mapper.createToEntities(dtos);
        }
    }
}
