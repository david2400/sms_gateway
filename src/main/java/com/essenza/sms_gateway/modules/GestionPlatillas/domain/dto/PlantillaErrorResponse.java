package com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error de la administración de plantillas")
public class PlantillaErrorResponse {
    @Schema(example = "VALIDACION")
    private String codigo;
    private String mensaje;
    private List<String> detalles;
    private LocalDateTime fecha;
}
