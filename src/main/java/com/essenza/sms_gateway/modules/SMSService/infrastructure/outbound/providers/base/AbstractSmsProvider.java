package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.base;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.CreateSmsCampaignCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.SmsCampaignResultQuery;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.OtpSendResult;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.SendOtpRequest;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSmsBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.model.*;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.SmsProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * Clase base abstracta para implementar nuevos proveedores SMS
 * Proporciona funcionalidad común y un contrato claro para nuevos proveedores
 * 
 * PARA AGREGAR UN NUEVO PROVEEDOR:
 * 1. Extender esta clase
 * 2. Implementar los métodos abstractos
 * 3. Anotar con @Component
 * 4. Configurar credenciales en application.yml
 */
@Slf4j
public abstract class AbstractSmsProvider implements SmsProviderPort {

    protected final RestTemplate restTemplate;
    protected SmsProviderConfig config;
    protected boolean configured = false;

    protected AbstractSmsProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void configure(SmsProviderConfig config) {
        this.config = config;
        this.configured = true;
        log.info("[{}] Configurado con baseUrl: {}", getProviderName(), config.getBaseUrl());
        onConfigure(config);
    }

    /**
     * Hook para configuración adicional específica del proveedor
     */
    protected void onConfigure(SmsProviderConfig config) {
        // Override en subclases si se necesita configuración adicional
    }

    @Override
    public boolean isAvailable() {
        if (!configured || config == null || !config.isEnabled()) {
            return false;
        }
        return checkHealth();
    }

    /**
     * Verifica la salud del proveedor (conexión, credenciales, etc.)
     */
    protected boolean checkHealth() {
        // Por defecto, retorna true si está configurado
        // Override para implementar health check real
        return configured;
    }

    @Override
    public SmsCampaignOperationResult createCampaign(CreateSmsCampaignCommand command) {
        log.info("[{}] Creando campaña: {}", getProviderName(), command.getName());
        
        if (!validateCommand(command)) {
            return SmsCampaignOperationResult.failure("Comando inválido", "VALIDATION_ERROR");
        }
        
        try {
            return doCreateCampaign(command);
        } catch (Exception e) {
            log.error("[{}] Error creando campaña: {}", getProviderName(), e.getMessage(), e);
            return SmsCampaignOperationResult.failure("Error: " + e.getMessage(), "PROVIDER_ERROR");
        }
    }

    @Override
    public SmsBatchResult sendBatch(SendSmsBatchCommand command) {
        log.info("[{}] Enviando batch de {} mensajes", getProviderName(), command.getDestinations().size());
        
        if (!validateCommand(command)) {
            return SmsBatchResult.failure("Comando inválido", "VALIDATION_ERROR");
        }
        
        try {
            return doSendBatch(command);
        } catch (Exception e) {
            log.error("[{}] Error enviando batch: {}", getProviderName(), e.getMessage(), e);
            return SmsBatchResult.failure("Error: " + e.getMessage(), "PROVIDER_ERROR");
        }
    }

    @Override
    public OtpSendResult sendOtp(SendOtpRequest request) {
        log.info("[{}] Enviando OTP a: {}", getProviderName(), maskPhone(request.getPhoneNumber()));
        
        if (!validateRequest(request)) {
            return OtpSendResult.failure("Request inválido", "VALIDATION_ERROR");
        }
        
        try {
            return doSendOtp(request);
        } catch (Exception e) {
            log.error("[{}] Error enviando OTP: {}", getProviderName(), e.getMessage(), e);
            return OtpSendResult.failure("Error: " + e.getMessage(), "PROVIDER_ERROR");
        }
    }

    @Override
    public SmsCampaignResultQuery getCampaignResults(String campaignId) {
        log.info("[{}] Obteniendo resultados de campaña: {}", getProviderName(), campaignId);
        
        try {
            return doGetCampaignResults(campaignId);
        } catch (Exception e) {
            log.error("[{}] Error obteniendo resultados: {}", getProviderName(), e.getMessage(), e);
            return null;
        }
    }

    // ========== Métodos abstractos que deben implementar los proveedores ==========

    /**
     * Implementación específica para crear campaña
     */
    protected abstract SmsCampaignOperationResult doCreateCampaign(CreateSmsCampaignCommand command);

    /**
     * Implementación específica para enviar batch
     */
    protected abstract SmsBatchResult doSendBatch(SendSmsBatchCommand command);

    /**
     * Implementación específica para enviar OTP
     */
    protected abstract OtpSendResult doSendOtp(SendOtpRequest request);

    /**
     * Implementación específica para obtener resultados
     */
    protected abstract SmsCampaignResultQuery doGetCampaignResults(String campaignId);

    // ========== Métodos utilitarios ==========

    /**
     * Crea headers HTTP con autenticación básica
     */
    protected HttpHeaders createBasicAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (config.getUsername() != null && config.getPassword() != null) {
            headers.setBasicAuth(config.getUsername(), config.getPassword());
        }
        return headers;
    }

    /**
     * Crea headers HTTP con API Key
     */
    protected HttpHeaders createApiKeyHeaders(String headerName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (config.getApiKey() != null) {
            headers.set(headerName, config.getApiKey());
        }
        return headers;
    }

    /**
     * Crea una entidad HTTP con el payload y headers
     */
    protected <T> HttpEntity<T> createEntity(T payload, HttpHeaders headers) {
        return new HttpEntity<>(payload, headers);
    }

    /**
     * Enmascara número de teléfono para logs
     */
    protected String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }

    /**
     * Valida comando de campaña
     */
    protected boolean validateCommand(CreateSmsCampaignCommand command) {
        return command != null && command.isValid();
    }

    /**
     * Valida comando de batch
     */
    protected boolean validateCommand(SendSmsBatchCommand command) {
        return command != null && command.isValid();
    }

    /**
     * Valida request de OTP
     */
    protected boolean validateRequest(SendOtpRequest request) {
        return request != null && request.isValid();
    }

    /**
     * Genera un ID único para transacciones
     */
    protected String generateTransactionId() {
        return getProviderType().getCode() + "-" + System.currentTimeMillis();
    }
}
