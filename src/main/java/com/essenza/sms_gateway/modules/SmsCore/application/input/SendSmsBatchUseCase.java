package com.essenza.sms_gateway.modules.SmsCore.application.input;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsBatchRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsBatchResponse;

/**
 * Envío masivo por el endpoint central (ruta proveedor NULL).
 */
public interface SendSmsBatchUseCase {

    SmsBatchResponse send(SendSmsBatchRequest request);
}
