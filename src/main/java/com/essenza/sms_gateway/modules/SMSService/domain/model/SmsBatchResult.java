package com.essenza.sms_gateway.modules.SMSService.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Value object representing the result of a batch SMS submission (campaign or provider specific).
 */
@Value
@Builder(toBuilder = true)
public class SmsBatchResult {

    boolean success;
    String campaignId;
    String batchId;
    String providerReference;
    int acceptedCount;
    int rejectedCount;
    String errorMessage;
    String errorCode;
    LocalDateTime timestamp;
    @Singular("metadataEntry")
    Map<String, Object> metadata;

    public static SmsBatchResult success(String campaignId, String batchId) {
        return SmsBatchResult.builder()
                .success(true)
                .campaignId(campaignId)
                .batchId(batchId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SmsBatchResult failure(String errorMessage, String errorCode) {
        return SmsBatchResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
