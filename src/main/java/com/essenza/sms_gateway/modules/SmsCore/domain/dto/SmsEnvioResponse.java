package com.essenza.sms_gateway.modules.SmsCore.domain.dto;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchStatus;
import com.essenza.sms_gateway.shared.common.domain.dto.AuditInfoDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Respuesta de un envío de SMS.
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resultado de un envío de SMS")
public class SmsEnvioResponse extends AuditInfoDto {

    @Schema(description = "Id del envío (consultable en /v1/sms/envios/{id})")
    private UUID envioId;

    @Schema(description = "Estado final", example = "ENVIADO")
    private DispatchStatus estado;

    @Schema(description = "Proveedor con el que se envió (o el último intentado)", example = "INALAMBRIA")
    private String proveedor;

    @Schema(description = "Cuenta con la que se envió (o la última intentada)", example = "INALAMBRIA_OTP")
    private String cuenta;

    @Schema(description = "HTTP status de la respuesta del proveedor", example = "200")
    private Integer httpStatus;

    @Schema(description = "Total de intentos realizados", example = "1")
    private int intentos;

    @Schema(description = "Código de error si falló", example = "TODAS_LAS_CUENTAS_FALLARON")
    private String codigoError;

    @Schema(description = "Detalle del error si falló")
    private String detalle;

    public static SmsEnvioResponse from(DispatchResult r) {
        return SmsEnvioResponse.builder()
                .envioId(r.getEnvioId())
                .estado(r.getStatus())
                .proveedor(r.getProviderCode())
                .cuenta(r.getAccountCode())
                .httpStatus(r.getHttpStatus())
                .intentos(r.getAttempts())
                .codigoError(r.getErrorCode())
                .detalle(r.getDetail())
                .build();
    }
}
