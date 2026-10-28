package com.essenza.sms_gateway.modules.SmsCore.domain.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchStatus;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.PlanResolution;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteStep;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsBatchSenderPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsDispatchLogPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsSenderPort;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Motor de envío MASIVO: un mismo tipo de mensaje para muchos destinatarios (texto igual o personalizado).
 * <p>
 * Comparte las reglas del envío individual (plan con {@link DispatchPlanner}: servicios por proveedor,
 * proveedor preferido, cuenta pedida; fallback solo entre proveedores que soportan el mismo tipo) y agrega:
 * <ul>
 *   <li>Si el proveedor del paso tiene API masiva ({@link SmsBatchSenderPort}) los pendientes se envían en lotes
 *       de {@code maxBatchSize}; cada lote tiene sus propios reintentos y, si los agota, pasa completo al siguiente paso.</li>
 *   <li>Si no la tiene, se envía uno por uno con {@link SmsSenderPort} (mismos reintentos que el individual).</li>
 *   <li>Cada destinatario queda como un envío independiente en el log (consultable por su envioId).</li>
 * </ul>
 */
@Slf4j
@Service
public class SmsBatchDispatchService {

    private final DispatchPlanner planner;
    private final SmsDispatchLogPort dispatchLog;
    private final Map<String, SmsSenderPort> senders;
    private final Map<String, SmsBatchSenderPort> batchSenders;
    private final AttemptRunner runner;
    private final ProveedorCircuitBreakers breakers;

    /** Sin circuit breaker (tests y usos fuera de Spring). */
    public SmsBatchDispatchService(DispatchPlanner planner,
                            SmsDispatchLogPort dispatchLog,
                            List<SmsSenderPort> senders,
                            List<SmsBatchSenderPort> batchSenders) {
        this(planner, dispatchLog, senders, batchSenders, null);
    }

    @Autowired
    public SmsBatchDispatchService(DispatchPlanner planner,
                                   SmsDispatchLogPort dispatchLog,
                                   List<SmsSenderPort> senders,
                                   List<SmsBatchSenderPort> batchSenders,
                                   CircuitBreakerRegistry circuitBreakers) {
        this.planner = planner;
        this.dispatchLog = dispatchLog;
        this.senders = senders.stream()
                .collect(Collectors.toUnmodifiableMap(s -> s.providerCode().toUpperCase(), Function.identity()));
        this.batchSenders = batchSenders.stream()
                .collect(Collectors.toUnmodifiableMap(s -> s.providerCode().toUpperCase(), Function.identity()));
        this.breakers = new ProveedorCircuitBreakers(circuitBreakers);
        this.runner = new AttemptRunner(dispatchLog, this::pause, this.breakers);
        log.info("[SMS-CORE] Proveedores con envío masivo nativo: {}", this.batchSenders.keySet());
    }

    /**
     * @param messages          mensajes ya resueltos (texto final por destinatario), todos del tipo {@code type}
     * @param channel           CENTRAL o PROVEEDOR
     * @param providerCode      proveedor del canal PROVEEDOR o {@code null}
     * @param preferredProvider proveedor preferido (canal CENTRAL), opcional
     * @param accountCode       cuenta pedida, opcional
     * @param batchId           id del lote (se envía al proveedor y vuelve en la respuesta)
     * @return un resultado por mensaje, en el mismo orden
     */
    public List<DispatchResult> dispatch(List<SmsDispatch> messages, MessageType type, DispatchChannel channel,
                                         String providerCode, String preferredProvider, String accountCode, String batchId) {
        int n = messages.size();
        String provider = providerCode != null ? providerCode.toUpperCase() : null;
        DispatchResult[] results = new DispatchResult[n];
        UUID[] ids = startAll(messages, channel, provider);

        List<Integer> pending = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (messages.get(i).isValid() && messages.get(i).getType() == type) {
                pending.add(i);
            } else {
                results[i] = close(DispatchResult.builder().envioId(ids[i]).status(DispatchStatus.FALLIDO)
                        .errorCode(DispatchResult.ERROR_VALIDACION).detail("Destino, mensaje o tipo inválidos").build(), null);
            }
        }
        if (pending.isEmpty()) {
            return Arrays.asList(results);
        }

