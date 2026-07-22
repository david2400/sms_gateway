package com.essenza.sms_gateway.modules.SMSService.domain.ports;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendInalambriaBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsBatchResult;

/**
 * Port for interacting with Inalambria REST API (OTP/templates/etc).
 */
public interface SmsInalambriaPort {

    SmsBatchResult sendBatch(SendInalambriaBatchCommand command);

    boolean isAvailable();

    String getProviderName();
}
