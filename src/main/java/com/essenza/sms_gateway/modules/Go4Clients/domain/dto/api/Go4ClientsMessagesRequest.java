package com.essenza.sms_gateway.modules.Go4Clients.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** Body de {@code POST /campaigns/sms/v1.0/{campaignId}}: enviar el mensaje a los destinatarios de la campaña. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Go4ClientsMessagesRequest {

    /** HIGH (defecto), MEDIUM, LOW. */
    @JsonProperty("priority")
    private String priority;

    @JsonProperty("message")
    private String message;

    @JsonProperty("destinationsList")
    private List<Destination> destinationsList;

    /** Campos personalizados por destinatario para landings (opcional). */
    @JsonProperty("landingCustomFields")
    private Map<String, Map<String, String>> landingCustomFields;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Destination {
        /** Número nacional, sin código de país. */
        @JsonProperty("phoneNumber")
        private String phoneNumber;
        @JsonProperty("countryCode")
        private String countryCode;
    }
}
