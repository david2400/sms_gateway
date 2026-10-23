package com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.VariableDataType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Variable que espera una plantilla (lo que el cliente debe enviar en {@code variables}). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaVariableInfo {
    @Schema(example = "fecha")
    private String nombre;
    @Schema(example = "FECHA")
    private VariableDataType tipoDato;
    private boolean obligatoria;
    private Integer longitudMax;
    private String patron;
    private String valorDefecto;
    private String descripcion;
}
