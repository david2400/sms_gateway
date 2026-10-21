package com.essenza.sms_gateway.modules.SmsCore.domain.dto;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request del endpoint central de SMS. El proveedor y la cuenta los decide la ruta en BD.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Envío de SMS por el endpoint centralizado (orquesta todos los proveedores)")
public class SendSmsRequest {

    @NotNull(message = "El tipo de mensaje es obligatorio")
    @Schema(description = "Tipo de mensaje: define la ruta de cuentas/proveedores", example = "OTP")
    private MessageType tipoMensaje;

    @NotBlank(message = "El destinatario es obligatorio")
    @Pattern(regexp = "\\+?\\d{10,15}", message = "El destinatario debe tener entre 10 y 15 dígitos")
    @Schema(description = "Número destino, 10 dígitos o con prefijo país", example = "3157132338")
    private String to;

    @Pattern(regexp = "\\d{1,4}", message = "Código de país inválido")
    @Schema(description = "Código de país cuando el número va sin prefijo", example = "57")
    private String countryCode;

    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 1600, message = "El mensaje no puede superar 1600 caracteres")
    @Schema(description = "Texto del SMS", example = "Su código de verificación es 123456")
    private String message;

    @Schema(description = "Remitente (si el proveedor lo soporta)", example = "Comfenalco")
    private String sender;

    @Size(max = 50)
    @Schema(description = "Código de la cuenta a usar (SmsCuenta.codigo). Opcional: si se indica se usa primero "
            + "esa cuenta y, si agota sus intentos, se continúa con el resto de la ruta", example = "INALAMBRIA_OTP")
    private String cuenta;
}
