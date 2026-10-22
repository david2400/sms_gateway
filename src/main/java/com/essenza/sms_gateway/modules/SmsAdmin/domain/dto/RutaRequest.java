package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import com.essenza.sms_gateway.shared.common.domain.dto.AuditInfoDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/** Alta de ruta (opcionalmente con sus pasos). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Ruta = (proveedor, tipo de mensaje). Sin proveedor = ruta del endpoint central")
public class RutaRequest extends AuditInfoDto {

    @Schema(description = "Id del proveedor; null para la ruta del endpoint central", example = "1")
    private Long proveedorId;

    @NotNull
    @Schema(description = "Id del tipo de mensaje (sms_tipo_mensaje; ver GET /v1/sms/admin/tipos-mensaje)", example = "1")
    private Long tipoMensajeId;

    @Valid
    @Schema(description = "Pasos iniciales (opcional)")
    private List<PasoRequest> pasos;
}
