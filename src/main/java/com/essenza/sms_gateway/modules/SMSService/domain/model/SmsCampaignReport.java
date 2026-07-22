package com.essenza.sms_gateway.modules.SMSService.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Value object for analytics/metrics returned by Go4Clients.
 */
@Value
@Builder(toBuilder = true)
public class SmsCampaignReport {

    String campaignId;
    String status;
    int delivered;
    int failed;
    int pending;
    int total;
    LocalDateTime generatedAt;
    @Singular("metadataEntry")
    Map<String, Object> metadata;
}
