package com.essenza.sms_gateway.modules.SMSService.application.input.Sms;

/**
 * Use Case: Check SMS service availability
 * Interface for checking SMS service status and provider information
 */
public interface SmsServiceAvailabilityUseCase {
    boolean isServiceAvailable();
    String getProviderName();
}
