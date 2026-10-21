package com.essenza.sms_gateway.modules.SmsCore.application.input;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.otp.OtpSendResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.otp.SendOtpRequest;

/**
 * Envío de OTP (con o sin actualización de PIN) sobre el motor de SmsCore, ruta CENTRAL tipo OTP.
 */
public interface SendOtpUseCase {

    OtpSendResult send(SendOtpRequest request, String idempotencyKey);

    /**
     * @param cuenta código de cuenta a usar primero (opcional, {@code null} = solo la ruta OTP)
     */
    OtpSendResult sendAutoGenerate(String phoneNumber, String message, Integer expirationMinutes, String cuenta);

    /**
     * @param cuenta código de cuenta a usar primero (opcional, {@code null} = solo la ruta OTP)
     */
    OtpSendResult sendWithDefaultMessage(String phoneNumber, String cuenta);
}
