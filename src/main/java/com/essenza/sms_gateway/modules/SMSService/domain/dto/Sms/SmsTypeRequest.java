package com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para enviar SMS especificando el tipo de mensaje
 */
@Data
@Builder
@Schema(
        description = "Request para enviar SMS con tipo específico"
)
public class SmsTypeRequest {
    
    @NotBlank(message = "El remitente es obligatorio")
    @Schema(description = "Remitente del SMS", example = "Comfenalco")
    private String from;
    
    @NotBlank(message = "El destinatario es obligatorio")
    @Schema(description = "Número de teléfono destinatario (10 dígitos, sin prefijo país)", example = "3157132338")
    private String to;
    
    @Schema(description = "Código de país del número (ej: 57 para Colombia)", example = "57")
    private String countryCode = "57";
    
    @NotBlank(message = "El mensaje es obligatorio")
    @Schema(description = "Contenido del mensaje SMS", example = "Su cita ha sido programada para mañana a las 10:00 AM. Por favor llegue 15 minutos antes.")
    private String message;
    
    /**
     * Tipo de SMS: OTP, HABEAS_DATA, TEMPLATE, PERSONALIZED, NORMAL
     */
    @Schema(description = "Tipo de mensaje SMS", allowableValues = {"OTP", "OTP", "HABEAS_DATA", "TEMPLATE", "PERSONALIZED"}, example = "NORMAL")
    private SmsMessageType smsType = SmsMessageType.OTP;
    
    /**
     * Número de destinatarios (para determinar si es masivo)
     */
    @Schema(description = "Número de destinatarios", example = "1")
    private Integer recipientCount = 1;
    
    /**
     * Convierte a SendSMSCommand para compatibilidad
     */
    public SendSMSCommand toSendSMSCommand() {
        return SendSMSCommand.builder()
                .from(from)
                .to(to)
                .message(message)
                .build();
    }
    
    /**
     * Valida que el comando sea válido
     */
    public boolean isValid() {
        return from != null && !from.isBlank()
            && to != null && !to.isBlank()
            && message != null && !message.isBlank()
            && smsType != null;
    }
}
