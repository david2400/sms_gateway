package com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Command: create a Go4Clients SMS campaign.
 */
@Value
@Builder(toBuilder = true)
@Schema(
        description = "Comando para crear campaña SMS"
)
public class CreateSmsCampaignCommand {

    @Schema(description = "Nombre de la campaña", example = "Campaña Recordatorios")
    String name;
    
    @Schema(description = "Remitente de la campaña", example = "Comfenalco")
    String sender;

    public boolean isValid() {
        return name != null && !name.isBlank()
            && sender != null && !sender.isBlank();
    }
}
