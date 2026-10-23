package com.essenza.sms_gateway.modules.SmsCore.application.input;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsBatchRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsBatchResponse;

/**
 * Envío masivo: mismo tipo de mensaje a muchos destinatarios (texto igual o por plantilla).
 */
public interface SendSmsBatchUseCase {

    /** Endpoint central (ruta proveedor NULL). */
    default SmsBatchResponse send(SendSmsBatchRequest request) {
        return send(request, null);
    }

    /**
     * @param providerCode proveedor del endpoint propio (ruta (proveedor, tipo)), o {@code null} para el central
     */
    SmsBatchResponse send(SendSmsBatchRequest request, String providerCode);
}
