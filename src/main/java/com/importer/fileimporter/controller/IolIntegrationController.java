package com.importer.fileimporter.controller;

import com.importer.fileimporter.dto.integration.iol.*;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.service.IolApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/integration/iol")
@RequiredArgsConstructor
@Tag(name = "IOL Integration", description = "Endpoints for fetching account and portfolio data from InvertirOnline")
public class IolIntegrationController {

    private final com.importer.fileimporter.service.IolIntegrationService iolIntegrationService;

    @GetMapping("/account-statement")
    @Operation(summary = "Get IOL account statement (balances)")
    public ResponseEntity<com.importer.fileimporter.dto.integration.iol.IolAccountStatementResponse> getAccountStatement(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(iolIntegrationService.getProcessedAccountStatement(user));
    }

    @GetMapping("/portfolio/{country}")
    @Operation(summary = "Get IOL portfolio assets")
    public ResponseEntity<com.importer.fileimporter.dto.integration.iol.IolPortfolioResponse> getPortfolio(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Country (argentina or estados_unidos)") @PathVariable String country) {
        return ResponseEntity.ok(iolIntegrationService.getProcessedPortfolio(user, country));
    }

    @GetMapping("/operations")
    @Operation(summary = "Get list of IOL operations")
    public ResponseEntity<List<com.importer.fileimporter.dto.integration.iol.IolOperationResponse>> getOperations(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(iolIntegrationService.getProcessedOperations(user));
    }

    @GetMapping("/operations/{number}")
    @Operation(summary = "Get specific IOL operation details")
    public ResponseEntity<com.importer.fileimporter.dto.integration.iol.IolOperationResponse> getOperationDetails(
            @AuthenticationPrincipal User user,
            @PathVariable Long number) {
        // Since we don't have a processed version for single operation yet, we call the api service or add it to integration
        return ResponseEntity.ok(iolIntegrationService.getProcessedOperations(user).stream()
                .filter(o -> o.getNumero().equals(number))
                .findFirst()
                .orElse(null));
    }

    @GetMapping("/profile")
    @Operation(summary = "Get IOL user profile data")
    public ResponseEntity<com.importer.fileimporter.dto.integration.iol.IolProfileResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(iolIntegrationService.getProcessedProfile(user));
    }
}
