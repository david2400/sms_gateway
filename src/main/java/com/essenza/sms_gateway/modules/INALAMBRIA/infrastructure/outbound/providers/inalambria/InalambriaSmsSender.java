package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.providers.inalambria;

import com.essenza.sms_gateway.modules.SmsCore.shared.ProviderRestTemplateFactory;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaDispatchPayloadMapper;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaHttpRequestMapper;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;

/**
 * Implementación de Inalambria para el motor de SmsCore.
 * <p>
 * Hace UN intento por llamada (los reintentos y el fallback los hace SmsCore).
 * Éxito = HTTP 2xx; por ahora no se interpreta el body de la respuesta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InalambriaSmsSender implements SmsSenderPort {

    public static final String PROVIDER_CODE = "INALAMBRIA";
    private static final String DEFAULT_PATH = "/MTMessage";
    private static final int MAX_DETAIL = 500;

    private final InalambriaDispatchPayloadMapper payloadMapper;
    private final InalambriaHttpRequestMapper httpRequestMapper;
    private final ProviderRestTemplateFactory restTemplateFactory;

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public SendOutcome send(SmsDispatch message, ProviderAccount account) {
        long start = System.currentTimeMillis();

        if (!account.hasCredentials() || account.getUsername() == null) {
            return SendOutcome.fail(null, "Cuenta " + account.getCode()
                    + " sin credenciales: revise usuario y la variable de entorno de secret_ref", 0);
        }

        String path = account.parameter("path") != null ? account.parameter("path") : DEFAULT_PATH;
        String url = httpRequestMapper.mapToEndpointUrl(account.getBaseUrl(), path);

        try {
            HashMap<String, Object> payload = payloadMapper.toPayload(message, account);
            HttpEntity<Object> request = httpRequestMapper.mapToHttpRequest(payload, account.getUsername(), account.getPassword());

            log.debug("[INALAMBRIA-SENDER] POST {} cuenta {} envío {}", url, account.getCode(), message.getId());
            ResponseEntity<String> response = restTemplateFactory
                    .get(account.getConnectTimeoutMs(), account.getReadTimeoutMs())
                    .postForEntity(url, request, String.class);

            int status = response.getStatusCode().value();
            long elapsed = System.currentTimeMillis() - start;
            return response.getStatusCode().is2xxSuccessful()
                    ? SendOutcome.ok(status, elapsed)
                    : SendOutcome.fail(status, truncate(response.getBody()), elapsed);

        } catch (HttpStatusCodeException e) {
            // 4xx / 5xx con respuesta del proveedor
            return SendOutcome.fail(e.getStatusCode().value(), truncate(e.getResponseBodyAsString()),
                    System.currentTimeMillis() - start);
        } catch (RestClientException e) {
            // Timeout, error de red, DNS: no hay HTTP status
            return SendOutcome.fail(null, e.getClass().getSimpleName() + ": " + truncate(e.getMessage()),
                    System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            log.error("[INALAMBRIA-SENDER] Error inesperado en envío {}: {}", message.getId(), e.getMessage(), e);
            return SendOutcome.fail(null, "Error técnico: " + truncate(e.getMessage()),
                    System.currentTimeMillis() - start);
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_DETAIL ? value : value.substring(0, MAX_DETAIL);
    }
}
