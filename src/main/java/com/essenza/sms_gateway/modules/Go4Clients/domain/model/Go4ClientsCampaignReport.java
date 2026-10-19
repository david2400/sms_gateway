package com.essenza.sms_gateway.modules.Go4Clients.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Reporte de una campaña según la analítica de Go4Clients.
 */
@Value
@Builder
@Schema(description = "Reporte de campaña Go4Clients")
public class Go4ClientsCampaignReport {

    boolean success;
    String campaignId;
    int total;
    int delivered;
    int failed;
    int pending;
    Integer httpStatus;
    String errorMessage;
}
