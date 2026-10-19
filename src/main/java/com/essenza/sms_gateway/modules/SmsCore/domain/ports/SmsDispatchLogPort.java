package com.essenza.sms_gateway.modules.SmsCore.domain.ports;

import com.essenza.sms_gateway.modules.SmsCore.domain.exception.DuplicateDispatchException;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteStep;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;

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
}
