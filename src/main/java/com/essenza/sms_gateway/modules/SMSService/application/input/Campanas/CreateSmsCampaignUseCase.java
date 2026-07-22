package com.essenza.sms_gateway.modules.SMSService.application.input.Campanas;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.CreateSmsCampaignCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsCampaignOperationResult;

/**
 * Use Case: Create a SMS campaign
 */
public interface CreateSmsCampaignUseCase {
    SmsCampaignOperationResult create(CreateSmsCampaignCommand command);
}
