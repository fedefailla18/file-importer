package com.importer.fileimporter.coverter;

import com.importer.fileimporter.dto.HoldingDto;
import com.importer.fileimporter.entity.Holding;
import com.importer.fileimporter.payload.request.AddHoldingRequest;

import java.util.List;
import java.util.stream.Collectors;

public class HoldingConverter implements GenericConverter<HoldingDto, Holding> {

    @Override
    public HoldingDto createFrom(Holding source) {
        return HoldingDto.builder()
                .symbol(source.getSymbol())
                .amount(source.getAmount())
                .portfolioName(source.getPortfolio().getName())
                .percentage(source.getPercent())
                .amountInBtc(source.getAmountInBtc())
                .amountInUsdt(source.getAmountInUsdt())
                .priceInBtc(source.getPriceInBtc())
                .priceInUsdt(source.getPriceInUsdt())
                .totalAmountBought(source.getTotalAmountBought())
                .totalAmountSold(source.getTotalAmountSold())
                .stableTotalCost(source.getStableTotalCost())
                .currentPositionInUsdt(source.getCurrentPositionInUsdt())
                .totalRealizedProfitUsdt(source.getTotalRealizedProfitUsdt())
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

    public Holding createTo(AddHoldingRequest holdingRequest) {
        return Holding.builder()
                .symbol(holdingRequest.getSymbol())
                .amount(holdingRequest.getAmount())
                .priceInBtc(holdingRequest.getCostInBtc())
                .priceInUsdt(holdingRequest.getCostInUsdt())
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

    private List<Holding> createFromRequest(List<AddHoldingRequest> dtos) {
        return dtos.stream()
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

        public static List<Holding> createFromRequest(List<AddHoldingRequest> dtos) {
            return mapper.createFromRequest(dtos);
        }
    }
}
