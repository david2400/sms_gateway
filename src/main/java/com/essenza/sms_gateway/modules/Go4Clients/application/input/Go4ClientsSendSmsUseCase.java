package com.essenza.sms_gateway.modules.Go4Clients.application.input;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;

/**
 * Envío individual solo por cuentas de Go4Clients (ruta GO4CLIENTS + tipo en BD), con reintentos y log.
 */
public interface Go4ClientsSendSmsUseCase {

    DispatchResult send(SendSmsRequest request, String idempotencyKey);
}
