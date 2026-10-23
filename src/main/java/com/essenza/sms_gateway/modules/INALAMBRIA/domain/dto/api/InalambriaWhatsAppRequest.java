package com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** Body de {@code POST /MultiChannelInput/SendMessageMetaWhatsApp}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InalambriaWhatsAppRequest {
    @JsonProperty("source")
    private String source;
    @JsonProperty("destination")
    private String destination;
    @JsonProperty("templateId")
    private String templateId;
    @JsonProperty("parameters")
    private List<String> parameters;
    /** Se conserva la escritura del servicio ("Aditional"). */
    @JsonProperty("optionalAditionalData")
    private Map<String, String> optionalAditionalData;
}
