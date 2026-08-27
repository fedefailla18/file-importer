package com.importer.fileimporter.service;

import com.importer.fileimporter.client.IolClient;
import com.importer.fileimporter.dto.integration.iol.*;
import com.importer.fileimporter.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class IolApiService {

    private final IolClient iolClient;
    private final IolTokenService iolTokenService;

    public IolTokenResponse loginForUser(User user) {
        // This is now primarily for the login endpoint in the controller
        // It bypasses the cache to ensure we can manually trigger/verify credentials
        return iolTokenService.getAccessTokenResponse(user);
    }

    public IolAccountStatementResponse getAccountStatement(User user) {
        String token = iolTokenService.getAccessToken(user);
        return iolClient.getAccountStatement("Bearer " + token);
    }

    public IolPortfolioResponse getPortfolio(User user, String country) {
        String token = iolTokenService.getAccessToken(user);
        return iolClient.getPortfolio("Bearer " + token, country);
    }

    public List<IolOperationResponse> getOperations(User user) {
        String token = iolTokenService.getAccessToken(user);
        return iolClient.getOperations("Bearer " + token);
    }

    public IolOperationResponse getOperationDetails(User user, Long operationNumber) {
        String token = iolTokenService.getAccessToken(user);
        return iolClient.getOperationDetails("Bearer " + token, operationNumber);
    }

    public IolProfileResponse getProfile(User user) {
        String token = iolTokenService.getAccessToken(user);
        return iolClient.getProfile("Bearer " + token);
    }
}
