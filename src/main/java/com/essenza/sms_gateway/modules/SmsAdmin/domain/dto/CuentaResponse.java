package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import com.essenza.sms_gateway.shared.common.domain.dto.AuditInfoDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Cuenta de proveedor (sin contraseña)")
public class CuentaResponse extends AuditInfoDto {
    private Long id;
    private Long proveedorId;
    private String proveedorCodigo;
    private String codigo;
    private String usuario;
    private String secretRef;
    @Schema(description = "true si la variable de entorno de secret_ref está definida en el servidor")
    private boolean secretDefinido;
    @Schema(description = "true si la cuenta tiene clave cifrada guardada (el valor nunca se devuelve)")
    private boolean claveConfigurada;
    private String remitente;
    private Map<String, Object> parametros;
    @Schema(description = "Ids de las rutas que usan esta cuenta")
    private List<Long> rutas;
}
