package com.essenza.sms_gateway.modules.SmsCore.domain.dto.usuario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para actualizar usuario con PIN en el servicio externo
 * Formato snake_case para compatibilidad con API externa
 */
@Data
@Builder
@Schema(
        description = "DTO para actualizar usuario con PIN en servicio externo"
)
public class ActualizarUsuarioDto {
    
    @NotBlank(message = "El tipo de identificación es obligatorio")
    @JsonProperty("tipo_identificacion")
    @Schema(description = "Tipo de identificación", example = "CC")
    private String tipoIdentificacion;

    @NotBlank(message = "El número de identificación es obligatorio")
    @JsonProperty("nro_identificacion")
    @Schema(description = "Número de identificación", example = "123456789")
    private String nroIdentificacion;

    @NotBlank(message = "El nombre es obligatorio")
    @JsonProperty("nombre")
    @Schema(description = "Nombre del usuario", example = "Juan Pérez")
    private String nombre;

    @NotBlank(message = "El PIN es obligatorio")
    @JsonProperty("pin")
    @Schema(description = "PIN del usuario", example = "1234")
    private String pin;
    
    /**
     * Valida que el DTO tenga todos los campos requeridos
     */
    @JsonIgnore
    public boolean isValid() {
        return tipoIdentificacion != null && !tipoIdentificacion.trim().isEmpty() &&
               nroIdentificacion != null && !nroIdentificacion.trim().isEmpty() &&
               nombre != null && !nombre.trim().isEmpty() &&
               pin != null && !pin.trim().isEmpty();
    }
}
