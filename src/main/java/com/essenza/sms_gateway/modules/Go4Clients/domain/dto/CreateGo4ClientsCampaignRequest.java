package com.essenza.sms_gateway.modules.Go4Clients.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Creación de campaña en Go4Clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Crear campaña SMS en Go4Clients")
public class CreateGo4ClientsCampaignRequest {

    @NotBlank(message = "El nombre de la campaña es obligatorio")
    @Size(max = 100)
    @Schema(description = "Nombre de la campaña", example = "Recordatorio citas octubre")
    private String name;

    @Schema(description = "Remitente; si no se envía se usa el de la cuenta o RANDOM", example = "RANDOM")
    private String sender;

    @Schema(description = "Código de la cuenta (sms_cuenta.codigo); si no se envía se usa la primera cuenta activa de Go4Clients",
            example = "GO4CLIENTS_PRINCIPAL")
    private String cuenta;
}
