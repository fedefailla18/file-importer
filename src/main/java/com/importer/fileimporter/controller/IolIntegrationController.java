package com.importer.fileimporter.controller;

import com.importer.fileimporter.dto.integration.iol.*;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.service.IolApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/integration/iol")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "IOL Integration", description = "Endpoints for fetching account and portfolio data from InvertirOnline")
public class IolIntegrationController {

    private final com.importer.fileimporter.service.IolIntegrationService iolIntegrationService;

    /**
     * IolErrorDecoder re-throws IOL's own HTTP status verbatim (e.g. 401 when IOL rejects the
     * stored username/password during token refresh). If that reaches the client as-is, the
     * FE's global axios interceptor treats ANY 401 as "this app's session expired" and logs the
     * user out entirely — even though it's IOL's credentials, not the app's JWT, that failed.
     * Remap to 424 (Failed Dependency) so an IOL-side auth failure surfaces as an IOL-specific
     * error instead of ejecting the user from the whole app.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleIolError(ResponseStatusException ex) {
        log.error("IOL integration error: {}", ex.getMessage());
        HttpStatus status = (ex.getStatus() == HttpStatus.UNAUTHORIZED || ex.getStatus() == HttpStatus.FORBIDDEN)
                ? HttpStatus.FAILED_DEPENDENCY
                : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(ex.getReason());
    }

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
