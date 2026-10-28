package com.essenza.sms_gateway.modules.SmsCore.domain.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteStep;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsDispatchLogPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsSenderPort;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Ejecuta los intentos de UN paso de la ruta (una cuenta) para UN mensaje: hasta {@code maxAttempts},
 * se detiene al primer éxito o ante un error permanente, y registra cada intento en el log.
 * No es un bean: lo crean los servicios del motor para compartir la misma política.
 */
@Slf4j
class AttemptRunner {

    /** Resultado de ejecutar un paso. */
    record StepRun(SendOutcome lastOutcome, int attempts, boolean interrupted) {
        boolean success() {
            return lastOutcome != null && lastOutcome.isSuccess();
        }
    }

    interface Pauser {
        boolean pause(long millis);
    }

    private final SmsDispatchLogPort dispatchLog;
    private final Pauser pauser;
    private final ProveedorCircuitBreakers breakers;

    AttemptRunner(SmsDispatchLogPort dispatchLog, Pauser pauser, ProveedorCircuitBreakers breakers) {
        this.dispatchLog = dispatchLog;
        this.pauser = pauser;
        this.breakers = breakers;
    }

    StepRun run(SmsSenderPort sender, SmsDispatch message, RouteStep step, UUID envioId) {
        String accountCode = step.getAccount().getCode();
        int maxAttempts = Math.max(1, step.getMaxAttempts());
        SendOutcome outcome = null;
        int attempts = 0;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            attempts++;
            outcome = safeSend(sender, message, step);
            record(envioId, step, attempt, outcome);
            if (outcome.isSuccess()) {
                log.info("[SMS-CORE] ✅ Envío {} OK con {} (intento {}/{} de la cuenta, status {})",
                        envioId, accountCode, attempt, maxAttempts, outcome.getHttpStatus());
                return new StepRun(outcome, attempts, false);
            }
            log.warn("[SMS-CORE] ❌ Envío {} falló con {} (intento {}/{}, status {}): {}",
                    envioId, accountCode, attempt, maxAttempts, outcome.getHttpStatus(), outcome.getDetail());
            if (!outcome.isRetryable()) {
                log.warn("[SMS-CORE] Error permanente con {}: no se reintenta con la misma cuenta", accountCode);
                break;
            }
            if (attempt < maxAttempts && !pauser.pause(step.getWaitMs())) {
                return new StepRun(outcome, attempts, true);
            }
        }
        log.warn("[SMS-CORE] Cuenta {} agotó sus intentos, se pasa a la siguiente de la ruta", accountCode);
        return new StepRun(outcome, attempts, false);
    }

    void record(UUID envioId, RouteStep step, int attemptNumber, SendOutcome outcome) {
        try {
            dispatchLog.attempt(envioId, step, attemptNumber, outcome);
        } catch (RuntimeException e) {
            log.error("[SMS-CORE] No se pudo registrar el intento {} del envío {}: {}", attemptNumber, envioId, e.getMessage());
        }
    }

    private SendOutcome safeSend(SmsSenderPort sender, SmsDispatch message, RouteStep step) {
        String provider = step.getAccount().getProviderCode();
        if (!breakers.permite(provider)) {
            log.warn("[SMS-CORE] Circuito abierto para {}: se salta la cuenta {} sin consumir intentos",
                    provider, step.getAccount().getCode());
            return SendOutcome.permanentFail(null,
                    "Proveedor " + provider + " con circuito abierto (demasiados fallos recientes)", 0);
        }
        long start = System.nanoTime();
        SendOutcome outcome;
        try {
            SendOutcome result = sender.send(message, step.getAccount());
            outcome = result != null ? result
                    : SendOutcome.fail(null, "El proveedor no devolvió resultado",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        } catch (RuntimeException e) {
            // El contrato pide no lanzar, pero se protege el motor
            outcome = SendOutcome.fail(null, e.getClass().getSimpleName() + ": " + e.getMessage(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        }
        breakers.registrar(provider, outcome, System.nanoTime() - start);
        return outcome;
    }
}
