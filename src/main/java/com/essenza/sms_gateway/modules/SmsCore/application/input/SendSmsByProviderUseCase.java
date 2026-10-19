package com.essenza.sms_gateway.modules.SmsCore.application.input;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;

/**
 * Envío por el endpoint propio de un proveedor. Lo usan los módulos de proveedor
 * (ej. Inalambria): la ruta es (proveedor, tipo) y solo contiene cuentas de ese proveedor.
 * Se conservan los 3 intentos por cuenta y el log.
 */
public interface SendSmsByProviderUseCase {

    DispatchResult send(String providerCode, SmsDispatch message);
}
