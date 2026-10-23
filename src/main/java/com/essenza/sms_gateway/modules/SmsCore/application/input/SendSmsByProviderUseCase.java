package com.essenza.sms_gateway.modules.SmsCore.application.input;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;

/**
 * Envío por el endpoint propio de un proveedor. Lo usan los módulos de proveedor
 * (ej. Inalambria): la ruta es (proveedor, tipo) y solo contiene cuentas de ese proveedor.
 * El proveedor debe tener habilitado el servicio (tipo de mensaje). Se conservan los intentos por cuenta y el log.
 */
public interface SendSmsByProviderUseCase {

    /** Mensaje ya construido por el módulo del proveedor. */
    DispatchResult send(String providerCode, SmsDispatch message);

    /** Request estándar (texto directo o plantilla + variables). */
    DispatchResult send(String providerCode, SendSmsRequest request, String idempotencyKey);
}
