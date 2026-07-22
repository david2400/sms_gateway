package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendInalambriaBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsBatchResult;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.SmsInalambriaPort;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.config.SmsProvidersProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

/**
 * Adapter puro de Inalambria - Única responsabilidad: Adaptación técnica
 * 
 * Principio de Single Responsibility:
 * - SOLO adapta entre comandos de dominio y comunicación externa
 * - NO construye payloads (eso es responsabilidad de un PayloadBuilder)
 * - NO ejecuta HTTP (eso es responsabilidad de un HttpClient)
 * - NO transforma respuestas (eso es responsabilidad de un ResponseTransformer)
 * - SOLO orquesta la adaptación
 * 
 * Flujo limpio: Provider → Adapter → PayloadBuilder → HttpClient → ResponseTransformer → Provider
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.providers.inalambria.enabled", havingValue = "true")
public class InalambriaSmsAdapter implements SmsInalambriaPort {

    private final RestTemplate restTemplate;
    private final SmsProvidersProperties props;
    private final InalambriaPayloadBuilder payloadBuilder;
    private final InalambriaHttpClient httpClient;
    private final InalambriaResponseTransformer responseTransformer;

    public InalambriaSmsAdapter(
            @Qualifier("inalambriaRestTemplate") RestTemplate restTemplate,
            SmsProvidersProperties smsProviderProperties,
            InalambriaPayloadBuilder payloadBuilder,
            InalambriaHttpClient httpClient,
            InalambriaResponseTransformer responseTransformer) {
        this.restTemplate = restTemplate;
        this.props = smsProviderProperties;
        this.payloadBuilder = payloadBuilder;
        this.httpClient = httpClient;
        this.responseTransformer = responseTransformer;
        
        // Obtener configuración del provider
        var providerConfig = props.getProviderConfig("inalambria");
        String baseUrl = providerConfig != null ? providerConfig.getBaseUrl() : "https://restaws.inalambria.com";
        log.info("[INALAMBRIA-ADAPTER] Inicializado con baseUrl: {}", baseUrl);
    }

    @Override
    public SmsBatchResult sendBatch(SendInalambriaBatchCommand command) {
        log.debug("[INALAMBRIA-ADAPTER] Adaptando batch con {} dispositivos", 
                command.getDevices() != null ? command.getDevices().size() : 0);

        try {
            // Single Responsibility: SOLO orquestar la adaptación
            
            // 1. Delegar construcción de payload al especialista
            HashMap<String, Object> payload = payloadBuilder.buildFromCommand(command);
            
            // 2. Delegar comunicación HTTP al especialista con tipo de cuenta
            String accountType = command.getAccountType() != null ? command.getAccountType().name().toLowerCase() : "masivo";
            ResponseEntity<HashMap<String, Object>> httpResponse = httpClient.sendRequest(payload, accountType);
            
            // 3. Delegar transformación de respuesta al especialista
            return responseTransformer.transformToDomainModel(httpResponse);

        } catch (RestClientException ex) {
            log.error("[INALAMBRIA-ADAPTER] Error de comunicación: {}", ex.getMessage(), ex);
            return SmsBatchResult.failure(ex.getMessage(), "COMMUNICATION_ERROR");
        } catch (Exception ex) {
            log.error("[INALAMBRIA-ADAPTER] Error técnico inesperado: {}", ex.getMessage(), ex);
            return SmsBatchResult.failure("Error técnico", "TECHNICAL_ERROR");
        }
    }

    @Override
    public boolean isAvailable() {
        // Verificación técnica: delegar al cliente HTTP
        return httpClient.isServiceAvailable();
    }

    @Override
    public String getProviderName() {
        return "Inalambria";
    }
}
