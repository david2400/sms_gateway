package com.essenza.sms_gateway.modules.SmsCore.domain.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.exception.DuplicateDispatchException;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchStatus;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.PlanResolution;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RoutePlan;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteStep;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsDispatchLogPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsSenderPort;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Motor de envío de SMS: reintentos por cuenta + fallback según la ruta en BD.
 * <p>
 * Algoritmo:
 * <ol>
 *   <li>Valida el mensaje (si es inválido no gasta intentos).</li>
 *   <li>Planea la ruta con {@link DispatchPlanner}: servicios habilitados por proveedor, proveedor preferido,
 *       cuenta solicitada y fallback solo entre proveedores que soportan el mismo tipo de mensaje.</li>
 *   <li>Por cada paso de la ruta, en orden: hasta {@code maxAttempts} intentos (3 por defecto)
 *       contra esa cuenta. Al primer éxito (HTTP 2xx) termina. Un error permanente pasa de inmediato al siguiente paso.</li>
 *   <li>Cada intento y el resultado final quedan en el log.</li>
 * </ol>
 * Solo depende de puertos del dominio: no conoce proveedores concretos.
 */
@Slf4j
@Service
public class SmsDispatchService {

    /** Intentos y espera para la cuenta pedida cuando no está en la ruta (mismos defaults de sms_ruta_paso). */
    static final int DEFAULT_MAX_ATTEMPTS = DispatchPlanner.DEFAULT_MAX_ATTEMPTS;
    static final long DEFAULT_WAIT_MS = DispatchPlanner.DEFAULT_WAIT_MS;

    private final DispatchPlanner planner;
    private final SmsDispatchLogPort dispatchLog;
    private final Map<String, SmsSenderPort> senders;
    private final AttemptRunner runner;

    /** Sin circuit breaker (tests y usos fuera de Spring). */
    public SmsDispatchService(DispatchPlanner planner,
                       SmsDispatchLogPort dispatchLog,
                       List<SmsSenderPort> senders) {
        this(planner, dispatchLog, senders, null);
    }

    @Autowired
    public SmsDispatchService(DispatchPlanner planner,
                              SmsDispatchLogPort dispatchLog,
                              List<SmsSenderPort> senders,
                              CircuitBreakerRegistry circuitBreakers) {
        this.planner = planner;
        this.dispatchLog = dispatchLog;
        this.senders = senders.stream()
                .collect(Collectors.toUnmodifiableMap(s -> s.providerCode().toUpperCase(), Function.identity()));
        this.runner = new AttemptRunner(dispatchLog, this::pause, new ProveedorCircuitBreakers(circuitBreakers));
        log.info("[SMS-CORE] Proveedores registrados: {}", this.senders.keySet());
    }

    /**
     * Envía un SMS.
     *
     * @param message      mensaje canónico
     * @param channel      canal de entrada (CENTRAL o PROVEEDOR)
     * @param providerCode proveedor fijo (canal PROVEEDOR) o {@code null} (canal CENTRAL)
     */
    public DispatchResult dispatch(SmsDispatch message, DispatchChannel channel, String providerCode) {
        String provider = providerCode != null ? providerCode.toUpperCase() : null;

        // Idempotencia: si el cliente repite el request, se devuelve el resultado anterior
        Optional<DispatchResult> previous = findPrevious(message);
        if (previous.isPresent()) {
            log.info("[SMS-CORE] Idempotency-Key {} repetida, se devuelve envío {}",
                    message.getIdempotencyKey(), previous.get().getEnvioId());
            return previous.get();
        }

        UUID envioId;
        try {
            envioId = dispatchLog.start(message, channel, provider);
        } catch (DuplicateDispatchException e) {
            return findPrevious(message).orElseThrow(() -> e);
        } catch (RuntimeException e) {
            // El log no debe impedir el envío
            log.error("[SMS-CORE] No se pudo registrar el envío {} en el log: {}", message.getId(), e.getMessage());
            envioId = message.getId();
        }

        if (!message.isValid()) {
            return close(DispatchResult.builder()
                    .envioId(envioId).status(DispatchStatus.FALLIDO)
                    .errorCode(DispatchResult.ERROR_VALIDACION)
                    .detail("Destino, mensaje o tipo inválidos").build(), null);
        }

        PlanResolution resolution = planner.plan(message.getType(), provider,
                channel == DispatchChannel.CENTRAL ? message.getPreferredProvider() : null,
                message.getAccountCode());
        if (!resolution.isOk()) {
            return close(DispatchResult.builder()
                    .envioId(envioId).status(DispatchStatus.FALLIDO)
                    .providerCode(resolution.getProviderCode())
                    .accountCode(resolution.getAccountCode())
                    .errorCode(resolution.getErrorCode())
                    .detail(resolution.getDetail()).build(), null);
        }
        return execute(envioId, message, resolution.getPlan());
    }

