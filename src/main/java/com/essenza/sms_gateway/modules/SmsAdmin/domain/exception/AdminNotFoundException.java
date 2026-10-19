package com.essenza.sms_gateway.modules.SmsAdmin.domain.exception;

/** Recurso no encontrado (404). */
public class AdminNotFoundException extends RuntimeException {
    public AdminNotFoundException(String message) {
        super(message);
    }
}
