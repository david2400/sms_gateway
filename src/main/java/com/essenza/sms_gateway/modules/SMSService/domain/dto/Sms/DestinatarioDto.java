package com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * DTO para representar un destinatario con número de teléfono y código de país
 */
@Data
@Builder
@Schema(
        description = "Destinatario con número de teléfono y código de país"
)
public class DestinatarioDto {
    
    @Schema(description = "Número de teléfono destinatario (10 dígitos, sin prefijo país)", example = "3157132338")
    private String phoneNumber;
    
    @Schema(description = "Código de país del número (ej: 57 para Colombia)", example = "57")
    private String countryCode = "57";
}
