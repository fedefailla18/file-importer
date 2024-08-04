package com.importer.fileimporter.facade;

import com.importer.fileimporter.coverter.HoldingConverter;
import com.importer.fileimporter.dto.HoldingDto;
import com.importer.fileimporter.dto.PortfolioDistribution;
import com.importer.fileimporter.entity.Holding;
import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Symbol;
import com.importer.fileimporter.payload.request.AddHoldingRequest;
import com.importer.fileimporter.service.GetSymbolHistoricPriceHelper;
import com.importer.fileimporter.service.HoldingService;
import com.importer.fileimporter.service.PortfolioService;
import com.importer.fileimporter.service.SymbolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.importer.fileimporter.utils.OperationUtils.BTC;
import static com.importer.fileimporter.utils.OperationUtils.USDT;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioDistributionFacade {

    private final SymbolService symbolService;
    private final PortfolioService portfolioService;
    private final HoldingService holdingService;
    private final GetSymbolHistoricPriceHelper getSymbolHistoricPriceHelper;

    public HoldingDto addPortfolioHolding(AddHoldingRequest request) {
        Symbol savedSymbol = symbolService.findOrSaveSymbol(request.getSymbol(), request.getName());

        Portfolio portfolio = portfolioService.findOrSave(request.getPortfolio());

        return Optional.ofNullable(
                holdingService.saveSymbolHolding(savedSymbol, portfolio, request.getAmount()))
                .map(HoldingConverter.Mapper::createFrom)
                .orElse(null);
    }

    public HoldingDto getHolding(String symbol) {
        Symbol foundSymbol = symbolService.findSymbol(symbol);
        return Optional.ofNullable(
                holdingService.getHolding(foundSymbol.getSymbol()))
                .map(HoldingConverter.Mapper::createFrom)
                .orElse(null);
    }

    public PortfolioDistribution getPortfolioByName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing param.");
        }
        if (Objects.equals(name.toLowerCase(Locale.ROOT), "all")) {
            List<Portfolio> portfolios = portfolioService.getAll();
            return getPortfolios(portfolios);
        }
        Portfolio portfolio = portfolioService.getByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Porfolio not found."));
        List<Holding> holdings = portfolio.getHoldings().stream()
                .filter(e -> e.getAmount().compareTo(BigDecimal.ZERO) != 0 &&
                        !e.getSymbol().equals("BTC"))
                .sorted(Comparator.comparing(Holding::getPercent).reversed())
                .collect(Collectors.toList());
        return PortfolioDistribution.builder()
                .portfolioName(portfolio.getName())
                .holdings(HoldingConverter.Mapper.createFromEntities(holdings))
                .build();
    }

    public PortfolioDistribution calculatePortfolioInBtcAndUsdt(String name) {
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing param.");
        }
        if (Objects.equals(name.toLowerCase(Locale.ROOT), "all")) {
            List<Portfolio> portfolios = portfolioService.getAll();
            return getPortfolios(portfolios);
        }
        Portfolio portfolio = portfolioService.findOrSave(name);
        return calculatePortfolioInBtcAndUsdt(portfolio);
    }

    @Transactional
    public List<PortfolioDistribution> calculatePortfolioInBtcAndUsdt() {
        List<Portfolio> portfolios = portfolioService.findAll();
        return portfolios.stream()
                .map(this::calculatePortfolioInBtcAndUsdt)
                .collect(Collectors.toList());
    }

    public PortfolioDistribution calculatePortfolioInBtcAndUsdt(Portfolio portfolio) {
        PortfolioDistribution portfolioDistribution = PortfolioDistribution.builder()
                .portfolioName(portfolio.getName())
                .holdings(new ArrayList<>())
                .build();

        addHoldingsToPortfolioDistribution(portfolio, portfolioDistribution);

        BigDecimal totalUsdt = portfolioDistribution.getTotalInUsdt();
        portfolioDistribution.setTotalUsdt(totalUsdt);
        portfolioDistribution.getHoldings().forEach(
                e -> {
                    e.setPercentage(e.getAmountInUsdt()
                            .divide(totalUsdt, 7, RoundingMode.DOWN)
                            .multiply(new BigDecimal(100)));
                    updateHolding(e, portfolio);
                }
        );
        return portfolioDistribution;
    }

    private void addHoldingsToPortfolioDistribution(Portfolio portfolio, PortfolioDistribution portfolioDistribution) {
        portfolio.getHoldings()
                .forEach(e -> {
                    Map<String, Double> price = getSymbolHistoricPriceHelper.getPrice(e.getSymbol());
                    BigDecimal btcprice = BigDecimal.valueOf(price.get(BTC) != null ? price.get(BTC) : 0d);
                    BigDecimal usdtprice = BigDecimal.valueOf(price.get(USDT) != null ? price.get(USDT) : 0d);
                    // TODO: use converter to create this HoldingDto.
                    portfolioDistribution.getHoldings().add(HoldingDto.builder()
                            .symbol(e.getSymbol())
                            .amount(e.getAmount())
                            .priceInBtc(btcprice)
                            .priceInUsdt(usdtprice)
                            .amountInBtc(BTC.equals(e.getSymbol()) ? e.getAmount() :
                                    btcprice.multiply(e.getAmount()))
                            .amountInUsdt(USDT.equals(e.getSymbol()) ? e.getAmount() :
                                    usdtprice.multiply(e.getAmount()))
                            .totalAmountBought(e.getTotalAmountBought())
                            .totalAmountSold(e.getTotalAmountSold())
                            .stableTotalCost(e.getStableTotalCost())
                            .currentPositionInUsdt(e.getCurrentPositionInUsdt())
                            .build());
                }
        );
    }

    private HoldingDto updateHolding(HoldingDto e, Portfolio portfolio) {
        return holdingService.updatePercentageHolding(e, portfolio);
    }

    private PortfolioDistribution getPortfolios(List<Portfolio> portfolios) {
        return getPortfoliosWithHoldings(portfolios.stream()
                .flatMap(e -> e.getHoldings().stream())
                .collect(Collectors.toList()));
    }

    public PortfolioDistribution getPortfoliosWithHoldings(List<Holding> holdings) {
        PortfolioDistribution portfolioDistribution = PortfolioDistribution.builder()
                .portfolioName("All")
                .holdings(new ArrayList<>())
                .build();

        Map<String, HoldingDto> holdingsBySymbol = groupHoldingsBySymbol(holdings);
        List<HoldingDto> holdingDtos = new ArrayList<>(holdingsBySymbol.values());
        portfolioDistribution.setHoldings(holdingDtos);
        portfolioDistribution.calculateHoldingPercent();
        holdingDtos.sort(Comparator.comparing(HoldingDto::getAmountInUsdt, Comparator.reverseOrder()));
        return portfolioDistribution;
    }

    // this could be in PorrfolioDistribution or other new class
    public Map<String, HoldingDto> groupHoldingsBySymbol(List<Holding> holdings) {
        return holdings.stream()
                .collect(Collectors.toMap(Holding::getSymbol, // Group holdings by symbol
                        HoldingConverter.Mapper::createFrom,
                        (e1, e2) -> {
                            log.info("Holding1:" + e1.getSymbol() + " - Holding2:" + e2.getSymbol());
                            BigDecimal priceInUsdt = (e1.getPriceInUsdt() != null && e2.getPriceInUsdt() != null) ?
                                    e1.getPriceInUsdt().add(e2.getPriceInUsdt()) :
                                    BigDecimal.ZERO;
                            BigDecimal priceInBtc = (e1.getPriceInBtc() != null && e2.getPriceInBtc() != null) ?
                                    e1.getPriceInBtc().add(e2.getPriceInBtc()) : BigDecimal.ZERO;
                            return HoldingDto.builder()
                                    .symbol(e1.getSymbol())
                                    .portfolioName(e1.getPortfolioName() + " - " + e2.getPortfolioName())
                                    .priceInUsdt(priceInUsdt)
                                    .priceInBtc(priceInBtc)
                                    .amount(e1.getAmount().add(e2.getAmount()))
                                    .amountInUsdt(e1.getAmountInUsdt().add(e2.getAmountInUsdt()).setScale(0, RoundingMode.DOWN))
                                    .amountInBtc(e1.getAmountInBtc().add(e2.getAmountInBtc()))
                                    .percentage(e1.getPercentage().add(e2.getPercentage()))
                                    .build();
                        }
                ));
    }

    public ResponseEntity<byte[]> downloadExcel() {
        List<Portfolio> portfolios = portfolioService.findAll();
        // Sample list of HoldingDto (Replace this with your actual data)
        PortfolioDistribution portfoliosDistribution = getPortfolios(portfolios);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             Workbook workbook = createWorkbook(portfoliosDistribution)) {

            workbook.write(outputStream);
            byte[] excelBytes = outputStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "portfolio_distribution.xlsx");

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Workbook createWorkbook(PortfolioDistribution portfolios) {
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Portfolio Distribution");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Symbol", "Portfolio Name", "Amount", "Amount in BTC", "Amount in USDT", "Percentage"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // Populate data rows
        int rowNum = 1;
        for (HoldingDto holding : portfolios.getHoldings()) {
            Row row = sheet.createRow(rowNum++);
            BigDecimal getAmountInBtc = getGetAmount(holding.getAmountInBtc());
            BigDecimal amount = getGetAmount(holding.getAmount());
            BigDecimal getAmountInUsdt = getGetAmount(holding.getAmountInUsdt());
            row.createCell(0).setCellValue(holding.getSymbol());
            row.createCell(1).setCellValue(holding.getPortfolioName());
            row.createCell(2).setCellValue(amount.doubleValue());
            row.createCell(3).setCellValue(getAmountInBtc.doubleValue());
            row.createCell(4).setCellValue(getAmountInUsdt.doubleValue());
            row.createCell(5).setCellValue(holding.getPercentage().doubleValue());
        }

        // Autosize columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        return workbook;
    }

    private BigDecimal getGetAmount(BigDecimal holding) {
        return Optional.ofNullable(holding)
                .filter(Objects::nonNull)
                .orElse(BigDecimal.ZERO);
    }
}
