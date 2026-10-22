package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import com.essenza.sms_gateway.shared.common.domain.dto.AuditInfoDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Proveedor SMS")
public class ProveedorResponse extends AuditInfoDto {
    private Long id;
    private String codigo;
    private String nombre;
    private String baseUrl;
    private Integer connectTimeoutMs;
    private Integer readTimeoutMs;
    private LocalDateTime fechaCreacion;
    @Schema(description = "true si existe un módulo que implementa este proveedor (SmsSenderPort)")
    private boolean implementado;
    @Schema(description = "Cantidad de cuentas registradas")
    private int cuentas;
}
