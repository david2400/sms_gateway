package com.essenza.sms_gateway.modules.Go4Clients.domain.ports;

import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.Go4ClientsDestinatarioDto;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignReport;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;

import java.util.List;
import java.util.Map;

/**
 * API de campañas de Go4Clients. Cada método hace una sola llamada HTTP.
 */
public interface Go4ClientsCampaignPort {

    Go4ClientsCampaignResult createCampaign(ProviderAccount account, String name, String sender);

    Go4ClientsCampaignResult sendMessages(ProviderAccount account, String campaignId, String message,
                                          List<Go4ClientsDestinatarioDto> destinations, String priority,
                                          Map<String, Map<String, String>> landingCustomFields);

    Go4ClientsCampaignReport getReport(ProviderAccount account, String campaignId);
}
