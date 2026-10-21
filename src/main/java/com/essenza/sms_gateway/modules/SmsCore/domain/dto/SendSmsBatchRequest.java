package com.essenza.sms_gateway.modules.SmsCore.domain.dto;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Envío del mismo mensaje a varios destinatarios por el endpoint central.
 * Cada destinatario es un envío independiente del motor (con sus reintentos y su registro en el log).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Envío masivo por el endpoint central (un envío del motor por destinatario)")
public class SendSmsBatchRequest {

    public static final int MAX_DESTINATIONS = 500;

    @NotNull(message = "El tipo de mensaje es obligatorio")
    @Schema(example = "MASIVO")
    private MessageType tipoMensaje;

    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 1600)
    @Schema(example = "Recuerde su cita de mañana a las 10:00 AM")
    private String message;

    @Schema(description = "Remitente (si el proveedor lo soporta)")
    private String sender;

    @Size(max = 50)
    @Schema(description = "Código de la cuenta a usar para todos los destinatarios (SmsCuenta.codigo). Opcional: si se indica se usa primero "
            + "esa cuenta y, si agota sus intentos, se continúa con el resto de la ruta", example = "INALAMBRIA_OTP")
    private String cuenta;

    @NotEmpty(message = "Debe enviar al menos un destinatario")
    @Size(max = MAX_DESTINATIONS, message = "Máximo " + MAX_DESTINATIONS + " destinatarios; para volúmenes mayores use campañas")
    @Valid
    private List<Destinatario> destinations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Destinatario {
        @NotBlank
        @Pattern(regexp = "\\+?\\d{10,15}", message = "El destinatario debe tener entre 10 y 15 dígitos")
        @Schema(example = "3157132338")
        private String to;

        @Pattern(regexp = "\\d{1,4}", message = "Código de país inválido")
        @Schema(example = "57")
        private String countryCode;
    }
}
