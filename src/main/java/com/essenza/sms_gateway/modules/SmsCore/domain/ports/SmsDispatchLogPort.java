package com.essenza.sms_gateway.modules.SmsCore.domain.ports;

import com.essenza.sms_gateway.modules.SmsCore.domain.exception.DuplicateDispatchException;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteStep;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Log persistente de envíos ({@code sms_envio}) e intentos ({@code sms_envio_intento}).
 * Cada operación se confirma de inmediato (transacción corta), para no mantener
 * una transacción abierta mientras se llama al proveedor.
 */
public interface SmsDispatchLogPort {

    /**
     * Registra el envío en estado EN_PROCESO.
     *
     * @throws DuplicateDispatchException si ya existe un envío con la misma llave de idempotencia
     */
    UUID start(SmsDispatch message, DispatchChannel channel, String requestedProvider);

    /**
     * Registra un intento contra una cuenta.
     */
    void attempt(UUID envioId, RouteStep step, int attemptNumber, SendOutcome outcome);

    /**
     * Cierra el envío con su resultado final.
     */
    void finish(DispatchResult result, RouteStep lastStep);

    /**
     * Busca el resultado de un envío previo por su llave de idempotencia.
     */
    Optional<DispatchResult> findByIdempotencyKey(String idempotencyKey);

    // ---------------- Envío masivo (los adapters pueden optimizar con una sola transacción) ----------------

    /** Registra varios envíos en EN_PROCESO; devuelve los ids en el mismo orden. */
    default List<UUID> startAll(List<SmsDispatch> messages, DispatchChannel channel, String requestedProvider) {
        List<UUID> ids = new ArrayList<>(messages.size());
        for (SmsDispatch m : messages) {
            ids.add(start(m, channel, requestedProvider));
        }
        return ids;
    }

    /** Registra el mismo intento (un lote) para varios envíos. */
    default void attemptAll(List<UUID> envioIds, RouteStep step, int attemptNumber, SendOutcome outcome) {
        for (UUID id : envioIds) {
            attempt(id, step, attemptNumber, outcome);
        }
    }

    /** Cierra varios envíos que terminaron en el mismo paso. */
    default void finishAll(List<DispatchResult> results, RouteStep lastStep) {
        for (DispatchResult r : results) {
            finish(r, lastStep);
        }
    }
}
