package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.routing;

import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsProvider;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.SmsProviderPort;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.config.SmsProvidersProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Responsable únicamente de RESOLVER qué proveedor corresponde según cada
 * estrategia de enrutamiento (primario, round-robin, mejor rendimiento,
 * fallback). No ejecuta operaciones ni conoce políticas de reintento/tracking;
 * esas responsabilidades viven en {@link ProviderExecutionTracker} y en
 * {@code SmsProviderRouter} respectivamente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderResolver {

    private final SmsProviderRegistry registry;
    private final SmsProvidersProperties config;

    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    public SmsProvider primaryType() {
        return SmsProvider.fromCode(config.getRouting().getPrimary());
    }

    public List<String> fallbackCodes() {
        return config.getRouting().getFallback();
    }

    public Optional<SmsProviderPort> resolvePrimary() {
        return registry.get(primaryType());
    }

    public Optional<SmsProviderPort> resolveByCode(String code) {
        return registry.get(SmsProvider.fromCode(code));
    }

    /**
     * Avanza el índice round-robin y devuelve el proveedor seleccionado
     */
    public Optional<SmsProviderPort> resolveRoundRobin() {
        List<SmsProviderPort> available = registry.getAvailable();
        if (available.isEmpty()) {
            return Optional.empty();
        }
        int index = roundRobinIndex.getAndUpdate(i -> (i + 1) % available.size());
        return Optional.of(available.get(index % available.size()));
    }

    /**
     * Consulta (sin avanzar el índice) qué proveedor tocaría en round-robin
     */
    public Optional<SmsProviderPort> peekRoundRobin() {
        List<SmsProviderPort> available = registry.getAvailable();
        if (available.isEmpty()) {
            return Optional.empty();
        }
        int index = roundRobinIndex.get() % available.size();
        return Optional.of(available.get(index));
    }

    public Optional<SmsProviderPort> resolveBestPerformance() {
        List<SmsProviderPort> byPerformance = registry.getByPerformance();
        return byPerformance.isEmpty() ? Optional.empty() : Optional.of(byPerformance.get(0));
    }

    /**
     * Resuelve el proveedor "actual" (sin ejecutar nada) según la estrategia dada
     */
    public SmsProviderPort resolveCurrent(ProviderRoutingStrategy strategy) {
        switch (strategy) {
            case BEST_PERFORMANCE:
                return resolveBestPerformance().orElse(null);
            case ROUND_ROBIN:
                return peekRoundRobin().orElse(null);
            default:
                return resolvePrimary().orElse(null);
        }
    }
}
