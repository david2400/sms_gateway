package com.essenza.sms_gateway.modules.Mock.application.input;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;

/**
 * Envío de SMS solo por cuentas MOCK (ruta MOCK + tipo en BD).
 */
public interface MockSendSmsUseCase {

    DispatchResult send(SendSmsRequest request, String idempotencyKey);
}
