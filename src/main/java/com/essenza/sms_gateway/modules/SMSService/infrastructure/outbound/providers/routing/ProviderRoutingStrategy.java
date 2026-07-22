package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.routing;

/**
 * Estrategias de enrutamiento entre proveedores SMS
 */
public enum ProviderRoutingStrategy {
    
    /**
     * Usa solo el proveedor principal configurado
     */
    PRIMARY_ONLY,
    
    /**
     * Usa el proveedor principal, si falla usa el siguiente disponible
     */
    FAILOVER,
    
    /**
     * Distribuye carga entre proveedores activos (round-robin)
     */
    ROUND_ROBIN,
    
    /**
     * Selecciona el proveedor con mejor rendimiento/disponibilidad
     */
    BEST_PERFORMANCE,
    
    /**
     * Usa todos los proveedores en paralelo y retorna el primero que responda
     */
    RACE,
    
    /**
     * Selección basada en costo (más barato primero)
     */
    COST_OPTIMIZED
}
