package com.essenza.sms_gateway.modules.SmsCore.application.input;

/**
 * Recarga la configuración de rutas/cuentas desde BD (descarta la caché).
 */
public interface RefreshSmsRoutingUseCase {

    void refresh();
}
