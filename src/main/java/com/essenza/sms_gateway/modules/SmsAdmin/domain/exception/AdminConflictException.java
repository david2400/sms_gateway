package com.essenza.sms_gateway.modules.SmsAdmin.domain.exception;

/** Conflicto con datos existentes (409). */
public class AdminConflictException extends RuntimeException {
    public AdminConflictException(String message) {
        super(message);
    }
}
