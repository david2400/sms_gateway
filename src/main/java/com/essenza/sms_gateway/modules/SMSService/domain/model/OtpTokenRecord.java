package com.essenza.sms_gateway.modules.SMSService.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Represents an OTP token persisted for validation purposes.
 */
@Value
@Builder(toBuilder = true)
public class OtpTokenRecord {

    String documentNumber;
    String token;
    String phoneNumber;
    LocalDateTime expiresAt;
}
