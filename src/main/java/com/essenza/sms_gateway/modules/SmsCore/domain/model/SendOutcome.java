package com.essenza.sms_gateway.modules.SmsCore.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Resultado de UN intento contra un proveedor.
 * Por ahora el éxito se decide solo por HTTP 2xx; no se interpreta el body.
 */
@Value
@Builder
public class SendOutcome {

    boolean success;
    /** HTTP status devuelto por el proveedor; {@code null} si no hubo respuesta (timeout, red). */
    Integer httpStatus;
    /** Detalle del error (mensaje de excepción o body truncado). */
    String detail;
    long durationMs;
    /**
     * {@code false} = error permanente (credenciales, payload rechazado, número inválido): reintentar con la
     * MISMA cuenta no sirve y el motor pasa de inmediato al siguiente paso de la ruta.
     */
    @Builder.Default
    boolean retryable = true;

    public static SendOutcome ok(int httpStatus, long durationMs) {
        return SendOutcome.builder().success(true).httpStatus(httpStatus).durationMs(durationMs).build();
    }

    public static SendOutcome fail(Integer httpStatus, String detail, long durationMs) {
        return SendOutcome.builder().success(false).httpStatus(httpStatus).detail(detail).durationMs(durationMs).build();
    }

    /** Fallo que no se corrige reintentando con la misma cuenta (ej. 401, 403, payload inválido). */
    public static SendOutcome permanentFail(Integer httpStatus, String detail, long durationMs) {
        return SendOutcome.builder().success(false).retryable(false).httpStatus(httpStatus).detail(detail)
                .durationMs(durationMs).build();
    }
}
