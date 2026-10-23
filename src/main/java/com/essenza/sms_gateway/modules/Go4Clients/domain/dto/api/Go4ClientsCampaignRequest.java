package com.essenza.sms_gateway.modules.Go4Clients.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body de {@code POST /campaigns/sms/v1.0}: crear campaña. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Go4ClientsCampaignRequest {
    @JsonProperty("name")
    private String name;
    /** Remitente; {@code RANDOM} si no se configura. */
    @JsonProperty("sender")
    private String sender;
}
