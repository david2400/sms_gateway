package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.CreateSmsCampaignCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.SmsCampaignResultQuery;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.OtpSendResult;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.SendOtpRequest;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendInalambriaBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSMSCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSmsBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SmsTypeRequest;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.inalambria.InalambriaAccountOrchestrator;
import com.essenza.sms_gateway.modules.SMSService.domain.model.*;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.SmsProviderPort;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.SmsInalambriaPort;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.config.SmsProvidersProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Provider unificado para Inalambria que actúa como fachada limpia
 * 
 * Principio de Arquitectura Limpia:
 * - Este provider representa la lógica de negocio (capa interna)
 * - El adapter aísla la implementación técnica de la API externa (capa externa)
 * - No hay contaminación: el provider no sabe de HTTP, headers, o detalles de API
 * 
 * Flujo: Lógica de Negocio → Provider → Adapter → Mundo Exterior
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.providers.inalambria.enabled", havingValue = "true")
public class InalambriaSmsProvider implements SmsProviderPort {

    private SmsProviderConfig config;
    private final InalambriaAccountOrchestrator orchestrator;
    private final SmsInalambriaPort inalambriaPort; // Puerto limpio hacia el mundo exterior
    private final SmsProvidersProperties smsProviderProperties;
    
    public InalambriaSmsProvider(
        InalambriaAccountOrchestrator orchestrator,
       SmsInalambriaPort inalambriaPort,
       SmsProvidersProperties smsProviderProperties) {
        this.orchestrator = orchestrator;
        this.inalambriaPort = inalambriaPort;
        this.smsProviderProperties = smsProviderProperties;
    }
    

    @Override
    public SmsProvider getProviderType() {
        return SmsProvider.INALAMBRIA;
    }

    @Override
    public void configure(SmsProviderConfig config) {
        this.config = config;
        log.info("[INALAMBRIA] Configurado con usuario: {}", config.getUsername());
    }

    @Override
    public boolean isAvailable() {
        // Lógica de negocio: verificar disponibilidad del servicio
        return inalambriaPort.isAvailable();
    }

    @Override
    public String getProviderName() {
        // Lógica de negocio: obtener nombre del proveedor
        return inalambriaPort.getProviderName();
    }

    @Override
    public SmsCampaignOperationResult createCampaign(CreateSmsCampaignCommand command) {
        log.info("[INALAMBRIA] Creando campaña: {}", command.getName());

        try {
            // Inalambria no tiene concepto de campañas como Go4Clients
            // Simulamos creando una campaña virtual
            String campaignId = "INALAMBRIA-" + UUID.randomUUID().toString().substring(0, 8);

            return SmsCampaignOperationResult.success(campaignId, campaignId);

        } catch (Exception e) {
            log.error("[INALAMBRIA] Error creando campaña: {}", e.getMessage(), e);
            return SmsCampaignOperationResult.failure("Error creando campaña", "INALAMBRIA_ERROR");
        }
    }

    @Override
    public SmsBatchResult sendBatch(SendSmsBatchCommand command) {
        log.info("[INALAMBRIA] Procesando batch de {} destinatarios", command.getDestinations().size());

        try {
            // Lógica de negocio: validar y preparar el envío
            if (!command.isValid()) {
                return SmsBatchResult.failure("Comando inválido", "VALIDATION_ERROR");
            }

            // Usar el orquestador para transformar el comando de negocio a comando específico
            SendInalambriaBatchCommand inalambriaCommand = orchestrator.transformBatchCommand(command);
            
            // El puerto se encarga de la comunicación externa (limpio y aislado)
            SmsBatchResult result = inalambriaPort.sendBatch(inalambriaCommand);
            
            // Lógica de negocio: asociar metadatos del negocio
            if (result.isSuccess() && command.getCampaignId() != null) {
                return result.toBuilder()
                        .campaignId(command.getCampaignId())
                        .build();
            }
            
            return result;

        } catch (Exception e) {
            log.error("[INALAMBRIA] Error en lógica de batch: {}", e.getMessage(), e);
            return SmsBatchResult.failure("Error procesando batch", "BUSINESS_ERROR");
        }
    }

    @Override
    public OtpSendResult sendOtp(SendOtpRequest request) {
        log.info("[INALAMBRIA] Procesando OTP para {}", request.getPhoneNumber());

        try {
            // Lógica de negocio: validar request de negocio
            if (!request.isValid()) {
                return OtpSendResult.failure("Request inválido", "VALIDATION_ERROR");
            }

            // Usar el orquestador para transformar el comando de negocio a comando específico
            SendInalambriaBatchCommand otpCommand = orchestrator.transformOtpCommand(request);
            
            // El puerto se encarga de la comunicación externa (limpio y aislado)
            SmsBatchResult batchResult = inalambriaPort.sendBatch(otpCommand);

            if (batchResult.isSuccess()) {
                log.info("[INALAMBRIA] ✅ OTP procesado exitosamente");
                // Lógica de negocio: transformar resultado de batch a resultado OTP
                return OtpSendResult.success(
                        batchResult.getProviderReference(),
                        batchResult.getProviderReference(),
                        request.getPhoneNumber(),
                        request.getOtpCode(),
                        request.getExpirationMinutes()
                );
            } else {
                return OtpSendResult.failure(batchResult.getErrorMessage(), batchResult.getErrorCode());
            }

        } catch (Exception e) {
            log.error("[INALAMBRIA] Error en lógica de OTP: {}", e.getMessage(), e);
            return OtpSendResult.failure("Error procesando OTP", "BUSINESS_ERROR");
        }
    }

