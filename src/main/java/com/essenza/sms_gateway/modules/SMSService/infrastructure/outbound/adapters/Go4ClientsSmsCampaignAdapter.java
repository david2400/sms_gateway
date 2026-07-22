package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.CreateSmsCampaignCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSmsBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.SmsCampaignResultQuery;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsBatchResult;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsCampaignOperationResult;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsCampaignReport;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.SmsCampaignPort;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.config.SmsProvidersProperties;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.mappers.Go4Clients.Go4ClientsCampaignToPayloadMapper;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.mappers.Go4Clients.Go4ClientsHttpRequestMapper;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.mappers.Go4Clients.Go4ClientsResponseToDomainMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

/**
 * Adapter puro de Go4Clients - Única responsabilidad: Adaptación técnica
 * 
 * Principio de Single Responsibility:
 * - SOLO adapta entre comandos de dominio y comunicación externa
 * - NO construye payloads (eso es responsabilidad de un PayloadMapper)
 * - NO ejecuta HTTP (eso es responsabilidad de un HttpClient)
 * - NO transforma respuestas (eso es responsabilidad de un ResponseMapper)
 * - SOLO orquesta la adaptación usando MapStruct
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.go4clients.enabled", havingValue = "true")
public class Go4ClientsSmsCampaignAdapter implements SmsCampaignPort {

    private final RestTemplate restTemplate;
    private final SmsProvidersProperties props;
    private final Go4ClientsCampaignToPayloadMapper payloadMapper;
    private final Go4ClientsHttpRequestMapper httpMapper;
    private final Go4ClientsResponseToDomainMapper responseMapper;

    public Go4ClientsSmsCampaignAdapter(
            @Qualifier("go4ClientsRestTemplate") RestTemplate restTemplate,
            SmsProvidersProperties smsProviderProperties,
            Go4ClientsCampaignToPayloadMapper payloadMapper,
            Go4ClientsHttpRequestMapper httpMapper,
            Go4ClientsResponseToDomainMapper responseMapper) {
        this.restTemplate = restTemplate;
        this.props = smsProviderProperties;
        this.payloadMapper = payloadMapper;
        this.httpMapper = httpMapper;
        this.responseMapper = responseMapper;
    }

    @Override
    public SmsCampaignOperationResult createCampaign(CreateSmsCampaignCommand command) {
        log.debug("[GO4CLIENTS-ADAPTER] Creando campaña: {}", command.getName());

        try {
            // Single Responsibility: SOLO orquestar la adaptación
            
            // 1. Delegar construcción de payload al mapper MapStruct
            HashMap<String, Object> payload = payloadMapper.mapToApiPayload(command);
            
            // 2. Delegar comunicación HTTP al mapper y RestTemplate
            var providerConfig = props.getProviderConfig("go4clients");
            String baseUrl = providerConfig != null ? providerConfig.getBaseUrl() : "https://cloud.go4clients.com:8580/api";
            String apiKey = providerConfig != null ? providerConfig.getApiKey() : null;
            
            String endpointUrl = httpMapper.mapToEndpointUrl(baseUrl, "/campaigns");
            var request = httpMapper.mapToHttpRequest(payload, apiKey);
            ResponseEntity<HashMap<String, Object>> response = restTemplate.postForEntity(endpointUrl, request, (Class<HashMap<String, Object>>) (Class<?>) HashMap.class);
            
            // 3. Delegar transformación de respuesta al mapper MapStruct
            return responseMapper.mapToCampaignResult(response);

        } catch (RestClientException ex) {
            log.error("[GO4CLIENTS-ADAPTER] Error de comunicación: {}", ex.getMessage(), ex);
            return SmsCampaignOperationResult.failure("Error de comunicación", "COMMUNICATION_ERROR");
        } catch (Exception ex) {
            log.error("[GO4CLIENTS-ADAPTER] Error técnico inesperado: {}", ex.getMessage(), ex);
            return SmsCampaignOperationResult.failure("Error técnico", "TECHNICAL_ERROR");
        }
    }

    @Override
    public SmsBatchResult sendBatch(SendSmsBatchCommand command) {
        log.debug("[GO4CLIENTS-ADAPTER] Enviando batch para campaña: {}", command.getCampaignId());

        try {
            // Single Responsibility: SOLO orquestar la adaptación
            
            // 1. Delegar construcción de payload al mapper MapStruct
            HashMap<String, Object> payload = payloadMapper.mapToBatchPayload(command);
            
            // 2. Delegar comunicación HTTP al mapper y RestTemplate
            var providerConfig = props.getProviderConfig("go4clients");
            String baseUrl = providerConfig != null ? providerConfig.getBaseUrl() : "https://cloud.go4clients.com:8580/api";
            String apiKey = providerConfig != null ? providerConfig.getApiKey() : null;
            
            String endpointUrl = httpMapper.mapToEndpointUrl(baseUrl, 
                    "/campaigns/" + command.getCampaignId() + "/messages");
            var request = httpMapper.mapToHttpRequest(payload, apiKey);
            ResponseEntity<HashMap<String, Object>> response = restTemplate.postForEntity(endpointUrl, request, (Class<HashMap<String, Object>>) (Class<?>) HashMap.class);
            
            // 3. Delegar transformación de respuesta al mapper MapStruct
            return responseMapper.mapToBatchResult(response);

        } catch (RestClientException ex) {
            log.error("[GO4CLIENTS-ADAPTER] Error de comunicación: {}", ex.getMessage(), ex);
            return SmsBatchResult.failure("Error de comunicación", "COMMUNICATION_ERROR");
        } catch (Exception ex) {
            log.error("[GO4CLIENTS-ADAPTER] Error técnico inesperado: {}", ex.getMessage(), ex);
            return SmsBatchResult.failure("Error técnico", "TECHNICAL_ERROR");
        }
    }

    @Override
    public SmsCampaignReport getCampaignReport(SmsCampaignResultQuery query) {
        log.debug("[GO4CLIENTS-ADAPTER] Obteniendo reporte para campaña: {}", query.getCampaignId());

        try {
            // Single Responsibility: SOLO orquestar la adaptación
            
            // 1. Delegar construcción de URL al mapper
            var providerConfig = props.getProviderConfig("go4clients");
            String baseUrl = providerConfig != null ? providerConfig.getBaseUrl() : "https://cloud.go4clients.com:8580/api";
            
            String endpointUrl = httpMapper.mapToEndpointUrl(baseUrl, 
                    "/campaigns/" + query.getCampaignId() + "/report");
            
            // 2. Ejecutar petición GET (no necesita body)
            ResponseEntity<HashMap<String, Object>> response = restTemplate.getForEntity(endpointUrl, (Class<HashMap<String, Object>>) (Class<?>) HashMap.class);
            
            // 3. Delegar transformación de respuesta al mapper MapStruct
            return responseMapper.mapToCampaignReport(response);

        } catch (RestClientException ex) {
            log.error("[GO4CLIENTS-ADAPTER] Error de comunicación: {}", ex.getMessage(), ex);
            return SmsCampaignReport.builder()
                    .campaignId(query.getCampaignId())
                    .status("UNKNOWN")
                    .generatedAt(java.time.LocalDateTime.now())
                    .metadataEntry("error", ex.getMessage())
                    .build();
        } catch (Exception ex) {
            log.error("[GO4CLIENTS-ADAPTER] Error técnico inesperado: {}", ex.getMessage(), ex);
            return SmsCampaignReport.builder()
                    .campaignId(query.getCampaignId())
                    .status("ERROR")
                    .generatedAt(java.time.LocalDateTime.now())
                    .metadataEntry("error", ex.getMessage())
                    .build();
        }
    }

    @Override
    public boolean isAvailable() {
        // Verificación técnica básica
        var providerConfig = props.getProviderConfig("go4clients");
        if (providerConfig == null) return false;
        
        return providerConfig.isEnabled() && 
               providerConfig.getBaseUrl() != null && 
               !providerConfig.getBaseUrl().trim().isEmpty() &&
               providerConfig.getApiKey() != null && 
               !providerConfig.getApiKey().trim().isEmpty();
    }

    @Override
    public String getProviderName() {
        return "Go4Clients";
    }
}
