package com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Alta o modificación de plantilla SMS. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Plantilla SMS. Si no se envían 'variables' se crean a partir del contenido (TEXTO, obligatorias)")
public class PlantillaRequest {

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_\\-]{2,50}", message = "Código: 2 a 50 letras, números, _ o -")
    @Schema(description = "Código único (se guarda en mayúsculas). No se puede cambiar", example = "RECORDATORIO_CITA")
    private String codigo;

    @NotBlank
    @Size(max = 100)
    @Schema(example = "Recordatorio de cita")
    private String nombre;

    @Size(max = 255)
    private String descripcion;

    @Size(max = 30)
    @Schema(description = "Tipo de mensaje por defecto (código de SmsTipoMensaje). Opcional", example = "NORMAL")
    private String tipoMensaje;

    @NotBlank
    @Size(max = 1600)
    @Schema(example = "Hola {{nombre}}, le recordamos su cita el {{fecha}} a las {{hora}} en {{sede}}. Comfenalco Quindío")
    private String contenido;

    @Valid
    @Size(max = 50)
    private List<PlantillaVariableRequest> variables;
}
