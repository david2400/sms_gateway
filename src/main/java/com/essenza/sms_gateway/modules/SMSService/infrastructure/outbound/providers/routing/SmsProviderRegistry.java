package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.routing;

import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsProvider;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.SmsProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registro centralizado de proveedores SMS
 * Permite registrar/desregistrar proveedores dinámicamente
 * y mantiene el estado de salud de cada uno
 */
@Slf4j
@Component
public class SmsProviderRegistry {

    private final Map<SmsProvider, SmsProviderPort> providers = new ConcurrentHashMap<>();
    private final Map<SmsProvider, ProviderHealthStatus> healthStatuses = new ConcurrentHashMap<>();
    private final List<SmsProviderPort> injectedProviders;

    public SmsProviderRegistry(List<SmsProviderPort> injectedProviders) {
        this.injectedProviders = injectedProviders;
    }

    @PostConstruct
    public void init() {
        log.info("[REGISTRY] Inyectados {} providers: {}", 
                injectedProviders.size(),
                injectedProviders.stream().map(p -> p.getClass().getSimpleName() + "(" + p.getProviderName() + ")").collect(Collectors.joining(", ")));
        
        // Registrar todos los proveedores inyectados por Spring
        injectedProviders.forEach(this::register);
        log.info("[REGISTRY] ✅ Inicializado con {} proveedores: {}", 
                providers.size(), 
                providers.keySet().stream().map(SmsProvider::getName).collect(Collectors.joining(", ")));
    }

    /**
     * Registra un nuevo proveedor
     */
    public void register(SmsProviderPort provider) {
        SmsProvider type = provider.getProviderType();
        providers.put(type, provider);
        healthStatuses.put(type, ProviderHealthStatus.builder()
                .provider(type)
                .available(provider.isAvailable())
                .successCount(0)
                .failureCount(0)
                .averageResponseTimeMs(0)
                .lastCheck(Instant.now())
                .build());
        log.info("[REGISTRY] Proveedor registrado: {} (disponible: {})", type.getName(), provider.isAvailable());
    }

    /**
     * Desregistra un proveedor
     */
    public void unregister(SmsProvider type) {
        providers.remove(type);
        healthStatuses.remove(type);
        log.info("[REGISTRY] Proveedor desregistrado: {}", type.getName());
    }

    /**
     * Obtiene un proveedor específico
     */
    public Optional<SmsProviderPort> get(SmsProvider type) {
        return Optional.ofNullable(providers.get(type));
    }

    /**
     * Obtiene todos los proveedores registrados
     */
    public Collection<SmsProviderPort> getAll() {
        return Collections.unmodifiableCollection(providers.values());
    }

    /**
     * Obtiene proveedores disponibles (activos y saludables)
     */
    public List<SmsProviderPort> getAvailable() {
        return providers.entrySet().stream()
                .filter(e -> e.getValue().isAvailable())
                .filter(e -> {
                    ProviderHealthStatus status = healthStatuses.get(e.getKey());
                    return status == null || status.isHealthy();
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el estado de salud de un proveedor
     */
    public Optional<ProviderHealthStatus> getHealthStatus(SmsProvider type) {
        return Optional.ofNullable(healthStatuses.get(type));
    }

    /**
     * Obtiene todos los estados de salud
     */
    public Map<SmsProvider, ProviderHealthStatus> getAllHealthStatuses() {
        return Collections.unmodifiableMap(healthStatuses);
    }

    /**
     * Registra un éxito en un proveedor
     */
    public void recordSuccess(SmsProvider type, long responseTimeMs) {
        ProviderHealthStatus status = healthStatuses.get(type);
        if (status != null) {
            status.recordSuccess(responseTimeMs);
            log.debug("[REGISTRY] Éxito registrado para {}: {}ms", type.getName(), responseTimeMs);
        }
    }

    /**
     * Registra un fallo en un proveedor
     */
    public void recordFailure(SmsProvider type, String errorMessage) {
        ProviderHealthStatus status = healthStatuses.get(type);
        if (status != null) {
            status.recordFailure(errorMessage);
            log.warn("[REGISTRY] Fallo registrado para {}: {}", type.getName(), errorMessage);
        }
    }

    /**
     * Actualiza disponibilidad de un proveedor
     */
    public void updateAvailability(SmsProvider type, boolean available) {
        ProviderHealthStatus status = healthStatuses.get(type);
        if (status != null) {
            status.setAvailable(available);
            status.setLastCheck(Instant.now());
        }
    }

    /**
     * Verifica si un proveedor está registrado
     */
    public boolean isRegistered(SmsProvider type) {
        return providers.containsKey(type);
    }

    /**
     * Obtiene la cantidad de proveedores registrados
     */
    public int size() {
        return providers.size();
    }

    /**
     * Obtiene proveedores ordenados por rendimiento
     */
    public List<SmsProviderPort> getByPerformance() {
        return providers.entrySet().stream()
                .filter(e -> e.getValue().isAvailable())
                .sorted((a, b) -> {
                    ProviderHealthStatus statusA = healthStatuses.get(a.getKey());
                    ProviderHealthStatus statusB = healthStatuses.get(b.getKey());
                    if (statusA == null || statusB == null) return 0;
                    // Ordenar por tasa de éxito (mayor primero) y luego por tiempo de respuesta (menor primero)
                    int successCompare = Double.compare(statusB.getSuccessRate(), statusA.getSuccessRate());
                    if (successCompare != 0) return successCompare;
                    return Long.compare(statusA.getAverageResponseTimeMs(), statusB.getAverageResponseTimeMs());
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
}
