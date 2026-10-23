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

import java.util.List;
import java.util.Map;

/**
 * Envío de WhatsApp con plantilla aprobada por Meta. WhatsApp no permite texto libre fuera de una
 * conversación abierta: se envía el id de la plantilla y sus parámetros en orden.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Envío de WhatsApp (plantilla de Meta)")
public class SendWhatsAppRequest {

    @Schema(description = "Tipo de mensaje de canal WHATSAPP (por defecto WHATSAPP)", example = "WHATSAPP")
    private MessageType tipoMensaje;

    @NotBlank(message = "El destinatario es obligatorio")
    @Pattern(regexp = "\\+?\\d{10,15}", message = "El destinatario debe tener entre 10 y 15 dígitos")
    @Schema(description = "Número destino, 10 dígitos o con prefijo país", example = "3157132338")
    private String to;

    @Pattern(regexp = "\\d{1,4}", message = "Código de país inválido")
    @Schema(description = "Código de país cuando el número va sin prefijo", example = "57")
    private String countryCode;

    @NotBlank(message = "template_id es obligatorio")
    @Size(max = 100)
    @Schema(description = "Id de la plantilla aprobada por Meta en el proveedor", example = "ID_PLANTILLA_MARKETING")
    private String templateId;

    @NotNull(message = "parameters es obligatorio (lista vacía si la plantilla no tiene variables)")
    @Size(max = 50)
    @Schema(description = "Valores de la plantilla en el orden que exige; ninguno puede estar vacío",
            example = "[\"https://example.com/imagen.png\", \"Miguel\"]")
    private List<@NotBlank(message = "Los parámetros no pueden estar vacíos") @Size(max = 1024) String> parameters;

    @Size(max = 20)
    @Schema(description = "Número/identificador de origen. Opcional: por defecto el remitente de la cuenta", example = "573000000000")
    private String source;

    @Size(max = 20)
    @Schema(description = "Datos adicionales para el proveedor (optionalAditionalData en Inalambria)")
    private Map<String, String> datosAdicionales;

    @Size(max = 50)
    @Schema(description = "Cuenta a usar primero (SmsCuenta.codigo); el resto de la ruta queda de respaldo", example = "INALAMBRIA_WHATSAPP")
    private String cuenta;

    @Size(max = 30)
    @Schema(description = "Proveedor preferido (solo endpoint central)", example = "INALAMBRIA")
    private String proveedor;
}
