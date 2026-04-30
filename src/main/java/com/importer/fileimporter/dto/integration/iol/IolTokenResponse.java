package com.importer.fileimporter.dto.integration.iol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IolTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    @JsonProperty("expires_in")
    private Long expiresIn;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty(".issued")
    private String issued;
    
    @JsonProperty(".expires")
    private String expires;
}
