package com.essenza.sms_gateway.modules.SMSService.domain.ports;

import java.util.function.Function;

/**
 * Puerto de dominio que abstrae la selección y ejecución de operaciones
 * sobre un {@link SmsProviderPort}, aplicando la estrategia de enrutamiento,
 * failover y reintentos configurada (ver implementación de infraestructura
 * {@code SmsProviderRouter}).
 * <p>
 * El dominio depende únicamente de este contrato, nunca de una
 * implementación concreta de infraestructura.
 */
public interface MessageRouterPort {

    /**
     * Ejecuta una operación contra el proveedor resuelto según la estrategia
     * de enrutamiento activa (failover, round-robin, mejor rendimiento, etc.)
     *
     * @param operation    acción a ejecutar sobre el proveedor seleccionado
     * @param errorHandler función que construye el resultado de error cuando
     *                     ningún proveedor pudo completar la operación
     */
    <T> T execute(Function<SmsProviderPort, T> operation, Function<String, T> errorHandler);

    /**
     * Obtiene el proveedor actualmente resuelto según la estrategia configurada
     */
    SmsProviderPort getCurrentProvider();
}
