package com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Respuesta de WhatsApp: {@code { messageText, httpStatusCode, transactionNumber, errors, errorDetails }}.
 * {@code transactionNumber} tiene 28 dígitos: BigDecimal para no perder precisión.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InalambriaWhatsAppResponse {
    @JsonProperty("messageText")
    private String messageText;
    @JsonProperty("httpStatusCode")
    private Integer httpStatusCode;
    @JsonProperty("transactionNumber")
    private BigDecimal transactionNumber;
    /** Errores de validación separados por " | ". */
    @JsonProperty("errors")
    private String errors;
    @JsonProperty("errorDetails")
    private String errorDetails;
    @JsonIgnore
    private String rawBody;
}
