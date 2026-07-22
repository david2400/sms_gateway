package com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms;

import com.essenza.sms_gateway.modules.SMSService.domain.model.SMSPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Command: send a batch of SMS through Go4Clients campaigns.
 */
@Value
@Builder(toBuilder = true)
@Schema(
        description = "Command para enviar SMS masivo"
)
public class SendSmsBatchCommand {
    @Schema(description = "ID único de la campaña", example = "")
    String campaignId;
    
    @Schema(description = "Mensaje del SMS", example = "Recordatorio: Su cita médica está programada para mañana. Por favor confirmar asistencia.")
    String message;
    
    @Schema(description = "Prioridad del envío", allowableValues = {"LOW", "NORMAL", "HIGH"}, example = "NORMAL")
    SMSPriority priority;
    
    @Schema(description = "Lista de destinatarios con número de teléfono y código de país", example = "[{\"phoneNumber\": \"3157132338\", \"countryCode\": \"57\"}]")
    @Singular("destination")
    List<DestinatarioDto> destinations;
    
    @Schema(description = "Campos personalizados para landing pages",example = "[]")
    Map<String, Map<String, String>> landingCustomFields;
    
    @Schema(description = "Fecha programada para el envío", example = "2024-01-20T08:00:00")
    LocalDateTime scheduledDate;

    public boolean isValid() {
        return campaignId != null && !campaignId.isBlank()
            && message != null && !message.isBlank()
            && destinations != null && !destinations.isEmpty();
    }
}
