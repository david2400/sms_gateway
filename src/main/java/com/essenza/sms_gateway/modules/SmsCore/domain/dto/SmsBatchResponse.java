package com.essenza.sms_gateway.modules.SmsCore.domain.dto;

import com.essenza.sms_gateway.shared.common.domain.dto.AuditInfoDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Resultado de un envío masivo: totales y el resultado de cada destinatario.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Resultado del envío masivo")
public class SmsBatchResponse  extends AuditInfoDto {
    @Schema(description = "Id del lote (bulkId enviado al proveedor masivo)")
    private String loteId;
    @Schema(description = "Plantilla usada, si aplica")
    private String plantilla;
    private int total;
    private int enviados;
    private int fallidos;
    private List<SmsEnvioResponse> resultados;
}
