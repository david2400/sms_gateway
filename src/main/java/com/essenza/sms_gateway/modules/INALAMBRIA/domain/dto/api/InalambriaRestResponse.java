package com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de la API REST SMS de Inalambria ({@code /MTMessage}, {@code /mtmessage/massive}):
 * {@code { "TransactionNumber": "...", "Status": 0|1, "MessageText": "..." }}. Status 0 = validación correcta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InalambriaRestResponse {
    @JsonProperty("TransactionNumber")
    private String transactionNumber;
    @JsonProperty("Status")
    private Integer status;
    @JsonProperty("MessageText")
    private String messageText;
    /** Body tal como llegó (para el detalle del error si no es JSON). */
    @JsonIgnore
    private String rawBody;
}
