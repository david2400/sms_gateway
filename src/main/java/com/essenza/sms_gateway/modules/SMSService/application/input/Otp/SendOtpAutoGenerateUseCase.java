package com.essenza.sms_gateway.modules.SMSService.application.input.Otp;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.OtpSendResult;

/**
 * Use Case: Send OTP with auto-generated code
 */
public interface SendOtpAutoGenerateUseCase {
    OtpSendResult sendAutoGenerate(String phoneNumber, String message, Integer expirationMinutes);
}
