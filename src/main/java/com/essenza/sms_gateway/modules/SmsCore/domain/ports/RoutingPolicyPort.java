package com.essenza.sms_gateway.modules.SmsCore.domain.ports;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RoutePlan;

/**
 * Resuelve la ruta configurada en base de datos.
 */
public interface RoutingPolicyPort {

    /**
     * @param providerCode código del proveedor, o {@code null} para la ruta del endpoint central
     * @param type         tipo de mensaje
     * @return la ruta activa con sus pasos en orden; vacía si no hay configuración
     */
    RoutePlan resolve(String providerCode, MessageType type);

    /**
     * Descarta la caché de rutas para tomar cambios hechos en BD.
     */
    void refresh();
}
