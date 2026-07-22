package com.essenza.sms_gateway.modules.SMSService.application.services;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.CreateSmsCampaignCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.SmsCampaignResultQuery;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.OtpSendResult;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.SendOtpRequest;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSMSCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSmsBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SmsTypeRequest;
import com.essenza.sms_gateway.modules.SMSService.application.input.Otp.SendOtpAutoGenerateUseCase;
import com.essenza.sms_gateway.modules.SMSService.application.input.Otp.SendOtpDefaultMessageUseCase;
import com.essenza.sms_gateway.modules.SMSService.application.input.Otp.SendOtpUseCase;
import com.essenza.sms_gateway.modules.SMSService.application.input.Sms.*;
import com.essenza.sms_gateway.modules.SMSService.domain.model.*;
import com.essenza.sms_gateway.modules.SMSService.domain.services.MessageOrchestrator;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.routing.SmsProviderRegistry;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.routing.SmsProviderRouter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Servicio unificado para operaciones SMS
 * Usa SmsProviderRouter para enrutamiento inteligente con failover automático
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SmsOperationsService implements
        SendOtpUseCase,
        SendSmsBatchUseCase,
        ManageSmsProvidersUseCase,
        SendOtpAutoGenerateUseCase,
        SendOtpDefaultMessageUseCase,
        SendSmsWithTypeUseCase,
        SmsServiceAvailabilityUseCase,
        SendSmsUseCase {

    private static final String SMS_SERVICE = "smsService";
    private static final String OTP_SERVICE = "otpService";

    private final MessageOrchestrator messageOrchestrator;
    private final SmsProviderRouter providerRouter;
    private final SmsProviderRegistry providerRegistry;

    // ========== SMS Individual ==========

    @Override
    @CircuitBreaker(name = SMS_SERVICE, fallbackMethod = "fallbackSendSms")
    @Retry(name = SMS_SERVICE)
    public SMSResult send(SendSMSCommand command) {
        return messageOrchestrator.sendSms(command);
    }

    @Override
    @CircuitBreaker(name = SMS_SERVICE, fallbackMethod = "fallbackSendSmsWithType")
    @Retry(name = SMS_SERVICE)
    public SMSResult sendWithType(SmsTypeRequest request) {
        return messageOrchestrator.sendSmsWithType(request);
    }

    // ========== OTP ==========

    @Override
    @CircuitBreaker(name = OTP_SERVICE, fallbackMethod = "fallbackOtpResult")
    @Retry(name = OTP_SERVICE)
    public OtpSendResult send(SendOtpRequest request) {
        return messageOrchestrator.sendOtp(request);
    }

    @Override
    public OtpSendResult sendAutoGenerate(String phoneNumber, String message, Integer expirationMinutes) {
        String otpCode = messageOrchestrator.generateOtpCode();

        var request = SendOtpRequest.builder()
                .phoneNumber(phoneNumber)
                .otpCode(otpCode)
                .message(message != null ? message : "Tu código de verificación es: {OTP}")
                .expirationMinutes(expirationMinutes != null ? expirationMinutes : 5)
                .referenceId("AUTO-" + System.currentTimeMillis())
                .build();

        return send(request);
    }

    @Override
    public OtpSendResult sendWithDefaultMessage(String phoneNumber) {
        return sendAutoGenerate(phoneNumber, "Tu código de verificación es: {OTP}. Válido por 5 minutos.", 5);
    }

    // ========== Batch ==========

    @Override
    @CircuitBreaker(name = SMS_SERVICE, fallbackMethod = "fallbackBatchResult")
    @Retry(name = SMS_SERVICE)
    public SmsBatchResult send(SendSmsBatchCommand command) {
        return messageOrchestrator.sendBatch(command);
    }

    // ========== Provider Management ==========

    @Override
    public String getCurrentProvider() {
        var provider = providerRouter.getCurrentProvider();
        return provider != null ? provider.getProviderName() : "No Provider";
    }

    @Override
    public void switchProvider(SmsProvider provider) {
        log.info("[SMS] Cambiando proveedor a: {}", provider);
        providerRouter.switchProvider(provider);
    }

    @Override
    public Map<SmsProvider, Boolean> checkProvidersAvailability() {
        // Usar registry para obtener disponibilidad real con health status
        Map<SmsProvider, Boolean> availability = new java.util.HashMap<>();
        providerRegistry.getAllHealthStatuses().forEach((type, status) ->
                availability.put(type, status.isAvailable() && status.isHealthy())
        );
        return availability;
    }

    @Override
    public SmsProvider getBestAvailableProvider() {
        var bestList = providerRegistry.getByPerformance();
        return !bestList.isEmpty() ? bestList.get(0).getProviderType() : SmsProvider.MOCK;
    }

    @Override
    public String getProviderInfo() {
        var stats = providerRouter.getStats();
        return String.format("Strategy: %s, Primary: %s, Available: %d/%d",
                stats.getCurrentStrategy(),
                stats.getPrimaryProvider().getName(),
                stats.getAvailableProviders(),
                stats.getRegisteredProviders());
    }

    @Override
    public String getProviderName() {
        var provider = providerRouter.getCurrentProvider();
        return provider != null ? provider.getProviderName() : "No Provider";
    }

    @Override
    public boolean isCurrentProviderAvailable() {
        var provider = providerRouter.getCurrentProvider();
        return provider != null && provider.isAvailable();
    }

    @Override
    public String getCurrentProviderInfo() {
        var stats = providerRouter.getStats();
        return String.format("Strategy: %s, Primary: %s, Available: %d/%d",
                stats.getCurrentStrategy(),
                stats.getPrimaryProvider().getName(),
                stats.getAvailableProviders(),
                stats.getRegisteredProviders());
    }

    @Override
    public boolean isServiceAvailable() {
        return !providerRegistry.getAvailable().isEmpty();
    }

    // ========== Fallback Methods ==========

    @SuppressWarnings("unused")
    private SMSResult fallbackSendSms(SendSMSCommand cmd, Exception ex) {
        log.error("[SMS] Circuit breaker fallback: {}", ex.getMessage());
        return SMSResult.failure("Servicio temporalmente no disponible", "CIRCUIT_OPEN");
    }

    @SuppressWarnings("unused")
    private SMSResult fallbackSendSmsWithType(SmsTypeRequest req, Exception ex) {
        log.error("[SMS] Circuit breaker fallback: {}", ex.getMessage());
        return SMSResult.failure("Servicio temporalmente no disponible", "CIRCUIT_OPEN");
    }

    @SuppressWarnings("unused")
    private OtpSendResult fallbackOtpResult(SendOtpRequest req, Exception ex) {
        log.error("[OTP] Circuit breaker fallback: {}", ex.getMessage());
        return OtpSendResult.failure("Servicio OTP temporalmente no disponible", "CIRCUIT_OPEN");
    }

    @SuppressWarnings("unused")
    private SmsBatchResult fallbackBatchResult(SendSmsBatchCommand cmd, Exception ex) {
        log.error("[Batch] Circuit breaker fallback: {}", ex.getMessage());
        return SmsBatchResult.failure("Servicio batch temporalmente no disponible", "CIRCUIT_OPEN");
    }

    @SuppressWarnings("unused")
    private SmsCampaignOperationResult fallbackCampaignResult(CreateSmsCampaignCommand cmd, Exception ex) {
        log.error("[Campaign] Circuit breaker fallback: {}", ex.getMessage());
        return SmsCampaignOperationResult.failure("Servicio de campañas temporalmente no disponible", "CIRCUIT_OPEN");
    }

    @SuppressWarnings("unused")
    private SmsCampaignResultQuery fallbackCampaignQuery(String campaignId, Exception ex) {
        log.error("[Campaign] Circuit breaker fallback para resultados: {}", ex.getMessage());
        return null;
    }
}
