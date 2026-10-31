package com.essenza.sms_gateway.modules.SmsCore.domain.ports;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteLookup;
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
     * Ruta pedida explícitamente en el request.
     *
     * @param reference código ({@code CanalRuta.codigo}, sin importar mayúsculas) o id numérico de la ruta
     * @return la ruta con sus pasos activos en orden, o el motivo por el que no se puede usar (no existe / inactiva)
     */
    default RouteLookup findRoute(String reference) {
        return RouteLookup.notFound(reference);
    }

    /**
     * Descarta la caché de rutas para tomar cambios hechos en BD.
     */
    void refresh();
}
