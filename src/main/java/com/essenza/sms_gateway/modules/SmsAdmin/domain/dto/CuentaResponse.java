package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cuenta de proveedor (sin contraseña)")
public class CuentaResponse {
    private Long id;
    private Long proveedorId;
    private String proveedorCodigo;
    private String codigo;
    private String usuario;
    private String secretRef;
    @Schema(description = "true si la variable de entorno de secret_ref está definida en el servidor")
    private boolean secretDefinido;
    private String remitente;
    private Map<String, Object> parametros;
    private boolean activo;
    @Schema(description = "Ids de las rutas que usan esta cuenta")
    private List<Long> rutas;
}
