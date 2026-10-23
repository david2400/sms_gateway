package com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Respuesta de {@code /momessage}: {@code { "Id": <28 dígitos>, "Message": "OK"|"ERROR", "Status": true|false }}.
 * {@code Id} es el número de transacción (BigDecimal para no perder precisión).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InalambriaMoMessageResponse {
    @JsonProperty("Id")
    private BigDecimal id;
    @JsonProperty("Message")
    private String message;
    @JsonProperty("Status")
    private Boolean status;
    @JsonIgnore
    private String rawBody;
}
