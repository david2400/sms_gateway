package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.providers.inalambria;

import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaMoMessageRequest;
import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaMoMessageResponse;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.config.InalambriaProperties;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaMappingSupport;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaSmsBidireccionalMapper;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.shared.ProviderRestTemplateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

/**
 * SMS bidireccional de Inalambria ({@code POST https://rest.inalambria.com/momessage}). Lo invoca
 * {@link InalambriaSmsSender} para el tipo BIDIRECCIONAL. Request y reglas de la respuesta en
 * {@link InalambriaSmsBidireccionalMapper}. Sin credenciales o sin código corto: error permanente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InalambriaBidireccionalSender {

    public static final String OPT_BIDIRECCIONAL_URL = "bidireccionalUrl";

    private final InalambriaSmsBidireccionalMapper mapper;
    private final ProviderRestTemplateFactory restTemplateFactory;
    private final InalambriaProperties properties;

    public SendOutcome send(SmsDispatch message, ProviderAccount account) {
        if (!account.hasCredentials() || account.getUsername() == null) {
            return SendOutcome.permanentFail(null, "Cuenta " + account.getCode()
                    + " sin credenciales: revise usuario y clave (clave_cifrada + SMS_CRYPTO_KEY, o secret_ref)", 0);
        }
        if (mapper.shortCode(message, account) == null) {
            return SendOutcome.permanentFail(null, "Cuenta " + account.getCode()
                    + " sin código corto: configure SmsCuenta.remitente o parametros_json.shortCode (o envíe sender/short_code)", 0);
        }
        String custom = InalambriaMappingSupport.firstNonBlank(account.parameter(OPT_BIDIRECCIONAL_URL));
        String url = custom != null ? custom : properties.getBidireccional().getUrl();

        long start = System.currentTimeMillis();
        try {
            HttpEntity<InalambriaMoMessageRequest> request = mapper.toHttpRequest(message, account);
            log.debug("[INALAMBRIA-BIDIRECCIONAL] POST {} cuenta {} envío {}", url, account.getCode(), message.getId());
            ResponseEntity<String> response = restTemplateFactory
                    .get(account.getConnectTimeoutMs(), account.getReadTimeoutMs())
                    .postForEntity(url, request, String.class);
            InalambriaMoMessageResponse body = mapper.parseResponse(response.getBody());
            SendOutcome outcome = mapper.toOutcome(body, response.getStatusCode().value(), System.currentTimeMillis() - start);
            if (outcome.isSuccess()) {
                log.info("[INALAMBRIA-BIDIRECCIONAL] Envío {} aceptado, Id {}", message.getId(), mapper.numeroTransaccion(body));
            }
            return outcome;

        } catch (HttpStatusCodeException e) {
            return mapper.toOutcome(mapper.parseResponse(e.getResponseBodyAsString()), e.getStatusCode().value(),
                    System.currentTimeMillis() - start);
        } catch (RestClientException e) {
            return SendOutcome.fail(null, e.getClass().getSimpleName() + ": " + InalambriaMappingSupport.truncate(e.getMessage()),
                    System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            log.error("[INALAMBRIA-BIDIRECCIONAL] Error inesperado en envío {}: {}", message.getId(), e.getMessage(), e);
            return SendOutcome.fail(null, "Error técnico: " + InalambriaMappingSupport.truncate(e.getMessage()),
                    System.currentTimeMillis() - start);
        }
    }
}
