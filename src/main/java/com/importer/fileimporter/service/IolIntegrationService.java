package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.integration.iol.*;
import com.importer.fileimporter.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class IolIntegrationService {

    private final IolApiService iolApiService;
    private final RawResponseService rawResponseService;

    public IolAccountStatementResponse getProcessedAccountStatement(User user) {
        IolAccountStatementResponse raw = iolApiService.getAccountStatement(user);
        rawResponseService.saveResponse(user, com.importer.fileimporter.entity.ExchangeName.IOL, "ACCOUNT_STATEMENT", null, raw);
        return raw;
    }

    public IolPortfolioResponse getProcessedPortfolio(User user, String country) {
        IolPortfolioResponse raw = iolApiService.getPortfolio(user, country);
        rawResponseService.saveResponse(user, com.importer.fileimporter.entity.ExchangeName.IOL, "PORTFOLIO_" + country, null, raw);
        return raw;
    }

    public List<IolOperationResponse> getProcessedOperations(User user) {
        return iolApiService.getOperations(user);
    }

    public IolProfileResponse getProcessedProfile(User user) {
        return iolApiService.getProfile(user);
    }
}
