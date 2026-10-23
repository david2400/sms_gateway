package com.essenza.sms_gateway.modules.SmsCore.domain.dto;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Envío del mismo mensaje a varios destinatarios por el endpoint central.
 * Cada destinatario es un envío independiente del motor (con sus reintentos y su registro en el log).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Envío masivo (un registro de envío por destinatario; lotes nativos si el proveedor los soporta)")
public class SendSmsBatchRequest {

    public static final int MAX_DESTINATIONS = 5000;

    @Schema(description = "Tipo de mensaje. Obligatorio con 'message'; con 'plantilla' es opcional", example = "MASIVO")
    private MessageType tipoMensaje;

    @Size(max = 1600)
    @Schema(description = "Texto igual para todos. Excluyente con 'plantilla'", example = "Recuerde su cita de mañana a las 10:00 AM")
    private String message;

    @Size(max = 50)
    @Pattern(regexp = "[A-Za-z0-9_\\-]{2,50}", message = "Código de plantilla inválido")
    @Schema(description = "Plantilla: el texto se genera por destinatario con sus variables", example = "RECORDATORIO_CITA")
    private String plantilla;

    @Size(max = 50)
    @Schema(description = "Variables comunes a todos los destinatarios (cada destinatario puede sobrescribirlas)")
    private Map<String, String> variables;

    @Size(max = 30)
    @Schema(description = "Proveedor preferido (solo endpoint central)", example = "INALAMBRIA")
    private String proveedor;

    @Schema(description = "Remitente (si el proveedor lo soporta)")
    private String sender;

    @Size(max = 50)
    @Schema(description = "Código de la cuenta a usar para todos los destinatarios (SmsCuenta.codigo). Opcional: si se indica se usa primero "
            + "esa cuenta y, si agota sus intentos, se continúa con el resto de la ruta", example = "INALAMBRIA_OTP")
    private String cuenta;

    @NotEmpty(message = "Debe enviar al menos un destinatario")
    @Size(max = MAX_DESTINATIONS, message = "Máximo " + MAX_DESTINATIONS + " destinatarios por request")
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

        @Size(max = 50)
        @Schema(description = "Variables propias del destinatario", example = "{\"nombre\": \"Ana\"}")
        private Map<String, String> variables;
    }
}