        PlanResolution resolution = planner.plan(type, provider,
                channel == DispatchChannel.CENTRAL ? preferredProvider : null, accountCode);
        if (!resolution.isOk()) {
            List<DispatchResult> failed = new ArrayList<>();
            for (int i : pending) {
                results[i] = DispatchResult.builder().envioId(ids[i]).status(DispatchStatus.FALLIDO)
                        .providerCode(resolution.getProviderCode()).accountCode(resolution.getAccountCode())
                        .errorCode(resolution.getErrorCode()).detail(resolution.getDetail()).build();
                failed.add(results[i]);
            }
            closeAll(failed, null);
            return Arrays.asList(results);
        }

        int[] attempts = new int[n];
        SendOutcome[] lastOutcome = new SendOutcome[n];
        RouteStep[] lastStep = new RouteStep[n];

        for (RouteStep step : resolution.getPlan().getSteps()) {
            if (pending.isEmpty()) {
                break;
            }
            String stepProvider = step.getAccount().getProviderCode().toUpperCase();
            SmsBatchSenderPort batchSender = batchSenders.get(stepProvider);
            SmsSenderPort sender = senders.get(stepProvider);

            if (batchSender != null && batchSender.supports(type)) {
                pending = sendInChunks(batchSender, step, messages, ids, pending, results, attempts, lastOutcome, lastStep, batchId);
            } else if (sender != null) {
                pending = sendOneByOne(sender, step, messages, ids, pending, results, attempts, lastOutcome, lastStep);
            } else {
                log.warn("[SMS-CORE] Proveedor {} sin módulo registrado; se salta la cuenta {} en el masivo {}",
                        stepProvider, step.getAccount().getCode(), batchId);
            }
        }

