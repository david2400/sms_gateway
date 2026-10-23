package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Servicios habilitados de un proveedor. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Servicios (tipos de mensaje) que atiende el proveedor")
public class ServiciosProveedorResponse {
    private Long proveedorId;
    @Schema(example = "GO4CLIENTS")
    private String proveedorCodigo;
    @Schema(example = "[\"MASIVO\", \"NORMAL\"]")
    private List<String> tiposMensaje;
}
