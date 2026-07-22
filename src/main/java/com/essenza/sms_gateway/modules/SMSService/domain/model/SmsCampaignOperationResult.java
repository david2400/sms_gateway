package com.essenza.sms_gateway.modules.SMSService.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Value object representing the outcome of a campaign-level operation
 * (create campaign, update metadata, etc.).
 */
@Value
@Builder(toBuilder = true)
public class SmsCampaignOperationResult {

    boolean success;
    String campaignId;
    String providerReference;
    String status;
    String errorMessage;
    String errorCode;
    LocalDateTime timestamp;
    @Singular("metadataEntry")
    Map<String, Object> metadata;

    public static SmsCampaignOperationResult success(String campaignId, String providerReference) {
        return SmsCampaignOperationResult.builder()
                .success(true)
                .campaignId(campaignId)
                .providerReference(providerReference)
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SmsCampaignOperationResult failure(String errorMessage, String errorCode) {
        return SmsCampaignOperationResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .status("FAILED")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
