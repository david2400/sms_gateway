package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import com.essenza.sms_gateway.shared.common.domain.dto.AuditInfoDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

/**
 * Alta o modificación de cuenta. La contraseña se envía en {@code clave} y se guarda cifrada (nunca se devuelve);
 * alternativa: {@code secret_ref} con el nombre de una variable de entorno.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Datos de una cuenta de proveedor")
public class CuentaRequest extends AuditInfoDto {

    @NotNull
    @Schema(description = "Id del proveedor", example = "1")
    private Long proveedorId;

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_]{2,50}", message = "Código: 2 a 50 letras, números o _")
    @Schema(description = "Código único (se guarda en mayúsculas)", example = "INALAMBRIA_OTP")
    private String codigo;

    @Size(max = 100)
    @Schema(description = "Usuario o api key. Use 'env:VARIABLE' para leerlo del entorno", example = "COMFENALCOquindioOTP")
    private String usuario;

    @Pattern(regexp = "[A-Za-z_][A-Za-z0-9_.]{0,99}", message = "secret_ref debe ser el NOMBRE de una variable de entorno, no la contraseña")
    @Schema(description = "Opcional: nombre de la variable de entorno con la contraseña (si no se usa 'clave')",
            example = "INALAMBRIA_OTP_PASSWORD")
    private String secretRef;

    @Size(max = 200)
    @Schema(description = "Contraseña / api secret en texto plano. Se guarda cifrada (AES-256) y nunca se devuelve. "
            + "Al actualizar, si no se envía se conserva la actual", accessMode = Schema.AccessMode.WRITE_ONLY,
            example = "********")
    @ToString.Exclude
    private String clave;

    @Size(max = 20)
    @Schema(description = "Remitente", example = "RANDOM")
    private String remitente;

    @Schema(description = "Parámetros propios del proveedor (objeto JSON plano)", example = "{\"type\":1,\"messagePattern\":\"0\",\"templateId\":0}")
    private Map<String, Object> parametros;
}
