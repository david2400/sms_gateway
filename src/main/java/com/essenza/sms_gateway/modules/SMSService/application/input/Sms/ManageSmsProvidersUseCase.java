package com.essenza.sms_gateway.modules.SMSService.application.input.Sms;

import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsProvider;

import java.util.Map;

/**
 * Use Case: Manage SMS providers
 */
public interface ManageSmsProvidersUseCase {
    String getCurrentProvider();
    void switchProvider(SmsProvider provider);
    Map<SmsProvider, Boolean> checkProvidersAvailability();
    SmsProvider getBestAvailableProvider();
    String getProviderInfo();
    boolean isCurrentProviderAvailable();
    String getCurrentProviderInfo();
}
