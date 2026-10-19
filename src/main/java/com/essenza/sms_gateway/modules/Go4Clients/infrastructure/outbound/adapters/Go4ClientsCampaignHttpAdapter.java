package com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.Go4ClientsDestinatarioDto;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignReport;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignResult;
import com.essenza.sms_gateway.modules.Go4Clients.domain.ports.Go4ClientsCampaignPort;
import com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.mappers.Go4ClientsPayloadMapper;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.shared.ProviderRestTemplateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Go4ClientsCampaignHttpAdapter implements Go4ClientsCampaignPort {

    private static final int MAX_DETAIL = 500;

    private final Go4ClientsPayloadMapper mapper;
    private final ProviderRestTemplateFactory restTemplateFactory;

    @Override
    public Go4ClientsCampaignResult createCampaign(ProviderAccount account, String name, String sender) {
        if (!hasCredentials(account)) {
            return Go4ClientsCampaignResult.failure(account.getCode(), null, "SIN_CREDENCIALES", credentialsMessage(account));
        }
        try {
            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(mapper.createCampaignPayload(account, name, sender), mapper.headers(account));
            ResponseEntity<Map> response = rest(account).postForEntity(mapper.campaignsUrl(account), request, Map.class);
            Map<?, ?> body = response.getBody();
            log.info("[GO4CLIENTS] Campaña '{}' creada con cuenta {} (status {})", name, account.getCode(), response.getStatusCode().value());
            return Go4ClientsCampaignResult.builder()
                    .success(response.getStatusCode().is2xxSuccessful())
                    .campaignId(mapper.text(body, "id"))
                    .reference(mapper.text(body, "reference"))
                    .cuenta(account.getCode())
                    .httpStatus(response.getStatusCode().value())
                    .build();
        } catch (HttpStatusCodeException e) {
            return Go4ClientsCampaignResult.failure(account.getCode(), e.getStatusCode().value(), "HTTP_ERROR",
                    truncate(e.getResponseBodyAsString()));
        } catch (RestClientException e) {
            return Go4ClientsCampaignResult.failure(account.getCode(), null, "COMMUNICATION_ERROR",
                    e.getClass().getSimpleName() + ": " + truncate(e.getMessage()));
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
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                    mapper.sendMessagesPayload(message, destinations, priority, landingCustomFields), mapper.headers(account));
            ResponseEntity<Map> response = rest(account)
                    .postForEntity(mapper.campaignsUrl(account) + "/" + campaignId, request, Map.class);
            int generated = mapper.listSize(response.getBody(), "generatedIds");
            log.info("[GO4CLIENTS] Campaña {}: {} mensajes generados (status {})", campaignId, generated, response.getStatusCode().value());
            return Go4ClientsCampaignResult.builder()
                    .success(response.getStatusCode().is2xxSuccessful())
                    .campaignId(campaignId)
                    .generatedMessages(generated)
                    .cuenta(account.getCode())
                    .httpStatus(response.getStatusCode().value())
                    .build();
        } catch (HttpStatusCodeException e) {
            return Go4ClientsCampaignResult.failure(account.getCode(), e.getStatusCode().value(), "HTTP_ERROR",
                    truncate(e.getResponseBodyAsString()));
        } catch (RestClientException e) {
            return Go4ClientsCampaignResult.failure(account.getCode(), null, "COMMUNICATION_ERROR",
                    e.getClass().getSimpleName() + ": " + truncate(e.getMessage()));
        }
    }

    @Override
    public Go4ClientsCampaignReport getReport(ProviderAccount account, String campaignId) {
        if (!hasCredentials(account)) {
            return Go4ClientsCampaignReport.builder().campaignId(campaignId).errorMessage(credentialsMessage(account)).build();
        }
        try {
            ResponseEntity<Map> response = rest(account).exchange(mapper.analyticsUrl(account, campaignId),
                    HttpMethod.GET, new HttpEntity<>(mapper.headers(account)), Map.class);
            Object data = response.getBody() != null ? response.getBody().get("data") : null;
            Map<?, ?> d = data instanceof Map<?, ?> m ? m : Map.of();
            int total = mapper.number(d, "totalMessages");
            int delivered = mapper.number(d, "deliveredMessages");
            int failed = mapper.number(d, "failedMessages");
            return Go4ClientsCampaignReport.builder()
                    .success(response.getStatusCode().is2xxSuccessful())
                    .campaignId(campaignId)
                    .total(total).delivered(delivered).failed(failed)
                    .pending(Math.max(0, total - delivered - failed))
                    .httpStatus(response.getStatusCode().value())
                    .build();
        } catch (HttpStatusCodeException e) {
            return Go4ClientsCampaignReport.builder().campaignId(campaignId)
                    .httpStatus(e.getStatusCode().value()).errorMessage(truncate(e.getResponseBodyAsString())).build();
        } catch (RestClientException e) {
            return Go4ClientsCampaignReport.builder().campaignId(campaignId)
                    .errorMessage(e.getClass().getSimpleName() + ": " + truncate(e.getMessage())).build();
        }
    }

    private RestTemplate rest(ProviderAccount account) {
        return restTemplateFactory.get(account.getConnectTimeoutMs(), account.getReadTimeoutMs());
    }

    private static boolean hasCredentials(ProviderAccount account) {
        return account.hasCredentials() && account.getUsername() != null && !account.getUsername().isBlank();
    }

    private static String credentialsMessage(ProviderAccount account) {
        return "Cuenta " + account.getCode() + " sin credenciales: revise usuario (api key) y la variable de secret_ref (api secret)";
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_DETAIL ? value : value.substring(0, MAX_DETAIL);
    }
}
