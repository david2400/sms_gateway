package com.essenza.sms_gateway.modules.SMSService.application.input.Otp;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.OtpSendResult;

/**
 * Use Case: Send OTP with default message
 */
public interface SendOtpDefaultMessageUseCase {
    OtpSendResult sendWithDefaultMessage(String phoneNumber);
}
