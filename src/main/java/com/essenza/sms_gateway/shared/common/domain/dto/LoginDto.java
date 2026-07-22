package com.essenza.sms_gateway.shared.common.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.commons.text.WordUtils;

@Data
public class LoginDto {

    @JsonProperty("api_token")
    private String apiToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Long expiresIn;

    public String getApiToken() {
        return WordUtils.capitalize(tokenType)+" "+apiToken;
    }

    public String getOnlyToken(){
        return apiToken;
    }
}
