package com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Command: generate and send an OTP token via SMS (Inalambria).
 */
@Value
@Builder(toBuilder = true)
@Schema(
        description = "Comando para generar y enviar token OTP vía SMS"
)
public class GenerateOtpTokenCommand {

    @Schema(description = "Número de teléfono del cliente (10 dígitos, sin prefijo país)", example = "3157132338")
    String phoneNumber;
    
    @Schema(description = "Código de país del número (ej: 57 para Colombia)", example = "57")
    String countryCode = "57";
    
    @Schema(description = "Nombre del cliente", example = "Juan Pérez")
    String customerName;
    
    @Schema(description = "Número de documento del cliente", example = "123456789")
    String documentNumber;
    
    @Schema(description = "Nombre de la campaña", example = "Campaña OTP")
    String campaignName;
    
    @Schema(description = "Plantilla del mensaje", example = "Su código es: {TOKEN}")
    String messageTemplate;
    
    @Schema(description = "Fecha programada para el envío", example = "2024-01-20T08:00:00")
    LocalDateTime scheduledAt;

    /**
     * Optional override of digits length (default 6).
     */
    @Schema(description = "Longitud del token (default 6)", example = "6")
    int tokenLength;

    public boolean isValid() {
        return phoneNumber != null && !phoneNumber.isBlank()
            && customerName != null && !customerName.isBlank()
            && documentNumber != null && !documentNumber.isBlank();
    }
}
