package com.essenza.sms_gateway.modules.SmsAdmin.domain.exception;

import java.util.List;

/** Datos inválidos (400). {@code detalles} lista los errores puntuales (ej. tipos de mensaje inexistentes). */
public class AdminValidationException extends RuntimeException {

    private final List<String> detalles;

    public AdminValidationException(String message) {
        this(message, List.of());
    }

    public AdminValidationException(String message, List<String> detalles) {
        super(message);
        this.detalles = detalles == null ? List.of() : List.copyOf(detalles);
    }

    public List<String> getDetalles() {
        return detalles;
    }
}
