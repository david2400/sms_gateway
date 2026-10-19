package com.essenza.sms_gateway.modules.Go4Clients.application.services;

import com.essenza.sms_gateway.modules.Go4Clients.application.input.Go4ClientsCampaignUseCase;
import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.CreateGo4ClientsCampaignRequest;
import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.SendGo4ClientsCampaignMessagesRequest;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignReport;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignResult;
import com.essenza.sms_gateway.modules.Go4Clients.domain.ports.Go4ClientsCampaignPort;
import com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.providers.Go4ClientsSmsSender;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.ProviderAccountQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orquesta las campañas de Go4Clients tomando la cuenta desde BD ({@code sms_cuenta}).
 */
@Service
@RequiredArgsConstructor
public class Go4ClientsCampaignService implements Go4ClientsCampaignUseCase {

    private final ProviderAccountQueryPort accountQuery;
    private final Go4ClientsCampaignPort campaignPort;

    @Override
    public Go4ClientsCampaignResult create(CreateGo4ClientsCampaignRequest request) {
        return account(request.getCuenta())
                .map(acc -> campaignPort.createCampaign(acc, request.getName(), request.getSender()))
                .orElseGet(() -> noAccount(request.getCuenta()));
    }

    @Override
    public Go4ClientsCampaignResult sendMessages(String campaignId, SendGo4ClientsCampaignMessagesRequest request) {
        return account(request.getCuenta())
                .map(acc -> campaignPort.sendMessages(acc, campaignId, request.getMessage(), request.getDestinations(),
                        request.getPriority(), request.getLandingCustomFields()))
                .orElseGet(() -> noAccount(request.getCuenta()));
    }

    @Override
    public Go4ClientsCampaignReport report(String campaignId, String cuenta) {
        return account(cuenta)
                .map(acc -> campaignPort.getReport(acc, campaignId))
                .orElseGet(() -> Go4ClientsCampaignReport.builder().campaignId(campaignId)
                        .errorMessage(noAccount(cuenta).getErrorMessage()).build());
    }

    private Optional<ProviderAccount> account(String cuenta) {
        return cuenta == null || cuenta.isBlank()
                ? accountQuery.findFirstActive(Go4ClientsSmsSender.PROVIDER_CODE)
                : accountQuery.findActive(Go4ClientsSmsSender.PROVIDER_CODE, cuenta.trim());
    }

    private static Go4ClientsCampaignResult noAccount(String cuenta) {
        return Go4ClientsCampaignResult.failure(cuenta, null, Go4ClientsCampaignResult.ERROR_SIN_CUENTA,
                cuenta == null || cuenta.isBlank()
                        ? "No hay cuentas activas de Go4Clients en sms_cuenta"
                        : "La cuenta " + cuenta + " no existe, está inactiva o no es de Go4Clients");
    }
}
