package com.essenza.sms_gateway.modules.SmsCore.domain.dto;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido.VariableError;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Error de un request de envío que se rechaza antes de llegar al motor (contenido / plantilla).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request de envío rechazado (no se registró ni se envió nada)")
public class SmsErrorResponse {

    @Schema(description = "VALIDACION, PLANTILLA_NO_ENCONTRADA, VARIABLES_INVALIDAS, PLANTILLA_MAL_CONFIGURADA",
            example = "VARIABLES_INVALIDAS")
    private String codigoError;

    @Schema(example = "Las variables no cumplen la plantilla RECORDATORIO_CITA (2 error(es))")
    private String detalle;

    @Schema(example = "RECORDATORIO_CITA")
    private String plantilla;

    @Schema(description = "Errores por variable (codigo: FALTANTE, DESCONOCIDA, TIPO_INVALIDO, LONGITUD_EXCEDIDA, "
            + "FORMATO_INVALIDO, MENSAJE_MUY_LARGO)")
    private List<VariableError> errores;

    private LocalDateTime fecha;
}