    private DispatchResult execute(UUID envioId, SmsDispatch message, RoutePlan plan) {
        int totalAttempts = 0;
        RouteStep lastStep = null;
        SendOutcome lastOutcome = null;

        for (RouteStep step : plan.getSteps()) {
            lastStep = step;
            SmsSenderPort sender = senders.get(step.getAccount().getProviderCode().toUpperCase());

            if (sender == null) {
                log.warn("[SMS-CORE] Proveedor {} sin módulo registrado; se salta la cuenta {}",
                        step.getAccount().getProviderCode(), step.getAccount().getCode());
                lastOutcome = SendOutcome.fail(null, "Proveedor sin implementación registrada", 0);
                totalAttempts++;
                runner.record(envioId, step, 1, lastOutcome);
                continue;
            }

            AttemptRunner.StepRun run = runner.run(sender, message, step, envioId);
            totalAttempts += run.attempts();
            lastOutcome = run.lastOutcome();
            if (run.success()) {
                return close(DispatchResult.builder()
                        .envioId(envioId).status(DispatchStatus.ENVIADO)
                        .providerCode(step.getAccount().getProviderCode())
                        .accountCode(step.getAccount().getCode())
                        .httpStatus(lastOutcome.getHttpStatus())
                        .attempts(totalAttempts).build(), step);
            }
            if (run.interrupted()) {
                return close(failed(envioId, step, lastOutcome, totalAttempts, "Envío interrumpido"), step);
            }
        }

        return close(failed(envioId, lastStep, lastOutcome, totalAttempts, null), lastStep);
    }

    /** Compatibilidad: ver {@link DispatchPlanner#withRequestedAccount}. */
    static RoutePlan withRequestedAccount(RoutePlan plan, com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount requested) {
        return DispatchPlanner.withRequestedAccount(plan, requested);
    }

    static DispatchResult failed(UUID envioId, RouteStep step, SendOutcome outcome, int attempts, String detail) {
        return DispatchResult.builder()
                .envioId(envioId).status(DispatchStatus.FALLIDO)
                .providerCode(step != null ? step.getAccount().getProviderCode() : null)
                .accountCode(step != null ? step.getAccount().getCode() : null)
                .httpStatus(outcome != null ? outcome.getHttpStatus() : null)
                .attempts(attempts)
                .errorCode(DispatchResult.ERROR_TODAS_FALLARON)
                .detail(detail != null ? detail : (outcome != null ? outcome.getDetail() : null))
                .build();
    }

    private DispatchResult close(DispatchResult result, RouteStep lastStep) {
        try {
            dispatchLog.finish(result, lastStep);
        } catch (RuntimeException e) {
            log.error("[SMS-CORE] No se pudo cerrar el envío {} en el log: {}", result.getEnvioId(), e.getMessage());
        }
        return result;
    }

    private Optional<DispatchResult> findPrevious(SmsDispatch message) {
        if (message.getIdempotencyKey() == null || message.getIdempotencyKey().isBlank()) {
            return Optional.empty();
        }
        try {
            return dispatchLog.findByIdempotencyKey(message.getIdempotencyKey());
        } catch (RuntimeException e) {
            log.error("[SMS-CORE] No se pudo consultar la Idempotency-Key: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Espera entre intentos. Protegido para poder sustituirlo en pruebas.
     *
     * @return {@code false} si el hilo fue interrumpido
     */
    protected boolean pause(long millis) {
        if (millis <= 0) {
            return true;
        }
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
