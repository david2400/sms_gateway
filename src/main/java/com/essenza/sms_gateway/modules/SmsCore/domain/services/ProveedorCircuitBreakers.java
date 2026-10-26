package com.essenza.sms_gateway.modules.SmsCore.domain.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Circuit breaker por proveedor (sms-PROVEEDOR) sobre el motor de envío.
 * <p>
 * Los senders devuelven {@link SendOutcome} en vez de lanzar excepciones, así que el resultado se
 * traduce manualmente:
 * <ul>
 *   <li>éxito → {@code onSuccess};</li>
 *   <li>fallo reintentable (timeout, red, 5xx) → {@code onError}: degrada al proveedor;</li>
 *   <li>fallo permanente (credenciales, payload rechazado) → no cuenta: no es una caída del proveedor.</li>
 * </ul>
 * Con circuito ABIERTO el paso de la ruta se salta sin consumir intentos (fallo permanente).
 * Registro {@code null} = deshabilitado (tests).
 */
@Slf4j
class ProveedorCircuitBreakers {

    static final String NOMBRE_CONFIG = "smsProveedor";

    private final CircuitBreakerRegistry registry;
    private final Set<String> instrumentados = ConcurrentHashMap.newKeySet();

    ProveedorCircuitBreakers(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    static ProveedorCircuitBreakers deshabilitado() {
        return new ProveedorCircuitBreakers(null);
    }

    /** {@code true} si el proveedor puede recibir una llamada (cerrado o half-open con cupo). */
    boolean permite(String providerCode) {
        CircuitBreaker cb = breaker(providerCode);
        return cb == null || cb.tryAcquirePermission();
    }

    /** Registra el resultado del intento en el breaker del proveedor. */
    void registrar(String providerCode, SendOutcome outcome, long durationNanos) {
        CircuitBreaker cb = breaker(providerCode);
        if (cb == null || outcome == null) {
            return;
        }
        if (outcome.isSuccess()) {
            cb.onSuccess(durationNanos, TimeUnit.NANOSECONDS);
        } else if (outcome.isRetryable()) {
            cb.onError(durationNanos, TimeUnit.NANOSECONDS, new RuntimeException(outcome.getDetail()));
        }
    }

    private CircuitBreaker breaker(String providerCode) {
        if (registry == null || providerCode == null) {
            return null;
        }
        CircuitBreaker cb = registry.circuitBreaker("sms-" + providerCode.toUpperCase(), NOMBRE_CONFIG);
        if (instrumentados.add(cb.getName())) {
            cb.getEventPublisher().onStateTransition(e -> log.warn(
                    "[SMS-CORE] Circuit breaker {}: {} -> {}", e.getCircuitBreakerName(),
                    e.getStateTransition().getFromState(), e.getStateTransition().getToState()));
        }
        return cb;
    }
}
