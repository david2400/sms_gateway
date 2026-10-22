package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import com.essenza.sms_gateway.shared.common.domain.dto.AuditInfoDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/** Ruta tal como la usará el motor (después de filtrar inactivos y validar proveedor). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Ruta efectiva que usará el motor de envío")
public class RutaEfectivaResponse extends AuditInfoDto {
    private String proveedor;
    private String tipoMensaje;
    @Schema(description = "false si no hay ruta activa con pasos: el envío respondería 422")
    private boolean configurada;
    private List<Paso> pasos;
    @Schema(description = "Problemas detectados (cuentas sin credenciales, proveedores sin módulo)")
    private List<String> advertencias;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Paso extends AuditInfoDto {
        private int orden;
        private String proveedor;
        private String cuenta;
        private int maxIntentos;
        private long esperaMs;
        @Schema(description = "Usuario y secreto resueltos")
        private boolean credencialesOk;
        @Schema(description = "Hay un módulo que implementa el proveedor")
        private boolean implementado;
    }
}
