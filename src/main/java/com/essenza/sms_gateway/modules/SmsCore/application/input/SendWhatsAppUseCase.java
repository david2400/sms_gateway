package com.essenza.sms_gateway.modules.SmsCore.application.input;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendWhatsAppRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;

/**
 * Envío de WhatsApp por el mismo motor del SMS: ruta por tipo de mensaje (WHATSAPP), servicios por proveedor,
 * reintentos, fallback (solo proveedores con el servicio WhatsApp) y log.
 */
public interface SendWhatsAppUseCase {

    /**
     * @param providerCode proveedor del endpoint propio, o {@code null} para el central
     */
    DispatchResult send(SendWhatsAppRequest request, String idempotencyKey, String providerCode);
}
