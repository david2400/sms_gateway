package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.CreateSmsCampaignCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.SmsCampaignResultQuery;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.OtpSendResult;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.SendOtpRequest;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSmsBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.model.*;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.SmsProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Adaptador para el proveedor Go4Clients
 * Implementa la interfaz unificada para enviar SMS vía Go4Clients
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.providers.go4clients.enabled", havingValue = "true")
public class Go4ClientsSmsProvider implements SmsProviderPort {

    private final RestTemplate restTemplate;
    private SmsProviderConfig config;
    
    public Go4ClientsSmsProvider(@Qualifier("go4ClientsRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    private static final String BASE_URL = "https://cloud.go4clients.com:8580/api";
    private static final String CAMPAIGNS_URL = BASE_URL + "/campaigns/sms/v1.0";
    private static final String ANALYTICS_URL = BASE_URL + "/analytics/sms/v1.0";

    @Override
    public SmsProvider getProviderType() {
        return SmsProvider.GO4CLIENTS;
    }

    @Override
    public void configure(SmsProviderConfig config) {
        this.config = config;
        log.info("[GO4CLIENTS] Configurado con API Key: {}", config.getApiKey().substring(0, 4) + "****");
    }

    @Override
    public boolean isAvailable() {
        if (config == null || !config.isValid()) {
            return false;
        }
        
        try {
            // Go4Clients no tiene endpoint de health check confiable, 
            // así que verificamos que la configuración sea válida
            return config.getApiKey() != null && !config.getApiKey().trim().isEmpty() &&
                   config.getApiSecret() != null && !config.getApiSecret().trim().isEmpty() &&
                   config.getBaseUrl() != null && !config.getBaseUrl().trim().isEmpty();
        } catch (Exception e) {
            log.warn("[GO4CLIENTS] Error verificando disponibilidad: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "Go4Clients";
    }

    @Override
    public SmsCampaignOperationResult createCampaign(CreateSmsCampaignCommand command) {
        log.info("[GO4CLIENTS] Creando campaña: {}", command.getName());

        try {
            HttpHeaders headers = createHeaders();
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", command.getName());
            payload.put("sender", config.getSender() != null ? config.getSender() : "RANDOM");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(CAMPAIGNS_URL, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                String campaignId = (String) responseBody.get("id");
                String reference = (String) responseBody.get("reference");
                
                log.info("[GO4CLIENTS] ✅ Campaña creada. ID: {}", campaignId);
                return SmsCampaignOperationResult.success(campaignId, reference);
            } else {
                log.error("[GO4CLIENTS] Error creando campaña: {}", response.getStatusCode());
                return SmsCampaignOperationResult.failure("Error creando campaña", "HTTP_ERROR");
            }

        } catch (Exception e) {
            log.error("[GO4CLIENTS] Error creando campaña: {}", e.getMessage(), e);
            return SmsCampaignOperationResult.failure("Error interno: " + e.getMessage(), "INTERNAL_ERROR");
        }
    }

    @Override
    public SmsBatchResult sendBatch(SendSmsBatchCommand command) {
        log.info("[GO4CLIENTS] Enviando batch a {} destinatarios", command.getDestinations().size());

        try {
            HttpHeaders headers = createHeaders();
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("priority", "HIGH");
            payload.put("message", command.getMessage());
            payload.put("destinationsList", command.getDestinations());
            
            // Agregar landingCustomFields si existen
            if (command.getLandingCustomFields() != null) {
                payload.put("landingCustomFields", command.getLandingCustomFields());
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(CAMPAIGNS_URL + "/" + command.getCampaignId(), entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                List<String> generatedIds = (List<String>) responseBody.get("generatedIds");
                
                log.info("[GO4CLIENTS] ✅ Batch enviado. IDs generados: {}", generatedIds.size());
                return SmsBatchResult.builder()
                    .success(true)
                    .campaignId(command.getCampaignId())
                    .batchId(UUID.randomUUID().toString())
                    .acceptedCount(generatedIds.size())
                    .rejectedCount(0)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
            } else {
                log.error("[GO4CLIENTS] Error en batch: {}", response.getStatusCode());
                return SmsBatchResult.failure("Error en batch", "HTTP_ERROR");
            }

        } catch (Exception e) {
            log.error("[GO4CLIENTS] Error en batch: {}", e.getMessage(), e);
            return SmsBatchResult.failure("Error interno: " + e.getMessage(), "INTERNAL_ERROR");
        }
    }

    @Override
    public OtpSendResult sendOtp(SendOtpRequest request) {
        log.info("[GO4CLIENTS] Enviando OTP a {}", request.getPhoneNumber());

        try {
            if (!validatePhoneNumber(request.getPhoneNumber())) {
                return OtpSendResult.failure("Teléfono inválido", "INVALID_PHONE");
            }

            // Crear campaña temporal para OTP
            String campaignName = "OTP-" + System.currentTimeMillis();
            CreateSmsCampaignCommand campaignCommand = CreateSmsCampaignCommand.builder()
                    .name(campaignName)
                    .build();

            SmsCampaignOperationResult campaignResult = createCampaign(campaignCommand);
            
            if (!campaignResult.isSuccess()) {
                return OtpSendResult.failure("Error creando campaña OTP", campaignResult.getErrorCode());
            }

            // Enviar el OTP como batch
            SendSmsBatchCommand batchCommand = SendSmsBatchCommand.builder()
                    .campaignId(campaignResult.getCampaignId())
                    .message(request.buildFullMessage())
                    .destinations(Collections.singletonList(
                        com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.DestinatarioDto.builder()
                            .phoneNumber(request.getPhoneNumber())
                            .countryCode(request.getCountryCode() != null ? request.getCountryCode() : "57")
                            .build()
                    ))
                    .build();

            SmsBatchResult batchResult = sendBatch(batchCommand);

            if (batchResult.isSuccess()) {
                log.info("[GO4CLIENTS] ✅ OTP enviado. CampaignID: {}", campaignResult.getCampaignId());
                return OtpSendResult.success(
                        batchResult.getBatchId(),
                        campaignResult.getProviderReference(),
                        request.getPhoneNumber(),
                        request.getOtpCode(),
                        request.getExpirationMinutes()
                );
            } else {
                return OtpSendResult.failure("Error enviando OTP", batchResult.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("[GO4CLIENTS] Error enviando OTP: {}", e.getMessage(), e);
            return OtpSendResult.failure("Error interno: " + e.getMessage(), "INTERNAL_ERROR");
        }
    }

    @Override
    public SmsCampaignResultQuery getCampaignResults(String campaignId) {
        log.info("[GO4CLIENTS] Obteniendo resultados de campaña: {}", campaignId);

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String url = ANALYTICS_URL + "?uniqueCampaignID=" + campaignId;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                
                // Parsear resultados según la estructura de Go4Clients
                int totalMessages = (int) data.getOrDefault("totalMessages", 0);
                int deliveredMessages = (int) data.getOrDefault("deliveredMessages", 0);
                int failedMessages = (int) data.getOrDefault("failedMessages", 0);
                int pendingMessages = totalMessages - deliveredMessages - failedMessages;

                log.info("[GO4CLIENTS] ✅ Resultados obtenidos - Total: {}, Entregados: {}, Fallidos: {}", 
                        totalMessages, deliveredMessages, failedMessages);

                return SmsCampaignResultQuery.builder()
                        .campaignId(campaignId)
                        .build();
            } else {
                log.error("[GO4CLIENTS] Error obteniendo resultados: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("[GO4CLIENTS] Error obteniendo resultados: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Crea los headers para autenticación con Go4Clients
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apiKey", config.getApiKey());
        headers.set("apiSecret", config.getApiSecret());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Override
    public ProviderCapabilities getCapabilities() {
        return ProviderCapabilities.builder()
                .supportsCampaigns(true)
                .supportsBatch(true)
                .supportsOtp(true)
                .supportsTemplates(true)
                .supportsScheduledMessages(true)
                .supportsUnicode(true)
                .supportsDeliveryReports(true)
                .supportsCustomSender(true)
                .maxBatchSize(10000)
                .maxMessageLength(1600)
                .maxRecipientsPerBatch(10000)
                .build();
    }
}
