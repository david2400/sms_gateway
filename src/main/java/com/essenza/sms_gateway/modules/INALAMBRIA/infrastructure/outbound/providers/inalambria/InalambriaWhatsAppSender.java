package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.providers.inalambria;

import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaWhatsAppRequest;
import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaWhatsAppResponse;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.config.InalambriaProperties;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaMappingSupport;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaWhatsAppMapper;
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
 * WhatsApp de Inalambria ({@code POST https://multichannel.inalambria.com/MultiChannelInput/SendMessageMetaWhatsApp}).
 * Lo invoca {@link InalambriaSmsSender} cuando el tipo de mensaje es de canal WHATSAPP. Request y reglas de la respuesta
 * en {@link InalambriaWhatsAppMapper}. Validaciones previas (permanentes): credenciales, plantilla y número de origen.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InalambriaWhatsAppSender {

    public static final String OPT_WHATSAPP_URL = "whatsappUrl";

    private final InalambriaWhatsAppMapper mapper;
    private final ProviderRestTemplateFactory restTemplateFactory;
    private final InalambriaProperties properties;

    public SendOutcome send(SmsDispatch message, ProviderAccount account) {
        if (!account.hasCredentials() || account.getUsername() == null) {
            return SendOutcome.permanentFail(null, "Cuenta " + account.getCode()
                    + " sin credenciales de WhatsApp: revise usuario y clave (clave_cifrada + SMS_CRYPTO_KEY, o secret_ref)", 0);
        }
        if (message.getExternalTemplateId() == null || message.getExternalTemplateId().isBlank()) {
            return SendOutcome.permanentFail(null, "WhatsApp requiere template_id (plantilla aprobada por Meta)", 0);
        }
        if (mapper.source(message, account) == null) {
            return SendOutcome.permanentFail(null, "Cuenta " + account.getCode()
                    + " sin número de origen: configure SmsCuenta.remitente o parametros_json.source", 0);
        }
        String custom = InalambriaMappingSupport.firstNonBlank(account.parameter(OPT_WHATSAPP_URL));
        String url = custom != null ? custom : properties.getWhatsapp().getUrl();

        long start = System.currentTimeMillis();
        try {
            HttpEntity<InalambriaWhatsAppRequest> request = mapper.toHttpRequest(message, account);
            log.debug("[INALAMBRIA-WHATSAPP] POST {} cuenta {} envío {} plantilla {}", url, account.getCode(),
                    message.getId(), message.getExternalTemplateId());
            ResponseEntity<String> response = restTemplateFactory
                    .get(account.getConnectTimeoutMs(), account.getReadTimeoutMs())
                    .postForEntity(url, request, String.class);
            InalambriaWhatsAppResponse body = mapper.parseResponse(response.getBody());
            SendOutcome outcome = mapper.toOutcome(body, response.getStatusCode().value(), System.currentTimeMillis() - start);
            if (outcome.isSuccess()) {
                log.info("[INALAMBRIA-WHATSAPP] Envío {} aceptado, transactionNumber {}", message.getId(), mapper.numeroTransaccion(body));
            }
            return outcome;

        } catch (HttpStatusCodeException e) {
            return mapper.toOutcome(mapper.parseResponse(e.getResponseBodyAsString()), e.getStatusCode().value(),
                    System.currentTimeMillis() - start);
        } catch (RestClientException e) {
            return SendOutcome.fail(null, e.getClass().getSimpleName() + ": " + InalambriaMappingSupport.truncate(e.getMessage()),
                    System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            log.error("[INALAMBRIA-WHATSAPP] Error inesperado en envío {}: {}", message.getId(), e.getMessage(), e);
            return SendOutcome.fail(null, "Error técnico: " + InalambriaMappingSupport.truncate(e.getMessage()),
                    System.currentTimeMillis() - start);
        }
    }
}
