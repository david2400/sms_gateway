package com.essenza.sms_gateway.modules.SmsCore.domain.dto.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Resultado de la operación de actualización de usuario
 */
@Data
@Builder
@Schema(
        description = "Resultado de la operación de actualización de usuario"
)
public class ActualizarUsuarioResult {
    
    @Schema(description = "Indica si la operación fue exitosa", example = "true")
    private boolean success;
    
    @Schema(description = "Mensaje de resultado", example = "Usuario actualizado exitosamente")
    private String message;
    
    @Schema(description = "Código de error si falló", example = "INVALID_PIN")
    private String errorCode;
    
    @Schema(description = "Timestamp de la operación", example = "2024-01-20T08:00:00Z")
    private Instant timestamp;
    
    /**
     * Crea un resultado exitoso
     */
    public static ActualizarUsuarioResult success(String message) {
        return ActualizarUsuarioResult.builder()
                .success(true)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
    
    /**
     * Crea un resultado fallido
     */
    public static ActualizarUsuarioResult failure(String message, String errorCode) {
        return ActualizarUsuarioResult.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(Instant.now())
                .build();
    }
}
