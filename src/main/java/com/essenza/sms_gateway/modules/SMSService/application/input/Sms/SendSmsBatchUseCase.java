package com.essenza.sms_gateway.modules.SMSService.application.input.Sms;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSmsBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsBatchResult;

/**
 * Use Case: Send SMS batch
 */
public interface SendSmsBatchUseCase {
    SmsBatchResult send(SendSmsBatchCommand command);
}
