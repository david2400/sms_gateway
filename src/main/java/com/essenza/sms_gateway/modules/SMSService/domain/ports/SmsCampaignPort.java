package com.essenza.sms_gateway.modules.SMSService.domain.ports;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.CreateSmsCampaignCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSmsBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.SmsCampaignResultQuery;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsBatchResult;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsCampaignOperationResult;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsCampaignReport;

/**
 * Port for Go4Clients campaign operations.
 */
public interface SmsCampaignPort {

    SmsCampaignOperationResult createCampaign(CreateSmsCampaignCommand command);

    SmsBatchResult sendBatch(SendSmsBatchCommand command);

    SmsCampaignReport getCampaignReport(SmsCampaignResultQuery query);

    boolean isAvailable();

    String getProviderName();
}
