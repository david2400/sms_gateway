package com.essenza.sms_gateway.modules.SMSService.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * Capacidades de un proveedor de SMS
 */
@Data
@Builder
public class ProviderCapabilities {
    
    private boolean supportsCampaigns;
    private boolean supportsBatch;
    private boolean supportsOtp;
    private boolean supportsTemplates;
    private boolean supportsScheduledMessages;
    private boolean supportsUnicode;
    private boolean supportsDeliveryReports;
    private boolean supportsCustomSender;
    private int maxBatchSize;
    private int maxMessageLength;
    private int maxRecipientsPerBatch;
    private String supportedCountries;
    private String timeZone;
    
    /**
     * Verifica si soporta una característica específica
     */
    public boolean supports(ProviderFeature feature) {
        switch (feature) {
            case CAMPAIGNS:
                return supportsCampaigns;
            case BATCH:
                return supportsBatch;
            case OTP:
                return supportsOtp;
            case TEMPLATES:
                return supportsTemplates;
            case SCHEDULED_MESSAGES:
                return supportsScheduledMessages;
            case UNICODE:
                return supportsUnicode;
            case DELIVERY_REPORTS:
                return supportsDeliveryReports;
            case CUSTOM_SENDER:
                return supportsCustomSender;
            default:
                return false;
        }
    }
    
    /**
     * Enumeración de características posibles
     */
    public enum ProviderFeature {
        CAMPAIGNS,
        BATCH,
        OTP,
        TEMPLATES,
        SCHEDULED_MESSAGES,
        UNICODE,
        DELIVERY_REPORTS,
        CUSTOM_SENDER
    }
}
