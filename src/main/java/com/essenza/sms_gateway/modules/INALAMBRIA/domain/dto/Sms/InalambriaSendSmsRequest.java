package com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.Sms;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request del endpoint propio de Inalambria. Además de los datos comunes acepta
 * los campos nativos de RESTAWS 3.1, que sobrescriben los de la cuenta.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Envío de SMS solo por cuentas de Inalambria")
public class InalambriaSendSmsRequest {

    @NotNull(message = "El tipo de mensaje es obligatorio")
    @Schema(description = "Tipo de mensaje: define la ruta de cuentas de Inalambria", example = "OTP")
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

    // ===== Campos nativos de Inalambria (opcionales) =====

    @Min(0)
    @Schema(description = "TemplateId de Inalambria", example = "0")
    private Integer templateId;

    @Schema(description = "MessagePattern de Inalambria (0 = texto, 1 = plantilla)", example = "0")
    private String messagePattern;

    @Schema(description = "Enviar como Flash SMS", example = "false")
    private Boolean flashSms;

    @Schema(description = "Url asociada al mensaje (si aplica)")
    private String url;

    @Size(max = 50)
    @Schema(description = "Código de una cuenta de Inalambria (SmsCuenta.codigo). Opcional: si se indica se usa primero "
            + "esa cuenta y, si agota sus intentos, se continúa con el resto de la ruta", example = "INALAMBRIA_OTP")
    private String cuenta;
}
