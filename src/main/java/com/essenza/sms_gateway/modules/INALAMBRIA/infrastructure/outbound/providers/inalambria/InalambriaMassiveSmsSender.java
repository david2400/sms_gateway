package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.providers.inalambria;

import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaMasivoRequest;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.config.InalambriaProperties;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaMappingSupport;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaSmsMasivoMapper;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsBatchSenderPort;
import com.essenza.sms_gateway.modules.SmsCore.shared.ProviderRestTemplateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Envío masivo nativo de Inalambria ({@code POST https://rest.inalambria.com/mtmessage/massive}).
 * Request, headers y reglas de la respuesta están en {@link InalambriaSmsMasivoMapper}.
 * Un intento por llamada; reintentos y fallback los hace {@code SmsBatchDispatchService} de SmsCore.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InalambriaMassiveSmsSender implements SmsBatchSenderPort {

    public static final String OPT_MASSIVE_URL = "massiveUrl";
    public static final String OPT_MASSIVE_CHUNK_SIZE = "massiveChunkSize";

    private final InalambriaSmsMasivoMapper mapper;
    private final ProviderRestTemplateFactory restTemplateFactory;
    private final InalambriaProperties properties;

    @Override
    public String providerCode() {
        return InalambriaSmsSender.PROVIDER_CODE;
    }

    /** SMS normales; el BIDIRECCIONAL necesita su código corto y va uno a uno por {@code /momessage}. */
    @Override
    public boolean supports(MessageType type) {
        return type != null && type.isSms() && type != MessageType.BIDIRECCIONAL;
    }

    @Override
    public int maxBatchSize(ProviderAccount account) {
        String custom = account.parameter(OPT_MASSIVE_CHUNK_SIZE);
        if (custom != null && !custom.isBlank()) {
            try {
                return Math.max(1, Integer.parseInt(custom.trim()));
            } catch (NumberFormatException e) {
                log.warn("[INALAMBRIA-MASIVO] massiveChunkSize inválido en la cuenta {}: {}", account.getCode(), custom);
            }
        }
        return Math.max(1, properties.getMassive().getChunkSize());
    }

    @Override
    public SendOutcome sendBatch(List<SmsDispatch> messages, ProviderAccount account, String batchId) {
        if (InalambriaSmsSender.isWhatsAppAccount(account)) {
            return SendOutcome.permanentFail(null, "La cuenta " + account.getCode() + " es de WhatsApp; no envía SMS masivos", 0);
        }
        if (!account.hasCredentials() || account.getUsername() == null) {
            return SendOutcome.permanentFail(null, "Cuenta " + account.getCode()
                    + " sin credenciales: revise usuario y la clave (clave_cifrada + SMS_CRYPTO_KEY, o secret_ref)", 0);
        }
        String custom = InalambriaMappingSupport.firstNonBlank(account.parameter(OPT_MASSIVE_URL));
        String url = custom != null ? custom : properties.getMassive().getUrl();

        long start = System.currentTimeMillis();
        try {
            HttpEntity<InalambriaMasivoRequest> request = mapper.toHttpRequest(messages, account, batchId);
            log.debug("[INALAMBRIA-MASIVO] POST {} cuenta {} lote {} ({} SMS)", url, account.getCode(), batchId, messages.size());
            ResponseEntity<String> response = restTemplateFactory
                    .get(account.getConnectTimeoutMs(), account.getReadTimeoutMs())
                    .postForEntity(url, request, String.class);
            SendOutcome outcome = mapper.toOutcome(mapper.parseResponse(response.getBody()),
                    response.getStatusCode().value(), System.currentTimeMillis() - start);
            if (outcome.isSuccess()) {
                log.info("[INALAMBRIA-MASIVO] Lote {} aceptado ({} SMS)", batchId, messages.size());
            }
            return outcome;

        } catch (HttpStatusCodeException e) {
            return mapper.toOutcome(mapper.parseResponse(e.getResponseBodyAsString()), e.getStatusCode().value(),
                    System.currentTimeMillis() - start);
        } catch (RestClientException e) {
            return SendOutcome.fail(null, e.getClass().getSimpleName() + ": " + InalambriaMappingSupport.truncate(e.getMessage()),
                    System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            log.error("[INALAMBRIA-MASIVO] Error inesperado en lote {}: {}", batchId, e.getMessage(), e);
            return SendOutcome.fail(null, "Error técnico: " + InalambriaMappingSupport.truncate(e.getMessage()),
                    System.currentTimeMillis() - start);
        }
    }
}
