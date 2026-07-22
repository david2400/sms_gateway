package com.essenza.sms_gateway.modules.SMSService.application.services;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.CreateSmsCampaignCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.SmsCampaignResultQuery;
import com.essenza.sms_gateway.modules.SMSService.application.input.Campanas.CreateSmsCampaignUseCase;
import com.essenza.sms_gateway.modules.SMSService.application.input.Campanas.GetSmsCampaignResultsUseCase;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsCampaignOperationResult;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.routing.SmsProviderRouter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SmsCampaniasService implements
        CreateSmsCampaignUseCase,
        GetSmsCampaignResultsUseCase {


    private static final String SMS_SERVICE = "smsService";

    private final SmsProviderRouter providerRouter;

    @Override
    @CircuitBreaker(name = SMS_SERVICE, fallbackMethod = "fallbackCampaignResult")
    @Retry(name = SMS_SERVICE)
    public SmsCampaignOperationResult create(CreateSmsCampaignCommand command) {
        log.info("[SMS] Creando campaña {}", command.getName());

        if (command == null || !command.isValid()) {
            return SmsCampaignOperationResult.failure("Comando inválido", "VALIDATION_ERROR");
        }

        // Usar router con failover automático
        return providerRouter.execute(
                provider -> {
                    var result = provider.createCampaign(command);
                    return result;
                },
                errorMsg -> SmsCampaignOperationResult.failure(errorMsg, "ROUTER_ERROR")
        );
    }

    @Override
    @CircuitBreaker(name = SMS_SERVICE, fallbackMethod = "fallbackCampaignQuery")
    @Retry(name = SMS_SERVICE)
    public SmsCampaignResultQuery getResults(String campaignId) {
        log.info("[SMS] Obteniendo resultados de campaña: {}", campaignId);

        if (campaignId == null || campaignId.isBlank()) {
            log.error("[SMS] Campaign ID requerido");
            return null;
        }

        // Usar router para obtener resultados
        return providerRouter.execute(
                provider -> provider.getCampaignResults(campaignId),
                errorMsg -> null
        );
    }

}
