package com.essenza.sms_gateway.modules.SMSService.application.input.Sms;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SmsTypeRequest;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SMSResult;

/**
 * Use Case: Send SMS with type
 * Interface for sending SMS messages with specific type
 */
public interface SendSmsWithTypeUseCase {
    SMSResult sendWithType(SmsTypeRequest request);
}
