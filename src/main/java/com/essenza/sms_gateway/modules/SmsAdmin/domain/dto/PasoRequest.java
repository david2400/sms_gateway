package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import com.essenza.sms_gateway.shared.common.domain.dto.AuditInfoDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Paso de una ruta: cuenta y política de intentos")
public class PasoRequest extends AuditInfoDto {

    @NotNull @Min(1) @Max(99)
    @Schema(description = "1 = principal, 2.. = respaldo", example = "1")
    private Integer orden;

    @NotNull
    @Schema(example = "1")
    private Long cuentaId;

    @Min(1) @Max(10)
    @Schema(description = "Intentos sobre la cuenta antes de pasar a la siguiente (por defecto 3)", example = "3")
    private Integer maxIntentos;

    @Min(0) @Max(60000)
    @Schema(description = "Espera entre intentos en ms (por defecto 500)", example = "500")
    private Integer esperaMs;
}
