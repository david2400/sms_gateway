package com.essenza.sms_gateway.modules.Go4Clients.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Envío de mensajes dentro de una campaña existente de Go4Clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Enviar mensajes a los destinatarios de una campaña Go4Clients")
public class SendGo4ClientsCampaignMessagesRequest {

    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 1600)
    @Schema(description = "Texto del SMS", example = "Recuerde su cita de mañana a las 10:00 AM")
    private String message;

    @NotEmpty(message = "Debe enviar al menos un destinatario")
    @Size(max = 10000, message = "Máximo 10000 destinatarios por envío")
    @Valid
    private List<Go4ClientsDestinatarioDto> destinations;

    @Schema(description = "Prioridad Go4Clients", example = "HIGH")
    private String priority;

    @Schema(description = "Campos personalizados para landing pages (formato nativo de Go4Clients)")
    private Map<String, Map<String, String>> landingCustomFields;

    @Schema(description = "Código de la cuenta (sms_cuenta.codigo); opcional", example = "GO4CLIENTS_PRINCIPAL")
    private String cuenta;
}
