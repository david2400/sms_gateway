package com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.VariableDataType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Declaración de una variable de plantilla. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Variable de la plantilla: se usa en el contenido como {{nombre}}")
public class PlantillaVariableRequest {

    @NotBlank
    @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,49}", message = "Nombre: letras, números y _, empezando por letra (máx. 50)")
    @Schema(example = "fecha")
    private String nombre;

    @Schema(description = "TEXTO (defecto), ENTERO, DECIMAL, MONEDA, FECHA, HORA, URL", example = "FECHA")
    private VariableDataType tipoDato;

    @Schema(description = "Obligatoria (defecto true). Con valor_defecto nunca falta", example = "true")
    private Boolean obligatoria;

    @Min(1)
    @Max(1600)
    @Schema(description = "Longitud máxima del valor recibido", example = "40")
    private Integer longitudMax;

    @Size(max = 200)
    @Schema(description = "Expresión regular que debe cumplir el valor completo", example = "[A-Za-zÁÉÍÓÚáéíóúÑñ ]+")
    private String patron;

    @Size(max = 200)
    @Schema(description = "Valor si el request no la envía", example = "Sede principal")
    private String valorDefecto;

    @Size(max = 255)
    private String descripcion;
}
