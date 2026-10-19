package com.essenza.sms_gateway.modules.SmsCore.domain.exception;

/**
 * Se lanza cuando llega un envío con una llave de idempotencia ya registrada.
 */
public class DuplicateDispatchException extends RuntimeException {

    private final String idempotencyKey;

    public DuplicateDispatchException(String idempotencyKey) {
        super("Ya existe un envío con Idempotency-Key " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
