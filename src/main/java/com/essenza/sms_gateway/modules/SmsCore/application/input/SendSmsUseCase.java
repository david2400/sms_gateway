package com.essenza.sms_gateway.modules.SmsCore.application.input;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;

/**
 * Envío por el endpoint central: la ruta (proveedor, NULL) decide cuentas y proveedores.
 */
public interface SendSmsUseCase {

    DispatchResult send(SendSmsRequest request, String idempotencyKey);
}
