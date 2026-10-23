package com.essenza.sms_gateway.modules.SmsCore.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Resultado de un envío masivo: totales y el resultado de cada destinatario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resultado del envío masivo")
public class SmsBatchResponse {
    @Schema(description = "Id del lote (bulkId enviado al proveedor masivo)")
    private String loteId;
    @Schema(description = "Plantilla usada, si aplica")
    private String plantilla;
    private int total;
    private int enviados;
    private int fallidos;
    private List<SmsEnvioResponse> resultados;
}
