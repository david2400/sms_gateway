package com.essenza.sms_gateway.modules.SmsAdmin.domain.ports;

/** Invalida la caché de rutas del motor de envío cuando cambia la configuración. */
public interface RoutingCacheAdminPort {
    /** Programa la invalidación para después del commit de la transacción en curso. */
    void invalidarAlConfirmar();
}
