package com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.Go4ClientsDestinatarioDto;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignReport;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignResult;
import com.essenza.sms_gateway.modules.Go4Clients.domain.ports.Go4ClientsCampaignPort;
import com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.mappers.Go4ClientsCampaignMapper;
import com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.mappers.Go4ClientsMappingSupport;
import com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.mappers.Go4ClientsMessageMapper;
import com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.mappers.Go4ClientsReportMapper;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.shared.ProviderRestTemplateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP de la API de campañas de Go4Clients. Un intento por llamada, sin excepciones hacia afuera.
 * Request y respuesta de cada operación en su mapper: {@link Go4ClientsCampaignMapper}, {@link Go4ClientsMessageMapper},
 * {@link Go4ClientsReportMapper}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Go4ClientsCampaignHttpAdapter implements Go4ClientsCampaignPort {

    private final Go4ClientsCampaignMapper campaignMapper;
    private final Go4ClientsMessageMapper messageMapper;
    private final Go4ClientsReportMapper reportMapper;
    private final ProviderRestTemplateFactory restTemplateFactory;

    @Override
    public Go4ClientsCampaignResult createCampaign(ProviderAccount account, String name, String sender) {
        if (!hasCredentials(account)) {
            return Go4ClientsCampaignResult.failure(account.getCode(), null, "SIN_CREDENCIALES", credentialsMessage(account));
        }
        try {
            ResponseEntity<String> response = rest(account).postForEntity(campaignMapper.url(account),
                    campaignMapper.toHttpRequest(account, name, sender), String.class);
            Go4ClientsCampaignResult result = campaignMapper.toResult(campaignMapper.parseResponse(response.getBody()),
                    response.getStatusCode().value(), account);
            log.info("[GO4CLIENTS] Campaña '{}' creada con cuenta {} (status {})", name, account.getCode(), result.getHttpStatus());
            return result;
        } catch (HttpStatusCodeException e) {
            return campaignMapper.toResult(campaignMapper.parseResponse(e.getResponseBodyAsString()), e.getStatusCode().value(), account);
        } catch (RestClientException e) {
            return communicationError(account, e);
        }
    }

    @Override
    public Go4ClientsCampaignResult sendMessages(ProviderAccount account, String campaignId, String message,
                                                 List<Go4ClientsDestinatarioDto> destinations, String priority,
                                                 Map<String, Map<String, String>> landingCustomFields) {
        if (!hasCredentials(account)) {
            return Go4ClientsCampaignResult.failure(account.getCode(), null, "SIN_CREDENCIALES", credentialsMessage(account));
        }
        try {
            ResponseEntity<String> response = rest(account).postForEntity(messageMapper.url(account, campaignId),
                    messageMapper.toHttpRequest(account, message, destinations, priority, landingCustomFields), String.class);
            Go4ClientsCampaignResult result = messageMapper.toResult(messageMapper.parseResponse(response.getBody()),
                    response.getStatusCode().value(), account, campaignId);
            log.info("[GO4CLIENTS] Campaña {}: {} mensajes generados (status {})", campaignId, result.getGeneratedMessages(),
                    result.getHttpStatus());
            return result;
        } catch (HttpStatusCodeException e) {
            return messageMapper.toResult(messageMapper.parseResponse(e.getResponseBodyAsString()), e.getStatusCode().value(),
                    account, campaignId);
        } catch (RestClientException e) {
            return communicationError(account, e);
        }
    }

    @Override
    public Go4ClientsCampaignReport getReport(ProviderAccount account, String campaignId) {
        if (!hasCredentials(account)) {
            return Go4ClientsCampaignReport.builder().campaignId(campaignId).errorMessage(credentialsMessage(account)).build();
        }
        try {
            ResponseEntity<String> response = rest(account).exchange(reportMapper.url(account, campaignId), HttpMethod.GET,
                    reportMapper.toHttpRequest(account), String.class);
            return reportMapper.toReport(reportMapper.parseResponse(response.getBody()), response.getStatusCode().value(), campaignId);
        } catch (HttpStatusCodeException e) {
            return reportMapper.toReport(reportMapper.parseResponse(e.getResponseBodyAsString()), e.getStatusCode().value(), campaignId);
        } catch (RestClientException e) {
            return Go4ClientsCampaignReport.builder().campaignId(campaignId)
                    .errorMessage(e.getClass().getSimpleName() + ": " + Go4ClientsMappingSupport.truncate(e.getMessage())).build();
        }
    }

    private RestTemplate rest(ProviderAccount account) {
        return restTemplateFactory.get(account.getConnectTimeoutMs(), account.getReadTimeoutMs());
    }

    private static Go4ClientsCampaignResult communicationError(ProviderAccount account, RestClientException e) {
        return Go4ClientsCampaignResult.failure(account.getCode(), null, "COMMUNICATION_ERROR",
                e.getClass().getSimpleName() + ": " + Go4ClientsMappingSupport.truncate(e.getMessage()));
    }

    private static boolean hasCredentials(ProviderAccount account) {
        return account.hasCredentials() && account.getUsername() != null && !account.getUsername().isBlank();
    }

    private static String credentialsMessage(ProviderAccount account) {
        return "Cuenta " + account.getCode() + " sin credenciales: revise usuario (api key) y el api secret (clave_cifrada + SMS_CRYPTO_KEY, o la variable de secret_ref)";
    }
}
