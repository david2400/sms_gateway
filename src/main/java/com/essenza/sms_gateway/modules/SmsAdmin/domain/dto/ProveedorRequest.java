package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Alta o modificación de proveedor. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de un proveedor SMS")
public class ProveedorRequest {

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_]{2,30}", message = "Código: 2 a 30 letras, números o _")
    @Schema(description = "Código único; debe coincidir con el providerCode del módulo (se guarda en mayúsculas)", example = "INALAMBRIA")
    private String codigo;

    @NotBlank
    @Size(max = 100)
    @Schema(example = "Inalambria")
    private String nombre;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "https?://.+", message = "base_url debe iniciar con http:// o https://")
    @Schema(example = "https://restaws.inalambria.com")
    private String baseUrl;

    @Min(100) @Max(60000)
    @Schema(description = "Timeout de conexión (ms)", example = "3000")
    private Integer connectTimeoutMs;

    @Min(100) @Max(120000)
    @Schema(description = "Timeout de lectura (ms)", example = "10000")
    private Integer readTimeoutMs;

    @Schema(description = "Activo (por defecto true al crear)", example = "true")
    private Boolean activo;
}
