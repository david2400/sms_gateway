package com.essenza.sms_gateway.modules.SMSService.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Value Object: Resultado del envío de SMS
 */
@Value
@Builder
public class SMSResult {
    
    boolean success;
    String messageId;
    String providerMessageId;
    int segmentCount;
    String errorMessage;
    String errorCode;
    LocalDateTime timestamp;
    long durationMs;
    
    /**
     * Crea un resultado exitoso
     */
    public static SMSResult success(String messageId, String providerMessageId, 
                                    int segmentCount, long durationMs) {
        return SMSResult.builder()
            .success(true)
            .messageId(messageId)
            .providerMessageId(providerMessageId)
            .segmentCount(segmentCount)
            .timestamp(LocalDateTime.now())
            .durationMs(durationMs)
            .build();
    }
    
    /**
     * Crea un resultado fallido
     */
    public static SMSResult failure(String errorMessage, String errorCode) {
        return SMSResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .errorCode(errorCode)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
