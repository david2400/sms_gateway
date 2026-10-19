package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ruta con sus pasos")
public class RutaResponse {
    private Long id;
    @Schema(description = "null = ruta del endpoint central")
    private Long proveedorId;
    @Schema(description = "CENTRAL o código del proveedor")
    private String proveedorCodigo;
    private String tipoMensaje;
    private boolean activo;
    private List<Paso> pasos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Paso {
        private Long id;
        private int orden;
        private Long cuentaId;
        private String cuentaCodigo;
        private String proveedorCodigo;
        private int maxIntentos;
        private int esperaMs;
        private boolean activo;
    }
}
