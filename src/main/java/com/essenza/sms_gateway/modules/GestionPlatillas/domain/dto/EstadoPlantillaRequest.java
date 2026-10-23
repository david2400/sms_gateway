package com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Activar o desactivar: eliminado=true desactiva, eliminado=false activa (igual que el resto de SmsAdmin)")
public class EstadoPlantillaRequest {
    @NotNull
    @Schema(example = "true")
    private Boolean eliminado;
}
