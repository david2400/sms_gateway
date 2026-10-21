package com.essenza.sms_gateway.modules.SmsCore.domain.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.exception.DuplicateDispatchException;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchStatus;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RoutePlan;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteStep;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.ProviderAccountQueryPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.RoutingPolicyPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsDispatchLogPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsSenderPort;
import lombok.extern.slf4j.Slf4j;
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
 *   <li>Resuelve la ruta (proveedor, tipo). {@code providerCode == null} = ruta central.</li>
 *   <li>Si el request pide una cuenta ({@link SmsDispatch#getAccountCode()}), esa cuenta va primero y el
 *       resto de la ruta queda como respaldo (sin repetir la cuenta pedida).</li>
 *   <li>Por cada paso de la ruta, en orden: hasta {@code maxAttempts} intentos (3 por defecto)
 *       contra esa cuenta. Al primer éxito (HTTP 2xx) termina.</li>
 *   <li>Si los intentos de una cuenta se agotan, pasa a la siguiente cuenta/proveedor.</li>
 *   <li>Cada intento y el resultado final quedan en el log.</li>
 * </ol>
 * Solo depende de puertos del dominio: no conoce proveedores concretos.
 */
@Slf4j
@Service
public class SmsDispatchService {

    /** Intentos y espera para la cuenta pedida cuando no está en la ruta (mismos defaults de sms_ruta_paso). */
    static final int DEFAULT_MAX_ATTEMPTS = 3;
    static final long DEFAULT_WAIT_MS = 500;

    private final RoutingPolicyPort routingPolicy;
    private final SmsDispatchLogPort dispatchLog;
    private final ProviderAccountQueryPort accountQuery;
    private final Map<String, SmsSenderPort> senders;

    public SmsDispatchService(RoutingPolicyPort routingPolicy,
                              SmsDispatchLogPort dispatchLog,
                              ProviderAccountQueryPort accountQuery,
                              List<SmsSenderPort> senders) {
        this.routingPolicy = routingPolicy;
        this.dispatchLog = dispatchLog;
        this.accountQuery = accountQuery;
        this.senders = senders.stream()
                .collect(Collectors.toUnmodifiableMap(s -> s.providerCode().toUpperCase(), Function.identity()));
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

        RoutePlan plan = routingPolicy.resolve(provider, message.getType());

        String requestedAccount = blankToNull(message.getAccountCode());
        if (requestedAccount != null) {
            Optional<ProviderAccount> account = accountQuery.findActive(provider, requestedAccount);
            if (account.isEmpty()) {
                log.warn("[SMS-CORE] Cuenta solicitada {} no válida para {}", requestedAccount,
                        provider == null ? "CENTRAL" : provider);
                return close(DispatchResult.builder()
                        .envioId(envioId).status(DispatchStatus.FALLIDO)
                        .providerCode(provider)
                        .accountCode(requestedAccount)
                        .errorCode(DispatchResult.ERROR_CUENTA_NO_VALIDA)
                        .detail("La cuenta " + requestedAccount + " no existe, está inactiva"
                                + (provider != null ? " o no pertenece al proveedor " + provider : "")).build(), null);
            }
            plan = withRequestedAccount(plan, account.get());
            log.info("[SMS-CORE] Envío {} usa la cuenta solicitada {}; respaldo: {} cuenta(s) de la ruta",
                    envioId, account.get().getCode(), plan.getSteps().size() - 1);
        }

        if (plan.isEmpty()) {
            log.error("[SMS-CORE] No hay ruta activa para proveedor={} tipo={}",
                    provider == null ? "CENTRAL" : provider, message.getType());
            return close(DispatchResult.builder()
                    .envioId(envioId).status(DispatchStatus.FALLIDO)
                    .providerCode(provider)
                    .errorCode(DispatchResult.ERROR_RUTA_NO_CONFIGURADA)
                    .detail("No hay ruta activa para " + (provider == null ? "CENTRAL" : provider)
                            + "/" + message.getType()).build(), null);
        }

        return execute(envioId, message, plan);
    }

    private DispatchResult execute(UUID envioId, SmsDispatch message, RoutePlan plan) {
        int totalAttempts = 0;
        RouteStep lastStep = null;
        SendOutcome lastOutcome = null;

        for (RouteStep step : plan.getSteps()) {
            lastStep = step;
            String accountCode = step.getAccount().getCode();
            SmsSenderPort sender = senders.get(step.getAccount().getProviderCode().toUpperCase());

            if (sender == null) {
                log.warn("[SMS-CORE] Proveedor {} sin módulo registrado; se salta la cuenta {}",
                        step.getAccount().getProviderCode(), accountCode);
                lastOutcome = SendOutcome.fail(null, "Proveedor sin implementación registrada", 0);
                totalAttempts++;
                recordAttempt(envioId, step, 1, lastOutcome);
                continue;
            }

            int maxAttempts = Math.max(1, step.getMaxAttempts());
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                totalAttempts++;
                lastOutcome = safeSend(sender, message, step);
                recordAttempt(envioId, step, attempt, lastOutcome);

                if (lastOutcome.isSuccess()) {
                    log.info("[SMS-CORE] ✅ Envío {} OK con {} (intento {}/{} de la cuenta, status {})",
                            envioId, accountCode, attempt, maxAttempts, lastOutcome.getHttpStatus());
                    return close(DispatchResult.builder()
                            .envioId(envioId).status(DispatchStatus.ENVIADO)
                            .providerCode(step.getAccount().getProviderCode())
                            .accountCode(accountCode)
                            .httpStatus(lastOutcome.getHttpStatus())
                            .attempts(totalAttempts).build(), step);
                }

                log.warn("[SMS-CORE] ❌ Envío {} falló con {} (intento {}/{}, status {}): {}",
                        envioId, accountCode, attempt, maxAttempts, lastOutcome.getHttpStatus(), lastOutcome.getDetail());

                if (attempt < maxAttempts && !pause(step.getWaitMs())) {
                    return close(failed(envioId, step, lastOutcome, totalAttempts, "Envío interrumpido"), step);
                }
            }
            log.warn("[SMS-CORE] Cuenta {} agotó {} intentos, se pasa a la siguiente de la ruta", accountCode, maxAttempts);
        }

        return close(failed(envioId, lastStep, lastOutcome, totalAttempts, null), lastStep);
    }

    /**
     * Pone la cuenta pedida como primer paso y deja el resto de la ruta como respaldo, sin repetirla.
     * Si la cuenta ya estaba en la ruta conserva sus intentos y espera; si no, usa los valores por defecto.
     * Los pasos se renumeran en el orden en que se ejecutarán (es el {@code paso_orden} del log).
     */
    static RoutePlan withRequestedAccount(RoutePlan plan, ProviderAccount requested) {
        List<RouteStep> routeSteps = plan.getSteps() != null ? plan.getSteps() : List.of();
        RouteStep inRoute = routeSteps.stream()
                .filter(s -> sameAccount(s.getAccount(), requested))
                .findFirst().orElse(null);

        RoutePlan.RoutePlanBuilder builder = RoutePlan.builder()
                .routeId(plan.getRouteId())
                .providerCode(plan.getProviderCode())
                .messageType(plan.getMessageType());
        int order = 1;
        builder.step(RouteStep.builder()
                .order(order++)
                .account(requested)
                .maxAttempts(inRoute != null ? inRoute.getMaxAttempts() : DEFAULT_MAX_ATTEMPTS)
                .waitMs(inRoute != null ? inRoute.getWaitMs() : DEFAULT_WAIT_MS)
                .build());
        for (RouteStep step : routeSteps) {
            if (!sameAccount(step.getAccount(), requested)) {
                builder.step(RouteStep.builder()
                        .order(order++)
                        .account(step.getAccount())
                        .maxAttempts(step.getMaxAttempts())
                        .waitMs(step.getWaitMs())
                        .build());
            }
        }
        return builder.build();
    }

    private static boolean sameAccount(ProviderAccount a, ProviderAccount b) {
        return a != null && b != null && a.getCode() != null && a.getCode().equalsIgnoreCase(b.getCode());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private DispatchResult failed(UUID envioId, RouteStep step, SendOutcome outcome, int attempts, String detail) {
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

    private SendOutcome safeSend(SmsSenderPort sender, SmsDispatch message, RouteStep step) {
        long start = System.currentTimeMillis();
        try {
            SendOutcome outcome = sender.send(message, step.getAccount());
            return outcome != null ? outcome
                    : SendOutcome.fail(null, "El proveedor no devolvió resultado", System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            // El contrato pide no lanzar, pero se protege el motor
            return SendOutcome.fail(null, e.getClass().getSimpleName() + ": " + e.getMessage(),
                    System.currentTimeMillis() - start);
        }
    }

    private void recordAttempt(UUID envioId, RouteStep step, int attemptNumber, SendOutcome outcome) {
        try {
            dispatchLog.attempt(envioId, step, attemptNumber, outcome);
        } catch (RuntimeException e) {
            log.error("[SMS-CORE] No se pudo registrar el intento {} del envío {}: {}", attemptNumber, envioId, e.getMessage());
        }
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
