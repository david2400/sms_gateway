package com.essenza.sms_gateway.modules.Go4Clients.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta de {@code GET /analytics/sms/v1.0?uniqueCampaignID=...}: {@code { "data": { totalMessages, ... } }}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Go4ClientsReportResponse {

    @JsonProperty("data")
    private Metrics data;

    @JsonIgnore
    private String rawBody;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metrics {
        @JsonProperty("totalMessages")
        private Integer totalMessages;
        @JsonProperty("deliveredMessages")
        private Integer deliveredMessages;
        @JsonProperty("failedMessages")
        private Integer failedMessages;
    }
}
