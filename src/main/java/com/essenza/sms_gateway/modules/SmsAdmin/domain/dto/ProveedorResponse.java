package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Proveedor SMS")
public class ProveedorResponse {
    private Long id;
    private String codigo;
    private String nombre;
    private String baseUrl;
    private Integer connectTimeoutMs;
    private Integer readTimeoutMs;
    private boolean activo;
    private LocalDateTime fechaCreacion;
    @Schema(description = "true si existe un módulo que implementa este proveedor (SmsSenderPort)")
    private boolean implementado;
    @Schema(description = "Cantidad de cuentas registradas")
    private int cuentas;
}