        // Los que quedaron pendientes fallaron en todas las cuentas
        for (int i : pending) {
            results[i] = close(SmsDispatchService.failed(ids[i], lastStep[i], lastOutcome[i], attempts[i],
                    lastStep[i] == null ? "Ningún proveedor de la ruta tiene módulo registrado" : null), lastStep[i]);
        }
        long ok = Arrays.stream(results).filter(DispatchResult::isSent).count();
        log.info("[SMS-CORE] Masivo {} tipo {}: {} enviados, {} fallidos", batchId, type, ok, n - ok);
        return Arrays.asList(results);
    }

    private List<Integer> sendInChunks(SmsBatchSenderPort batchSender, RouteStep step, List<SmsDispatch> messages,
                                       UUID[] ids, List<Integer> pending, DispatchResult[] results, int[] attempts,
                                       SendOutcome[] lastOutcome, RouteStep[] lastStep, String batchId) {
        int size = Math.max(1, batchSender.maxBatchSize(step.getAccount()));
        int maxAttempts = Math.max(1, step.getMaxAttempts());
        List<Integer> stillPending = new ArrayList<>();
        int chunkNumber = 0;

        for (int from = 0; from < pending.size(); from += size) {
            List<Integer> chunk = pending.subList(from, Math.min(from + size, pending.size()));
            List<SmsDispatch> chunkMessages = chunk.stream().map(messages::get).toList();
            List<UUID> chunkIds = chunk.stream().map(i -> ids[i]).toList();
            String chunkId = batchId + "-" + (++chunkNumber);
            SendOutcome outcome = null;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                outcome = safeSendBatch(batchSender, chunkMessages, step, chunkId);
                for (int i : chunk) {
                    attempts[i]++;
                    lastOutcome[i] = outcome;
                    lastStep[i] = step;
                }
                recordAll(chunkIds, step, attempt, outcome);
                if (outcome.isSuccess()) {
                    log.info("[SMS-CORE] ✅ Lote {} ({} SMS) OK con {} (intento {}/{}, status {})",
                            chunkId, chunk.size(), step.getAccount().getCode(), attempt, maxAttempts, outcome.getHttpStatus());
                    break;
                }
                log.warn("[SMS-CORE] ❌ Lote {} ({} SMS) falló con {} (intento {}/{}, status {}): {}",
                        chunkId, chunk.size(), step.getAccount().getCode(), attempt, maxAttempts,
                        outcome.getHttpStatus(), outcome.getDetail());
                if (!outcome.isRetryable() || (attempt < maxAttempts && !pause(step.getWaitMs()))) {
                    break;
                }
            }

            if (outcome != null && outcome.isSuccess()) {
                List<DispatchResult> sent = new ArrayList<>();
                for (int i : chunk) {
                    results[i] = DispatchResult.builder().envioId(ids[i]).status(DispatchStatus.ENVIADO)
                            .providerCode(step.getAccount().getProviderCode()).accountCode(step.getAccount().getCode())
                            .httpStatus(outcome.getHttpStatus()).attempts(attempts[i]).build();
                    sent.add(results[i]);
                }
                closeAll(sent, step);
            } else {
                stillPending.addAll(chunk);
            }
        }
        return stillPending;
    }

    private List<Integer> sendOneByOne(SmsSenderPort sender, RouteStep step, List<SmsDispatch> messages, UUID[] ids,
                                       List<Integer> pending, DispatchResult[] results, int[] attempts,
                                       SendOutcome[] lastOutcome, RouteStep[] lastStep) {
        List<Integer> stillPending = new ArrayList<>();
        for (int i : pending) {
            AttemptRunner.StepRun run = runner.run(sender, messages.get(i), step, ids[i]);
            attempts[i] += run.attempts();
            lastOutcome[i] = run.lastOutcome();
            lastStep[i] = step;
            if (run.success()) {
                results[i] = close(DispatchResult.builder().envioId(ids[i]).status(DispatchStatus.ENVIADO)
                        .providerCode(step.getAccount().getProviderCode()).accountCode(step.getAccount().getCode())
                        .httpStatus(run.lastOutcome().getHttpStatus()).attempts(attempts[i]).build(), step);
            } else {
                stillPending.add(i);
            }
        }
        return stillPending;
    }

    private SendOutcome safeSendBatch(SmsBatchSenderPort sender, List<SmsDispatch> chunk, RouteStep step, String chunkId) {
        String provider = step.getAccount().getProviderCode();
        if (!breakers.permite(provider)) {
            log.warn("[SMS-CORE] Circuito abierto para {}: se salta el lote {} sin consumir intentos", provider, chunkId);
            return SendOutcome.permanentFail(null,
                    "Proveedor " + provider + " con circuito abierto (demasiados fallos recientes)", 0);
        }
        long start = System.nanoTime();
        SendOutcome outcome;
        try {
            SendOutcome result = sender.sendBatch(chunk, step.getAccount(), chunkId);
            outcome = result != null ? result
                    : SendOutcome.fail(null, "El proveedor no devolvió resultado",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        } catch (RuntimeException e) {
            outcome = SendOutcome.fail(null, e.getClass().getSimpleName() + ": " + e.getMessage(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        }
        breakers.registrar(provider, outcome, System.nanoTime() - start);
        return outcome;
    }

    private UUID[] startAll(List<SmsDispatch> messages, DispatchChannel channel, String provider) {
        UUID[] ids = new UUID[messages.size()];
        try {
            List<UUID> started = dispatchLog.startAll(messages, channel, provider);
            for (int i = 0; i < ids.length; i++) {
                ids[i] = started.get(i);
            }
        } catch (RuntimeException e) {
            log.error("[SMS-CORE] No se pudo registrar el masivo en el log: {}", e.getMessage());
            for (int i = 0; i < ids.length; i++) {
                ids[i] = messages.get(i).getId();
            }
        }
        return ids;
    }

    private void recordAll(List<UUID> ids, RouteStep step, int attempt, SendOutcome outcome) {
        try {
            dispatchLog.attemptAll(ids, step, attempt, outcome);
        } catch (RuntimeException e) {
            log.error("[SMS-CORE] No se pudo registrar el intento {} del lote: {}", attempt, e.getMessage());
        }
    }

    private DispatchResult close(DispatchResult result, RouteStep step) {
        try {
            dispatchLog.finish(result, step);
        } catch (RuntimeException e) {
            log.error("[SMS-CORE] No se pudo cerrar el envío {} en el log: {}", result.getEnvioId(), e.getMessage());
        }
        return result;
    }

    private void closeAll(List<DispatchResult> list, RouteStep step) {
        try {
            dispatchLog.finishAll(list, step);
        } catch (RuntimeException e) {
            log.error("[SMS-CORE] No se pudo cerrar {} envíos en el log: {}", list.size(), e.getMessage());
        }
    }

    /** Espera entre intentos. Protegido para sustituirlo en pruebas. */
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
