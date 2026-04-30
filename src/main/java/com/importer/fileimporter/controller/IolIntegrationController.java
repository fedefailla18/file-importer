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

    private final IolApiService iolApiService;

    @GetMapping("/account-statement")
    @Operation(summary = "Get IOL account statement (balances)")
    public ResponseEntity<IolAccountStatementResponse> getAccountStatement(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(iolApiService.getAccountStatement(user));
    }

    @GetMapping("/portfolio/{country}")
    @Operation(summary = "Get IOL portfolio assets")
    public ResponseEntity<IolPortfolioResponse> getPortfolio(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Country (argentina or estados_unidos)") @PathVariable String country) {
        return ResponseEntity.ok(iolApiService.getPortfolio(user, country));
    }

    @GetMapping("/operations")
    @Operation(summary = "Get list of IOL operations")
    public ResponseEntity<List<IolOperationResponse>> getOperations(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(iolApiService.getOperations(user));
    }

    @GetMapping("/operations/{number}")
    @Operation(summary = "Get specific IOL operation details")
    public ResponseEntity<IolOperationResponse> getOperationDetails(
            @AuthenticationPrincipal User user,
            @PathVariable Long number) {
        return ResponseEntity.ok(iolApiService.getOperationDetails(user, number));
    }

    @GetMapping("/profile")
    @Operation(summary = "Get IOL user profile data")
    public ResponseEntity<IolProfileResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(iolApiService.getProfile(user));
    }
}
