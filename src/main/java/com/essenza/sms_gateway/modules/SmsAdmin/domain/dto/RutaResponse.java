package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import com.essenza.sms_gateway.shared.common.domain.dto.AuditInfoDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Ruta con sus pasos")
public class RutaResponse extends AuditInfoDto {
    private Long id;
    @Schema(description = "null = ruta del endpoint central")
    private Long proveedorId;
    @Schema(description = "CENTRAL o código del proveedor")
    private String proveedorCodigo;
    @Schema(description = "Id del tipo de mensaje (sms_tipo_mensaje)")
    private Long tipoMensajeId;
    @Schema(description = "Código del tipo de mensaje (ej. OTP)")
    private String tipoMensaje;
    private List<Paso> pasos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Paso extends AuditInfoDto {
        private Long id;
        private int orden;
        private Long cuentaId;
        private String cuentaCodigo;
        private String proveedorCodigo;
        private int maxIntentos;
        private int esperaMs;
    }
}
