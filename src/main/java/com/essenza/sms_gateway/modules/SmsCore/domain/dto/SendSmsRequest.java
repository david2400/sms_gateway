package com.essenza.sms_gateway.modules.SmsCore.domain.dto;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request del endpoint central de SMS. El proveedor y la cuenta los decide la ruta en BD.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Envío de SMS por el endpoint centralizado (orquesta todos los proveedores)")
public class SendSmsRequest {

    @Schema(description = "Tipo de mensaje: define la ruta de cuentas/proveedores. Obligatorio con 'message' salvo que se "
            + "envíe 'ruta' (se usa el tipo de la ruta); con 'plantilla' es opcional (se usa el tipo de la plantilla)", example = "OTP")
    private MessageType tipoMensaje;

    @Size(max = 50, message = "La ruta no puede superar 50 caracteres")
    @Pattern(regexp = "[A-Za-z0-9_\\-]{1,50}", message = "Ruta inválida: use el código o el id de la ruta")
    @Schema(description = "Ruta a ejecutar: código (CanalRuta.codigo) o id. Opcional: si se envía se ejecutan SUS pasos en orden "
            + "(no se resuelve por tipo de mensaje) y no se puede combinar con 'proveedor' ni 'cuenta'. Si viene 'tipo_mensaje' "
            + "debe ser el de la ruta. Ruta inexistente o inactiva → FALLIDO RUTA_NO_ENCONTRADA / RUTA_INACTIVA",
            example = "OTP_PRIORITARIA")
    private String ruta;

    @NotBlank(message = "El destinatario es obligatorio")
    @Pattern(regexp = "\\+?\\d{10,15}", message = "El destinatario debe tener entre 10 y 15 dígitos")
    @Schema(description = "Número destino, 10 dígitos o con prefijo país", example = "3157132338")
    private String to;

    @Pattern(regexp = "\\d{1,4}", message = "Código de país inválido")
    @Schema(description = "Código de país cuando el número va sin prefijo", example = "57")
    private String countryCode;

    @Size(max = 1600, message = "El mensaje no puede superar 1600 caracteres")
    @Schema(description = "Texto del SMS. Excluyente con 'plantilla'", example = "Su código de verificación es 123456")
    private String message;

    @Size(max = 50)
    @Pattern(regexp = "[A-Za-z0-9_\\-]{2,50}", message = "Código de plantilla inválido")
    @Schema(description = "Código de la plantilla (SmsPlantilla.codigo). Excluyente con 'message'", example = "RECORDATORIO_CITA")
    private String plantilla;

    @Size(max = 50, message = "Máximo 50 variables")
    @Schema(description = "Valores de las variables de la plantilla. Se validan en modo estricto: faltantes, desconocidas, "
            + "tipo, longitud y formato", example = "{\"nombre\": \"Ana\", \"fecha\": \"2026-11-05\"}")
    private Map<String, String> variables;

    @Size(max = 30)
    @Schema(description = "Proveedor preferido (solo endpoint central). Debe tener habilitado el servicio del tipo de mensaje; "
            + "sus cuentas van primero y el resto de la ruta central queda de respaldo", example = "INALAMBRIA")
    private String proveedor;

    @Schema(description = "Remitente (si el proveedor lo soporta). En tipo_mensaje BIDIRECCIONAL es el código corto de origen", example = "Comfenalco")
    private String sender;

    @Size(max = 50)
    @Schema(description = "Código de la cuenta a usar (SmsCuenta.codigo). Opcional: si se indica se usa primero "
            + "esa cuenta y, si agota sus intentos, se continúa con el resto de la ruta", example = "INALAMBRIA_OTP")
    private String cuenta;
}
