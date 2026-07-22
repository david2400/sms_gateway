package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.CreateSmsCampaignCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.SmsCampaignResultQuery;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.OtpSendResult;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.SendOtpRequest;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSmsBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.model.*;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.SmsProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Provider Mock para pruebas y desarrollo
 * Simula el comportamiento de un proveedor real sin realizar llamadas HTTP
 */
@Slf4j
@Component
public class MockSmsProvider implements SmsProviderPort {

    private SmsProviderConfig config;
    private boolean configured = false;

    @Override
    public SmsProvider getProviderType() {
        return SmsProvider.MOCK;
    }

    @Override
    public void configure(SmsProviderConfig config) {
        this.config = config;
        this.configured = true;
        log.info("[MOCK] Configurado como provider de fallback");
    }

    @Override
    public boolean isAvailable() {
        return true; // Mock siempre disponible
    }

    @Override
    public String getProviderName() {
        return "Mock";
    }

    @Override
    public SmsCampaignOperationResult createCampaign(CreateSmsCampaignCommand command) {
        log.info("[MOCK] Creando campaña mock: {}", command.getName());
        String campaignId = "MOCK-" + UUID.randomUUID().toString().substring(0, 8);
        return SmsCampaignOperationResult.success(campaignId, campaignId);
    }

    @Override
    public SmsBatchResult sendBatch(SendSmsBatchCommand command) {
        log.info("[MOCK] Enviando batch mock de {} destinatarios", command.getDestinations().size());
        
        return SmsBatchResult.builder()
                .success(true)
                .campaignId(command.getCampaignId())
                .batchId("MOCK-BATCH-" + System.currentTimeMillis())
                .acceptedCount(command.getDestinations().size())
                .rejectedCount(0)
                .timestamp(LocalDateTime.now())
                .providerReference("MOCK-REF-" + UUID.randomUUID().toString().substring(0, 8))
                .build();
    }

    @Override
    public OtpSendResult sendOtp(SendOtpRequest request) {
        log.info("[MOCK] Enviando OTP mock a: {}", request.getPhoneNumber());
        
        return OtpSendResult.success(
                "MOCK-OTP-" + System.currentTimeMillis(),
                "MOCK-REF-" + UUID.randomUUID().toString().substring(0, 8),
                request.getPhoneNumber(),
                request.getOtpCode(),
                request.getExpirationMinutes()
        );
    }

    @Override
    public SmsCampaignResultQuery getCampaignResults(String campaignId) {
        log.info("[MOCK] Obteniendo resultados mock de campaña: {}", campaignId);
        
        return SmsCampaignResultQuery.builder()
                .campaignId(campaignId)
                .build();
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
