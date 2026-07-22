package com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Query object to fetch analytics for a Go4Clients campaign.
 */
@Value
@Builder(toBuilder = true)
@Schema(
        description = "Query para obtener resultados de campaña SMS"
)
public class SmsCampaignResultQuery {

    @Schema(description = "ID de la campaña", example = "CAMP-12345")
    String campaignId;

    public boolean isValid() {
        return campaignId != null && !campaignId.isBlank();
    }
}
