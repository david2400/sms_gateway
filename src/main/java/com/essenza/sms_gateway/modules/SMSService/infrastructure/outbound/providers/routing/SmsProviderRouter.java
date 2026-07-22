package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.routing;

import com.essenza.sms_gateway.modules.SMSService.domain.model.*;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.MessageRouterPort;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.SmsProviderPort;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.config.SmsProvidersProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

/**
 * Router para proveedores SMS: coordina la POLÍTICA de enrutamiento y failover
 * (qué hacer y en qué orden), delegando en {@link ProviderResolver} la selección
 * concreta de proveedor y en {@link ProviderExecutionTracker} la ejecución con
 * medición de métricas de salud.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsProviderRouter implements MessageRouterPort {

    private final SmsProviderRegistry registry;
    private final SmsProvidersProperties config;
    private final ProviderResolver resolver;
    private final ProviderExecutionTracker tracker;

    /**
     * Ejecuta una operación con el proveedor según la estrategia configurada
     */
    @Override
    public <T> T execute(Function<SmsProviderPort, T> operation, Function<String, T> errorHandler) {
        ProviderRoutingStrategy strategy = config.getRouting().getStrategy();
        log.debug("[ROUTER] Ejecutando con estrategia: {}", strategy);
        
        // Debug: mostrar proveedores disponibles
        log.info("[ROUTER] Proveedores registrados: {}", registry.size());
        registry.getAll().forEach(p -> log.info("[ROUTER] - {} (disponible: {})", p.getProviderName(), p.isAvailable()));
        
        switch (strategy) {
            case FAILOVER:
                return executeWithFailover(operation, errorHandler);
            case ROUND_ROBIN:
                return executeRoundRobin(operation, errorHandler);
            case BEST_PERFORMANCE:
                return executeBestPerformance(operation, errorHandler);
            case PRIMARY_ONLY:
            default:
                return executePrimaryOnly(operation, errorHandler);
        }
    }

    /**
     * Ejecuta solo con el proveedor principal
     */
    private <T> T executePrimaryOnly(Function<SmsProviderPort, T> operation, Function<String, T> errorHandler) {
        Optional<SmsProviderPort> provider = resolver.resolvePrimary();

        if (provider.isEmpty() || !provider.get().isAvailable()) {
            SmsProvider primaryType = resolver.primaryType();
            log.error("[ROUTER] Proveedor principal {} no disponible", primaryType.getName());
            return errorHandler.apply("Proveedor no disponible: " + primaryType.getName());
        }
        
        return tracker.executeWithTracking(provider.get(), operation, errorHandler);
    }

    /**
     * Ejecuta con failover automático si el principal falla
     */
    private <T> T executeWithFailover(Function<SmsProviderPort, T> operation, Function<String, T> errorHandler) {
        String primaryCode = config.getRouting().getPrimary();

        // Intentar con el proveedor principal primero
        Optional<SmsProviderPort> primary = resolver.resolvePrimary();

        if (primary.isPresent() && primary.get().isAvailable()) {
            try {
                T result = tracker.executeWithTracking(primary.get(), operation, errorHandler);
                if (tracker.isSuccessfulResult(result)) {
                    return result;
                }
            } catch (Exception e) {
                SmsProvider primaryType = resolver.primaryType();
                log.warn("[ROUTER] Fallo en proveedor principal {}: {}", primaryType.getName(), e.getMessage());
                registry.recordFailure(primaryType, e.getMessage());
            }
        }
        
        // Intentar con los proveedores de fallback en orden
        for (String fallbackCode : resolver.fallbackCodes()) {
            if (fallbackCode.equals(primaryCode)) continue;
            
            SmsProvider fallbackType = SmsProvider.fromCode(fallbackCode);
            Optional<SmsProviderPort> fallback = resolver.resolveByCode(fallbackCode);
            if (fallback.isPresent() && fallback.get().isAvailable()) {
                log.info("[ROUTER] Intentando fallback con: {}", fallbackType.getName());
                try {
                    T result = tracker.executeWithTracking(fallback.get(), operation, errorHandler);
                    if (tracker.isSuccessfulResult(result)) {
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("[ROUTER] Fallo en fallback {}: {}", fallbackType.getName(), e.getMessage());
                    registry.recordFailure(fallbackType, e.getMessage());
                }
            }
        }
        
        log.error("[ROUTER] Todos los proveedores fallaron");
        return errorHandler.apply("Todos los proveedores fallaron");
    }

    /**
     * Ejecuta con round-robin entre proveedores disponibles
     */
    private <T> T executeRoundRobin(Function<SmsProviderPort, T> operation, Function<String, T> errorHandler) {
        Optional<SmsProviderPort> provider = resolver.resolveRoundRobin();

        if (provider.isEmpty()) {
            log.error("[ROUTER] No hay proveedores disponibles");
            return errorHandler.apply("No hay proveedores disponibles");
        }
        
        log.debug("[ROUTER] Round-robin seleccionó: {}", provider.get().getProviderName());
        return tracker.executeWithTracking(provider.get(), operation, errorHandler);
    }

    /**
     * Ejecuta con el proveedor de mejor rendimiento
     */
    private <T> T executeBestPerformance(Function<SmsProviderPort, T> operation, Function<String, T> errorHandler) {
        Optional<SmsProviderPort> best = resolver.resolveBestPerformance();

        if (best.isEmpty()) {
            log.error("[ROUTER] No hay proveedores disponibles");
            return errorHandler.apply("No hay proveedores disponibles");
        }
        
        log.debug("[ROUTER] Mejor rendimiento seleccionó: {}", best.get().getProviderName());
        
        return tracker.executeWithTracking(best.get(), operation, errorHandler);
    }

    /**
     * Obtiene el proveedor actual según la estrategia
     */
    @Override
    public SmsProviderPort getCurrentProvider() {
        return resolver.resolveCurrent(config.getRouting().getStrategy());
    }

    /**
     * Cambia la estrategia de enrutamiento
     */
    public void setRoutingStrategy(ProviderRoutingStrategy strategy) {
        config.getRouting().setStrategy(strategy);
        log.info("[ROUTER] Estrategia cambiada a: {}", strategy);
    }

    /**
     * Cambia el proveedor principal en tiempo de ejecución
     */
    public void switchProvider(SmsProvider provider) {
        config.getRouting().setPrimary(provider.getCode());
        log.info("[ROUTER] Proveedor principal cambiado a: {}", provider.getName());
    }

    /**
     * Verifica si el proveedor actual está disponible
     */
    public boolean isCurrentProviderAvailable() {
        SmsProviderPort provider = getCurrentProvider();
        return provider != null && provider.isAvailable();
    }

    /**
     * Obtiene información resumida del estado del router
     */
    public String getCurrentProviderInfo() {
        RouterStats stats = getStats();
        return String.format("Strategy: %s, Primary: %s, Available: %d/%d",
                stats.getCurrentStrategy(),
                stats.getPrimaryProvider().getName(),
                stats.getAvailableProviders(),
                stats.getRegisteredProviders());
    }

    /**
     * Obtiene estadísticas del router
     */
    public RouterStats getStats() {
        return RouterStats.builder()
                .currentStrategy(config.getRouting().getStrategy())
                .primaryProvider(SmsProvider.fromCode(config.getRouting().getPrimary()))
                .registeredProviders(registry.size())
                .availableProviders(registry.getAvailable().size())
                .healthStatuses(registry.getAllHealthStatuses())
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class RouterStats {
        private ProviderRoutingStrategy currentStrategy;
        private SmsProvider primaryProvider;
        private int registeredProviders;
        private int availableProviders;
        private java.util.Map<SmsProvider, ProviderHealthStatus> healthStatuses;
    }
}
