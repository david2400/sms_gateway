package com.essenza.sms_gateway.modules.SMSService.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Resultado de la operación de generación/envío de token OTP.
 */
@Value
@Builder(toBuilder = true)
public class OtpTokenResult {

    boolean success;
    String token;
    String transactionNumber;
    String errorMessage;
    String errorCode;
    LocalDateTime timestamp;

    public static OtpTokenResult success(String token, String transactionNumber) {
        return OtpTokenResult.builder()
                .success(true)
                .token(token)
                .transactionNumber(transactionNumber)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static OtpTokenResult failure(String message, String code) {
        return OtpTokenResult.builder()
                .success(false)
                .errorMessage(message)
                .errorCode(code)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
