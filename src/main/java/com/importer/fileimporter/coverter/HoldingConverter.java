package com.importer.fileimporter.coverter;

import com.importer.fileimporter.dto.HoldingDto;
import com.importer.fileimporter.entity.Holding;

import java.util.List;
import java.util.stream.Collectors;

public class HoldingConverter implements GenericConverter<HoldingDto, Holding> {

    @Override
    public HoldingDto createFrom(Holding holding) {
        return HoldingDto.builder()
                .symbol(holding.getSymbol())
                .amount(holding.getAmount())
                .portfolioName(holding.getPortfolio().getName())
                .percentage(holding.getPercent())
                .amountInBtc(holding.getAmountInBtc())
                .amountInUsdt(holding.getAmountInUsdt())
                .priceInBtc(holding.getPriceInBtc())
                .priceInUsdt(holding.getPriceInUsdt())
                .build();
    }

    @Override
    public Holding createTo(HoldingDto holdingDto) {
        return Holding.builder()
                .symbol(holdingDto.getSymbol())
                .amount(holdingDto.getAmount())
                .percent(holdingDto.getPercentage())
                .amountInBtc(holdingDto.getAmountInBtc())
                .amountInUsdt(holdingDto.getAmountInUsdt())
                .priceInBtc(holdingDto.getPriceInBtc())
                .priceInUsdt(holdingDto.getPriceInUsdt())
                .build();
    }

    @Override
    public List<HoldingDto> createFromEntities(List<Holding> holdings) {
        return holdings.stream()
                .map(this::createFrom)
                .collect(Collectors.toList());
    }

    @Override
    public List<Holding> createToEntities(List<HoldingDto> holdingDtos) {
        return holdingDtos.stream()
                .map(this::createTo)
                .collect(Collectors.toList());
    }

    public static class Mapper {
        public static final HoldingConverter mapper = new HoldingConverter();

        public static HoldingDto createFrom(Holding holding) {
            return mapper.createFrom(holding);
        }

        public static Holding createTo(HoldingDto dto) {
            return mapper.createTo(dto);
        }

        public static List<HoldingDto> createFromEntities(List<Holding> holdings) {
            return mapper.createFromEntities(holdings);
        }

        public static List<Holding> createToEntities(List<HoldingDto> dtos) {
            return mapper.createToEntities(dtos);
        }
    }
}
