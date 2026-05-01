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

    public IolAccountStatementResponse getProcessedAccountStatement(User user) {
        IolAccountStatementResponse raw = iolApiService.getAccountStatement(user);
        // Here we can apply business logic or transformations (e.g. initial USD conversion logic)
        return raw;
    }

    public IolPortfolioResponse getProcessedPortfolio(User user, String country) {
        IolPortfolioResponse raw = iolApiService.getPortfolio(user, country);
        // Transform IOL raw data to our internal representation if needed
        return raw;
    }

    public List<IolOperationResponse> getProcessedOperations(User user) {
        return iolApiService.getOperations(user);
    }

    public IolProfileResponse getProcessedProfile(User user) {
        return iolApiService.getProfile(user);
    }
}
