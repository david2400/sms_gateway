package com.essenza.sms_gateway.modules.SmsAdmin.domain.exception;

/** Datos inválidos (400). */
public class AdminValidationException extends RuntimeException {
    public AdminValidationException(String message) {
        super(message);
    }
}
