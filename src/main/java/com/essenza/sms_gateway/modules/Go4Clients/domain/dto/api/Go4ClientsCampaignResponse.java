package com.essenza.sms_gateway.modules.Go4Clients.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta de crear campaña: {@code { "id": "...", "reference": "..." }}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Go4ClientsCampaignResponse {
    @JsonProperty("id")
    private String id;
    @JsonProperty("reference")
    private String reference;
    @JsonIgnore
    private String rawBody;
}
