package com.essenza.sms_gateway.modules.Go4Clients.application.input;

import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.CreateGo4ClientsCampaignRequest;
import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.SendGo4ClientsCampaignMessagesRequest;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignReport;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignResult;

/**
 * Campañas de Go4Clients. No pasan por el motor de SmsCore: sin reintentos,
 * sin cambio de proveedor y sin log de envíos (decisión de negocio).
 */
public interface Go4ClientsCampaignUseCase {

    Go4ClientsCampaignResult create(CreateGo4ClientsCampaignRequest request);

    Go4ClientsCampaignResult sendMessages(String campaignId, SendGo4ClientsCampaignMessagesRequest request);

    Go4ClientsCampaignReport report(String campaignId, String cuenta);
}
