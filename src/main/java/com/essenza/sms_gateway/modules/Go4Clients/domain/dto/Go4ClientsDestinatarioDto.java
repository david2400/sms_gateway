package com.essenza.sms_gateway.modules.Go4Clients.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Destinatario de un envío de campaña Go4Clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Destinatario de campaña Go4Clients")
public class Go4ClientsDestinatarioDto {

    @NotBlank(message = "El número es obligatorio")
    @Pattern(regexp = "\\+?\\d{10,15}", message = "El número debe tener entre 10 y 15 dígitos")
    @Schema(description = "Número destino (10 dígitos, sin prefijo país)", example = "3157132338")
    private String phoneNumber;

    @Pattern(regexp = "\\d{1,4}", message = "Código de país inválido")
    @Schema(description = "Código de país", example = "57")
    private String countryCode;
}