    @Override
    public SmsCampaignResultQuery getCampaignResults(String campaignId) {
        log.info("[INALAMBRIA] Obteniendo resultados de campaña: {}", campaignId);

        try {
            // Inalambria no tiene endpoint específico para resultados de campaña
            // Simulamos resultados básicos
            return SmsCampaignResultQuery.builder()
                    .campaignId(campaignId)
                    .build();

        } catch (Exception e) {
            log.error("[INALAMBRIA] Error obteniendo resultados: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Envía un SMS individual usando la lógica de negocio limpia
     */
    @Override
    public SMSResult sendSms(SendSMSCommand command) {
        log.info("[INALAMBRIA] Procesando SMS individual para: {}", command.getTo());

        try {
            // Lógica de negocio: validar comando de negocio
            if (!command.isValid()) {
                return SMSResult.failure("Comando inválido", "VALIDATION_ERROR");
            }

            // Convertir a batch de un solo destinatario (lógica de negocio)
            SendSmsBatchCommand batchCommand = SendSmsBatchCommand.builder()
                    .campaignId("SMS-" + System.currentTimeMillis())
                    .destinations(List.of(
                        com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.DestinatarioDto.builder()
                            .phoneNumber(command.getTo())
                            .countryCode(command.getCountryCode() != null ? command.getCountryCode() : "57")
                            .build()
                    ))
                    .message(command.getMessage())
                    .build();

            // Reutilizar la lógica de batch existente
            SmsBatchResult batchResult = sendBatch(batchCommand);

            // Lógica de negocio: transformar resultado de batch a resultado SMS
            if (batchResult.isSuccess()) {
                return SMSResult.success(
                        batchResult.getProviderReference(),
                        batchResult.getProviderReference(),
                        1,
                        System.currentTimeMillis()
                );
            } else {
                return SMSResult.failure(batchResult.getErrorMessage(), batchResult.getErrorCode());
            }

        } catch (Exception e) {
            log.error("[INALAMBRIA] Error en lógica de SMS: {}", e.getMessage(), e);
            return SMSResult.failure("Error procesando SMS", "BUSINESS_ERROR");
        }
    }

    /**
     * Envía un SMS con tipo específico usando la lógica de negocio limpia
     */
    @Override
    public SMSResult sendSmsWithType(SmsTypeRequest request) {
        log.info("[INALAMBRIA] Procesando SMS tipo {} para: {}", request.getSmsType(), request.getTo());

        try {
            // Lógica de negocio: validar request de negocio
            if (!request.isValid()) {
                return SMSResult.failure("Request inválido", "VALIDATION_ERROR");
            }

            // Usar el orquestador para transformar el comando de negocio a comando específico
            SendInalambriaBatchCommand inalambriaCommand = orchestrator.transformSmsTypeRequest(request);
            
            // El puerto se encarga de la comunicación externa (limpio y aislado)
            SmsBatchResult batchResult = inalambriaPort.sendBatch(inalambriaCommand);

            // Lógica de negocio: transformar resultado de batch a resultado SMS
            if (batchResult.isSuccess()) {
                return SMSResult.success(
                        batchResult.getProviderReference(),
                        batchResult.getProviderReference(),
                        1,
                        System.currentTimeMillis()
                );
            } else {
                return SMSResult.failure(batchResult.getErrorMessage(), batchResult.getErrorCode());
            }

        } catch (Exception e) {
            log.error("[INALAMBRIA] Error en lógica de SMS con tipo: {}", e.getMessage(), e);
            return SMSResult.failure("Error procesando SMS", "BUSINESS_ERROR");
        }
    }

    @Override
    public ProviderCapabilities getCapabilities() {
        return ProviderCapabilities.builder()
                .supportsCampaigns(false) // Inalambria no tiene campañas como tal
                .supportsBatch(true)
                .supportsOtp(true)
                .supportsTemplates(true)
                .supportsScheduledMessages(true)
                .supportsUnicode(false)
                .supportsDeliveryReports(false)
                .supportsCustomSender(false)
                .maxBatchSize(100)
                .maxMessageLength(160)
                .maxRecipientsPerBatch(100)
                .build();
    }
}
