package com.essenza.sms_gateway.modules.SMSService.application.input.Campanas;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.SmsCampaignResultQuery;

/**
 * Use Case: Get SMS campaign results
 */
public interface GetSmsCampaignResultsUseCase {
    SmsCampaignResultQuery getResults(String campaignId);
}
