package com.importer.fileimporter.client;

import com.importer.fileimporter.dto.integration.iol.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "iolClient", url = "https://api.invertironline.com", configuration = IolFeignConfig.class)
public interface IolClient {

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    IolTokenResponse login(@RequestBody Map<String, ?> formData);

    @GetMapping("/api/v2/estadocuenta")
    IolAccountStatementResponse getAccountStatement(@RequestHeader("Authorization") String bearerToken);

    @GetMapping("/api/v2/portafolio/{pais}")
    IolPortfolioResponse getPortfolio(@RequestHeader("Authorization") String bearerToken, 
                                      @PathVariable("pais") String country);

    @GetMapping("/api/v2/operaciones")
    List<IolOperationResponse> getOperations(@RequestHeader("Authorization") String bearerToken);

    @GetMapping("/api/v2/operaciones/{numero}")
    IolOperationResponse getOperationDetails(@RequestHeader("Authorization") String bearerToken, 
                                             @PathVariable("numero") Long operationNumber);

    @GetMapping("/api/v2/datos-perfil")
    IolProfileResponse getProfile(@RequestHeader("Authorization") String bearerToken);
}
