package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Lista completa de servicios (códigos de tipo de mensaje) que atiende un proveedor. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reemplaza los servicios del proveedor. Lista vacía = el proveedor no atiende ningún tipo")
public class ServiciosProveedorRequest {

    @NotNull
    @Size(max = 50)
    @Schema(description = "Códigos de SmsTipoMensaje", example = "[\"NORMAL\", \"MASIVO\"]")
    private List<String> tiposMensaje;
}
