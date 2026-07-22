package com.essenza.sms_gateway.modules.SMSService.domain.ports;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.CreateSmsCampaignCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.SmsCampaignResultQuery;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.OtpSendResult;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.SendOtpRequest;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSMSCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSmsBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SmsTypeRequest;
import com.essenza.sms_gateway.modules.SMSService.domain.model.*;

import java.util.List;

/**
 * Interfaz unificada para todos los proveedores de SMS
 * Permite cambiar entre proveedores solo con configuración
 */
public interface SmsProviderPort {
    
    /**
     * Obtiene el tipo de proveedor
     */
    SmsProvider getProviderType();
    
    /**
     * Verifica si el proveedor está disponible
     */
    boolean isAvailable();
    
    /**
     * Obtiene el nombre del proveedor
     */
    String getProviderName();
    
    /**
     * Configura el proveedor con las credenciales y parámetros
     */
    void configure(SmsProviderConfig config);
    
    /**
     * Crea una campaña SMS
     */
    SmsCampaignOperationResult createCampaign(CreateSmsCampaignCommand command);
    
    /**
     * Envía un batch de SMS
     */
    SmsBatchResult sendBatch(SendSmsBatchCommand command);
    
    /**
     * Envía un mensaje OTP directamente
     */
    OtpSendResult sendOtp(SendOtpRequest request);
    
    /**
     * Obtiene resultados de una campaña
     */
    SmsCampaignResultQuery getCampaignResults(String campaignId);

    /**
     * Envía un SMS individual. Por defecto se apoya en {@link #sendBatch},
     * pero cada proveedor puede sobreescribirlo con su propia lógica optimizada
     * (ej. {@code InalambriaSmsProvider}). Esto evita que las capas superiores
     * necesiten conocer el tipo concreto del proveedor (instanceof).
     */
    default SMSResult sendSms(SendSMSCommand command) {
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

        SmsBatchResult batchResult = sendBatch(batchCommand);

        return batchResult.isSuccess()
                ? SMSResult.success("SMS-" + System.currentTimeMillis(), batchResult.getProviderReference(), 1, System.currentTimeMillis())
                : SMSResult.failure(batchResult.getErrorMessage(), batchResult.getErrorCode());
    }

    /**
     * Envía un SMS indicando su tipo de negocio (OTP, HABEAS_DATA, TEMPLATE, etc).
     * Por defecto se convierte a un envío estándar; los proveedores que soportan
     * cuentas/rutas distintas por tipo (ej. Inalambria) pueden sobreescribirlo.
     */
    default SMSResult sendSmsWithType(SmsTypeRequest request) {
        return sendSms(request.toSendSMSCommand());
    }
    
    /**
     * Valida un número de teléfono para este proveedor
     */
    default boolean validatePhoneNumber(String phoneNumber) {
        return phoneNumber != null && 
               phoneNumber.matches("\\+?\\d{10,15}") &&
               !phoneNumber.startsWith("000") &&
               !phoneNumber.startsWith("123");
    }
    
    /**
     * Genera un código OTP según las reglas del proveedor
     */
    default String generateOtpCode(int length) {
        if (length < 4 || length > 8) {
            length = 6; // Default
        }
        
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append((int) (Math.random() * 10));
        }
        return otp.toString();
    }
    
    /**
     * Obtiene las capacidades del proveedor
     */
    default ProviderCapabilities getCapabilities() {
        return ProviderCapabilities.builder()
                .supportsCampaigns(true)
                .supportsBatch(true)
                .supportsOtp(true)
                .supportsTemplates(true)
                .supportsScheduledMessages(true)
                .maxBatchSize(1000)
                .maxMessageLength(160)
                .build();
    }
}
