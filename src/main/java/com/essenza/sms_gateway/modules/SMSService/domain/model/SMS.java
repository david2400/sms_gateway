package com.essenza.sms_gateway.modules.SMSService.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Entidad de dominio: SMS
 */
@Value
@Builder
public class SMS {
    
    String id;
    PhoneNumber from;
    PhoneNumber to;
    String message;
    SMSPriority priority;
    LocalDateTime scheduledAt;
    
    private static final int MAX_SMS_LENGTH = 1600;
    private static final int SINGLE_SMS_LENGTH = 160;
    private static final int UNICODE_SMS_LENGTH = 70;
    
    /**
     * Valida que el SMS sea válido
     */
    public boolean isValid() {
        return from != null && from.isValid()
            && to != null && to.isValid()
            && message != null && !message.isBlank()
            && message.length() <= MAX_SMS_LENGTH;
    }
    
    /**
     * Calcula el número de segmentos del SMS
     */
    public int getSegmentCount() {
        if (message == null) return 0;
        
        int length = message.length();
        boolean isUnicode = containsUnicode(message);
        int segmentLength = isUnicode ? UNICODE_SMS_LENGTH : SINGLE_SMS_LENGTH;
        
        if (length <= segmentLength) {
            return 1;
        }
        
        // Para SMS concatenados, el límite es menor (153 o 67)
        int concatenatedLength = isUnicode ? 67 : 153;
        return (int) Math.ceil((double) length / concatenatedLength);
    }
    
    /**
     * Verifica si el mensaje contiene caracteres Unicode
     */
    private boolean containsUnicode(String text) {
        return text.chars().anyMatch(c -> c > 127);
    }
    
    /**
     * Verifica si es un SMS largo (múltiples segmentos)
     */
    public boolean isLongSMS() {
        return getSegmentCount() > 1;
    }
    
    /**
     * Obtiene el tamaño del mensaje
     */
    public int getLength() {
        return message != null ? message.length() : 0;
    }
}
