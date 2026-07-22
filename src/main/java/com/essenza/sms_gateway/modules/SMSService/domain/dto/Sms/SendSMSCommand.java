package com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms;

import com.essenza.sms_gateway.modules.SMSService.domain.model.PhoneNumber;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SMS;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SMSPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * DTO: Comando para enviar SMS
 */
@Value
@Builder
@Schema(
        description = "Comando para enviar SMS individual"
)
public class SendSMSCommand {
    
    @Schema(description = "Remitente del SMS", example = "Comfenalco")
    String from;
    
    @Schema(description = "Número de teléfono destinatario (10 dígitos, sin prefijo país)", example = "3157132338")
    String to;
    
    @Schema(description = "Código de país del número (ej: 57 para Colombia)", example = "57")
    String countryCode = "57";
    
    @Schema(description = "Contenido del mensaje SMS", example = "Su cita ha sido programada para mañana a las 10:00 AM. Por favor llegue 15 minutos antes.")
    String message;
    
    @Schema(description = "Prioridad del envío", allowableValues = {"LOW", "NORMAL", "HIGH"}, example = "NORMAL")
    SMSPriority priority;
    
    /**
     * Valida que el comando sea válido
     */
    public boolean isValid() {
        return from != null && !from.isBlank()
            && to != null && !to.isBlank()
            && message != null && !message.isBlank();
    }
    
    /**
     * Convierte el comando a SMS de dominio
     */
    public SMS toSMS() {
        return SMS.builder()
            .id(UUID.randomUUID().toString())
            .from(new PhoneNumber(from))
            .to(new PhoneNumber(to))
            .message(message)
            .priority(priority != null ? priority : SMSPriority.NORMAL)
            .build();
    }
    
    /**
     * Convierte el comando a SmsTypeRequest para usar con sendWithType()
     */
    public SmsTypeRequest toSmsTypeRequest() {
        // Normalizar el número: quitar +57 para Inalambria
        String normalizedTo = to;
        if (to != null && to.startsWith("+57")) {
            normalizedTo = to.substring(3); // Quitar +57
        }
        
        return SmsTypeRequest.builder()
            .from(from)
            .to(normalizedTo)
            .countryCode(countryCode != null ? countryCode : "57")
            .message(message)
            .smsType(SmsMessageType.OTP) // Usar OTP para que funcione con Inalambria
            .recipientCount(1)
            .build();
    }
}
