package com.essenza.sms_gateway.modules.Go4Clients.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Resultado de una operación de campaña en Go4Clients (crear o enviar mensajes).
 * Las campañas no pasan por el motor de SmsCore: sin reintentos, fallback ni feedback.
 */
@Value
@Builder
@Schema(description = "Resultado de operación de campaña Go4Clients")
public class Go4ClientsCampaignResult {

    /** No hay cuenta activa de Go4Clients que usar. */
    public static final String ERROR_SIN_CUENTA = "CUENTA_NO_CONFIGURADA";

    boolean success;
    String campaignId;
    @Schema(description = "Referencia devuelta por Go4Clients al crear la campaña")
    String reference;
    @Schema(description = "Cantidad de mensajes generados (envío de mensajes)")
    Integer generatedMessages;
    @Schema(description = "Cuenta usada")
    String cuenta;
    Integer httpStatus;
    String errorCode;
    String errorMessage;

    public static Go4ClientsCampaignResult failure(String cuenta, Integer httpStatus, String errorCode, String errorMessage) {
        return Go4ClientsCampaignResult.builder()
                .success(false).cuenta(cuenta).httpStatus(httpStatus)
                .errorCode(errorCode).errorMessage(errorMessage)
                .build();
    }
}
