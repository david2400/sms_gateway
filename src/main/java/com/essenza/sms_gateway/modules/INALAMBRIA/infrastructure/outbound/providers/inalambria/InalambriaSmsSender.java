package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.providers.inalambria;

import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaMtMessageRequest;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaMappingSupport;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaSmsMapper;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsSenderPort;
import com.essenza.sms_gateway.modules.SmsCore.shared.ProviderRestTemplateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

/**
 * Implementación de Inalambria para el motor de SmsCore. Tipos de canal WHATSAPP se delegan a
 * {@link InalambriaWhatsAppSender}, el tipo BIDIRECCIONAL a {@link InalambriaBidireccionalSender} ({@code /momessage});
 * el resto sale por SMS (RESTAWS MTMessage) con {@link InalambriaSmsMapper}.
 * <p>
 * Hace UN intento por llamada (los reintentos y el fallback los hace SmsCore).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InalambriaSmsSender implements SmsSenderPort {

    public static final String PROVIDER_CODE = "INALAMBRIA";
    /** {@code parametros_json.canal} de la cuenta: SMS (defecto) o WHATSAPP. Evita usar una cuenta en el canal equivocado. */
    public static final String OPT_CANAL = "canal";
    public static final String CANAL_WHATSAPP = "WHATSAPP";
    /** {@code parametros_json.path}: ruta o URL completa del endpoint (defecto /MTMessage sobre base_url). */
    public static final String OPT_PATH = "path";
    private static final String DEFAULT_PATH = "/MTMessage";

    private final InalambriaSmsMapper mapper;
    private final ProviderRestTemplateFactory restTemplateFactory;
    private final InalambriaWhatsAppSender whatsAppSender;
    private final InalambriaBidireccionalSender bidireccionalSender;

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public SendOutcome send(SmsDispatch message, ProviderAccount account) {
        // El motor tiene un SmsSenderPort por proveedor: el canal lo decide el módulo del proveedor
        boolean cuentaWhatsApp = isWhatsAppAccount(account);
        if (message.getType() != null && message.getType().isWhatsapp()) {
            return cuentaWhatsApp ? whatsAppSender.send(message, account)
                    : SendOutcome.permanentFail(null, "La cuenta " + account.getCode()
                    + " es de SMS; para WhatsApp use una cuenta con parametros_json.canal = WHATSAPP", 0);
        }
        if (cuentaWhatsApp) {
            return SendOutcome.permanentFail(null, "La cuenta " + account.getCode() + " es de WhatsApp; no envía SMS", 0);
        }
        if (message.getType() == MessageType.BIDIRECCIONAL) {
            return bidireccionalSender.send(message, account);
        }
        if (!account.hasCredentials() || account.getUsername() == null) {
            return SendOutcome.fail(null, "Cuenta " + account.getCode()
                    + " sin credenciales: revise usuario y la clave (clave_cifrada + SMS_CRYPTO_KEY, o la variable de secret_ref)", 0);
        }

        long start = System.currentTimeMillis();
        String url = endpoint(account);
        try {
            HttpEntity<InalambriaMtMessageRequest> request = mapper.toHttpRequest(message, account);
            log.debug("[INALAMBRIA-SENDER] POST {} cuenta {} envío {}", url, account.getCode(), message.getId());
            ResponseEntity<String> response = restTemplateFactory
                    .get(account.getConnectTimeoutMs(), account.getReadTimeoutMs())
                    .postForEntity(url, request, String.class);
            return mapper.toOutcome(mapper.parseResponse(response.getBody()), response.getStatusCode().value(),
                    System.currentTimeMillis() - start);

        } catch (HttpStatusCodeException e) {
            // 4xx / 5xx con respuesta del proveedor
            return mapper.toOutcome(mapper.parseResponse(e.getResponseBodyAsString()), e.getStatusCode().value(),
                    System.currentTimeMillis() - start);
        } catch (RestClientException e) {
            // Timeout, error de red, DNS: no hay HTTP status
            return SendOutcome.fail(null, e.getClass().getSimpleName() + ": " + InalambriaMappingSupport.truncate(e.getMessage()),
                    System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            log.error("[INALAMBRIA-SENDER] Error inesperado en envío {}: {}", message.getId(), e.getMessage(), e);
            return SendOutcome.fail(null, "Error técnico: " + InalambriaMappingSupport.truncate(e.getMessage()),
                    System.currentTimeMillis() - start);
        }
    }

    public static boolean isWhatsAppAccount(ProviderAccount account) {
        String canal = account.parameter(OPT_CANAL);
        return canal != null && CANAL_WHATSAPP.equalsIgnoreCase(canal.trim());
    }

    private static String endpoint(ProviderAccount account) {
        String path = account.parameter(OPT_PATH) != null ? account.parameter(OPT_PATH) : DEFAULT_PATH;
        return path.startsWith("http") ? path : account.getBaseUrl() + path;
    }
}
