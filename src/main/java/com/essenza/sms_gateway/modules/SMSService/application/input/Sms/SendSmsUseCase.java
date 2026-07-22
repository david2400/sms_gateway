package com.essenza.sms_gateway.modules.SMSService.application.input.Sms;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSMSCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SMSResult;

/**
 * Use Case: Send SMS
 * Interface for sending individual SMS messages
 */
public interface SendSmsUseCase {
    SMSResult send(SendSMSCommand command);
}
